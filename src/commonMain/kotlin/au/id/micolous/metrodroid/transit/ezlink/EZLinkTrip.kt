/*
 * EZLinkTrip.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.ezlink

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

data class EZUserData(val startStation: Station?,
                      val endStation: Station?,
                      val routeName: FormattedString) {
    companion object {
        fun parse(userData: String, type: CEPASTransaction.TransactionType): EZUserData {
            if (type == CEPASTransaction.TransactionType.BUS && (
                            userData.startsWith("SVC")
                                    || userData.startsWith("BUS")))
                return EZUserData(null, null,
                        Localizer.localizeFormatted(R.string.ez_bus_number, userData.substring(3, 7).replace(" ", "")))
            if (type == CEPASTransaction.TransactionType.CREATION)
                return EZUserData(Station.nameOnly(userData), null, Localizer.localizeFormatted(R.string.ez_first_use))
            if (type == CEPASTransaction.TransactionType.RETAIL)
                return EZUserData(Station.nameOnly(userData), null, Localizer.localizeFormatted(R.string.ez_retail_purchase))

            val routeName = when (type) {
                CEPASTransaction.TransactionType.BUS -> Localizer.localizeFormatted(R.string.unknown_format, userData)
                // FIXME: These aren't actually routes...
                CEPASTransaction.TransactionType.BUS_REFUND -> Localizer.localizeFormatted(R.string.ez_bus_refund)
                CEPASTransaction.TransactionType.MRT -> Localizer.localizeFormatted(R.string.ez_mrt)
                CEPASTransaction.TransactionType.TOP_UP -> Localizer.localizeFormatted(R.string.ez_topup)
                CEPASTransaction.TransactionType.SERVICE -> Localizer.localizeFormatted(R.string.ez_service_charge)
                else -> Localizer.localizeFormatted(R.string.unknown_format, type.toString())
            }

            if (userData.length > 6 && (userData[3] == '-' || userData[3] == ' ')) {
                val startStationAbbr = userData.substring(0, 3)
                val endStationAbbr = userData.substring(4, 7)
                return EZUserData(EZLinkTransitData.getStation(startStationAbbr), EZLinkTransitData.getStation(endStationAbbr), routeName)
            }
            return EZUserData(Station.nameOnly(userData), null, routeName)
        }
    }
}

@Parcelize
class EZLinkTrip (private val mTransaction: CEPASTransaction,
                  private val mCardName: String): Trip() {

    override val startTimestamp: Timestamp?
        get() = mTransaction.timestamp

    override val routeName: FormattedString?
        get() = EZUserData.parse(mTransaction.userData, mTransaction.type).routeName

    override val humanReadableRouteID: String?
        get() = mTransaction.userData

    override val fare: TransitCurrency?
        get() = if (mTransaction.type === CEPASTransaction.TransactionType.CREATION) null else TransitCurrency.SGD(-mTransaction.amount)

    override val startStation: Station?
        get() = EZUserData.parse(mTransaction.userData, mTransaction.type).startStation

    override val endStation: Station?
        get() = EZUserData.parse(mTransaction.userData, mTransaction.type).endStation

    override val mode: Trip.Mode
        get() = getMode(mTransaction.type)

    override fun getAgencyName(isShort: Boolean) =
        getAgencyName(mTransaction.type, mCardName, isShort)

    companion object {
        fun getMode(type: CEPASTransaction.TransactionType) =
                when (type) {
                    CEPASTransaction.TransactionType.BUS, CEPASTransaction.TransactionType.BUS_REFUND -> Trip.Mode.BUS
                    CEPASTransaction.TransactionType.MRT -> Trip.Mode.METRO
                    CEPASTransaction.TransactionType.TOP_UP -> Trip.Mode.TICKET_MACHINE
                    CEPASTransaction.TransactionType.RETAIL, CEPASTransaction.TransactionType.SERVICE -> Trip.Mode.POS
                    else -> Trip.Mode.OTHER
                }

        fun getAgencyName(type: CEPASTransaction.TransactionType, cardName: String, isShort: Boolean) = FormattedString.english(
                when (type) {
                    CEPASTransaction.TransactionType.BUS, CEPASTransaction.TransactionType.BUS_REFUND -> "BUS"
                    CEPASTransaction.TransactionType.CREATION,
                    CEPASTransaction.TransactionType.TOP_UP,
                    CEPASTransaction.TransactionType.SERVICE -> if (isShort && cardName == "EZ-Link") "EZ" else cardName
                    CEPASTransaction.TransactionType.RETAIL -> "POS"
                    else -> "SMRT"
                }
            )
    }
}
