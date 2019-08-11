/*
 * SuicaTrip.kt
 *
 * Copyright 2011 Kazzz
 * Copyright 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.transit.suica

import au.id.micolous.metrodroid.card.felica.FelicaBlock
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.NumberUtils

import kotlin.text.Regex

@Parcelize
class SuicaTrip (val balance: Int,
                 val consoleTypeInt: Int,
                 private val mProcessType: Int,
                 val fareRaw: Int,
                 override var startTimestamp: Timestamp?,
                 override var endTimestamp: Timestamp?,
                 override val startStation: Station?,
                 override val endStation: Station?,
                 val startStationId: Int,
                 val endStationId: Int,
                 val dateRaw: Int): Trip() {
    override val routeName: FormattedString?
        get() {
            if (startStation == null)
                return FormattedString("$consoleType $processType")
            val routeName = super.routeName ?: return null
            // SUICA HACK:
            // If there's something that looks like "#2" at the start, then mark
            // that as the default language.
            val m = LINE_NUMBER.matchEntire(routeName.unformatted)?.groups ?: return routeName
            // There is a line number
            //Log.d(TAG, String.format("num = %s, line = %s", m.group(1), m.group(2)));
            val sep = m[1]?.value?.length ?: return routeName
            return FormattedString.defaultLanguage(routeName.unformatted.substring(0, sep)) +
                routeName.substring(sep)
        }

    override val humanReadableRouteID: String?
        get() = if (startStation != null)
            super.humanReadableRouteID
        else
            NumberUtils.intToHex(consoleTypeInt) + " " + NumberUtils.intToHex(mProcessType)

    override val fare: TransitCurrency?
        get() = TransitCurrency.JPY(fareRaw)

    override val mode: Trip.Mode
        get() {
            val consoleType = consoleTypeInt and 0xFF
            return when {
                isTVM -> Trip.Mode.TICKET_MACHINE
                consoleType == 0xc8 -> Trip.Mode.VENDING_MACHINE
                consoleType == 0xc7 -> Trip.Mode.POS
                consoleTypeInt == CONSOLE_BUS.toByte().toInt() -> Trip.Mode.BUS
                else -> Trip.Mode.METRO
            }
        }

    private val consoleType: String
        get() = SuicaUtil.getConsoleTypeName(consoleTypeInt)

    private val processType: String
        get() = SuicaUtil.getProcessTypeName(mProcessType)

    private val isTVM: Boolean
        get() = isTVM(consoleTypeInt)

    override fun getAgencyName(isShort: Boolean) = startStation?.companyName

    /*
    public boolean isBus() {
        return mIsBus;
    }

    public boolean isProductSale() {
        return mIsProductSale;
    }

    public boolean isCharge() {
        return mIsCharge;
    }

    public int getBusLineCode() {
        return mBusLineCode;
    }

    public int getBusStopCode() {
        return mBusStopCode;
    }
    */

    fun setEndTime(hour: Int, min: Int) {
        endTimestamp = endTimestamp?.toDaystamp()?.promote(SuicaUtil.TZ, hour, min)
    }

    fun setStartTime(hour: Int, min: Int) {
        startTimestamp = startTimestamp?.toDaystamp()?.promote(SuicaUtil.TZ, hour, min)
    }

    companion object {
        private const val CONSOLE_BUS = 0x05
        private const val CONSOLE_CHARGE = 0x02
        
        /**
        * Used when localisePlaces=true to ensure route and line numbers are still read out in the
        * user's language.
        *
        * eg:
        * - "#7 Eastern Line" -> (local)#7 (foreign)Eastern Line
        * - "300 West" -> (local)300 (foreign)West
        * - "North Ferry" -> (foreign)North Ferry
        */
        private val LINE_NUMBER = Regex("(#?\\d+)?(\\D.+)")

        private fun isTVM(consoleTypeInt: Int): Boolean {
            val consoleType = consoleTypeInt and 0xFF
            val tvmConsoleTypes = intArrayOf(0x03, 0x07, 0x08, 0x12, 0x13, 0x14, 0x15)
            return consoleType in tvmConsoleTypes
        }

        fun parse(block: FelicaBlock, previousBalance: Int): SuicaTrip {
            val data = block.data

            // 00000080000000000000000000000000
            // 00 00 - console type
            // 01 00 - process type
            // 02 00 - ??
            // 03 80 - ??
            // 04 00 - date
            // 05 00 - date
            // 06 00 - enter line code
            // 07 00
            // 08 00
            // 09 00
            // 10 00
            // 11 00
            // 12 00
            // 13 00
            // 14 00
            // 15 00


            val consoleTypeInt = data[0].toInt()
            val mProcessType = data[1].toInt()

            val isProductSale = consoleTypeInt == 0xc7.toByte().toInt() || consoleTypeInt == 0xc8.toByte().toInt()

            val dateRaw = data.byteArrayToInt(4, 2)
            val startTimestamp = SuicaUtil.extractDate(isProductSale, data)
            val endTimestamp = startTimestamp
            // Balance is little-endian
            val balance = data.byteArrayToIntReversed(10, 2)

            val regionCode = data[15].toInt() and 0xFF

            val fareRaw = if (previousBalance >= 0) {
                previousBalance - balance
            } else {
                // Can't get amount for first record.
                0
            }


            val startStation: Station?
            val endStation: Station?
            // Unused block (new card)
            if (startTimestamp == null) {
                startStation = null
                endStation = null
            } else if (isProductSale || mProcessType == CONSOLE_CHARGE.toByte().toInt()) {
                startStation = null
                endStation = null
            } else if (consoleTypeInt == CONSOLE_BUS.toByte().toInt()) {
                val busLineCode = data.byteArrayToInt(6, 2)
                val busStopCode = data.byteArrayToInt(8, 2)
                startStation = SuicaDBUtil.getBusStop(regionCode, busLineCode, busStopCode)
                endStation = null
            } else if (isTVM(consoleTypeInt)) {
                val railEntranceLineCode = data[6].toInt() and 0xFF
                val railEntranceStationCode = data[7].toInt() and 0xFF
                startStation = SuicaDBUtil.getRailStation(regionCode, railEntranceLineCode,
                        railEntranceStationCode)
                endStation = null
            } else {
                val railEntranceLineCode = data[6].toInt() and 0xFF
                val railEntranceStationCode = data[7].toInt() and 0xFF
                val railExitLineCode = data[8].toInt() and 0xFF
                val railExitStationCode = data[9].toInt() and 0xFF
                startStation = SuicaDBUtil.getRailStation(regionCode, railEntranceLineCode,
                        railEntranceStationCode)
                endStation = SuicaDBUtil.getRailStation(regionCode, railExitLineCode,
                        railExitStationCode)
            }
            return SuicaTrip(balance = balance, consoleTypeInt = consoleTypeInt, mProcessType = mProcessType,
                    fareRaw = fareRaw, startTimestamp = startTimestamp, endTimestamp = endTimestamp,
                    startStation = startStation, endStation = endStation, dateRaw = dateRaw,
                    startStationId = data.byteArrayToInt(6, 2),
                    endStationId = data.byteArrayToInt(8, 2))
        }
    }
}
