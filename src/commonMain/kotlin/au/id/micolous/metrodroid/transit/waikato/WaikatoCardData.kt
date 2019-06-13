/*
 * WaikatoCardTransitData.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.waikato

import au.id.micolous.metrodroid.card.CardType

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

private val ROTORUA_CARD_INFO = CardInfo(
        name = "SmartRide (Rotorua)",
        locationId = R.string.location_rotorua,
        cardType = CardType.MifareClassic,
        imageId = R.drawable.rotorua,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        preview = true)
private val BUSIT_CARD_INFO = CardInfo(name = "BUSIT",
        locationId = R.string.location_waikato,
        cardType = CardType.MifareClassic,
        imageId = R.drawable.busitcard,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        preview = true)

private fun formatSerial(serial: Long) = serial.toString(16)

private fun getSerial(card: ClassicCard) = card[1, 0].data.byteArrayToLong(4, 4)

private fun getName(card: ClassicCard) =
        when {
            card[0, 1].data.sliceArray(0..4) == ImmutableByteArray.fromASCII("Panda") -> BUSIT_CARD_INFO.name
            else -> ROTORUA_CARD_INFO.name
        }

private fun parseTimestamp(input: ImmutableByteArray, off: Int): TimestampFull {
    val d = input.getBitsFromBuffer(off * 8, 5)
    val m = input.getBitsFromBuffer(off * 8 + 5, 4)
    val y = input.getBitsFromBuffer(off * 8 + 9, 4) + 2007
    val hm = input.getBitsFromBuffer(off * 8 + 13, 11)
    return TimestampFull(tz = MetroTimeZone.AUCKLAND, year = y, month = m - 1, day = d, hour = hm / 60,
            min = hm % 60)
}

private fun parseDate(input: ImmutableByteArray, off: Int): Daystamp {
    val d = input.getBitsFromBuffer(off * 8, 5)
    val m = input.getBitsFromBuffer(off * 8 + 5, 4)
    val y = input.getBitsFromBuffer(off * 8 + 9, 7) + 1991
    return Daystamp(year = y, month = m - 1, day = d)
}

@Parcelize
private data class WaikatoCardTrip(override val startTimestamp: TimestampFull,
                                   private val mCost: Int, private val mA: Int, private val mB: Int,
                                   override val mode: Mode) : Trip() {
    override val fare get() = if (mCost == 0 && mode == Mode.TICKET_MACHINE) null else TransitCurrency.NZD(mCost)

    override fun getRawFields(level: TransitData.RawLevel): String? = "${mA.toString(16)}/${mB.toString(16)}"

    companion object {
        fun parse(sector: ImmutableByteArray, mode: Mode): WaikatoCardTrip? {
            if (sector.isAllZero() || sector.isAllFF())
                return null
            val timestamp = parseTimestamp(sector, 1)
            val cost = sector.byteArrayToIntReversed(5, 2)
            val a = sector[0].toInt() and 0xff
            val b = sector[4].toInt() and 0xff
            return WaikatoCardTrip(startTimestamp = timestamp, mCost = cost, mA = a, mB = b, mode = mode)
        }
    }
}

@Parcelize
private data class WaikatoCardTransitData internal constructor(
        private val mSerial: Long,
        private val mBalance: Int,
        override val trips: List<WaikatoCardTrip>,
        override val cardName: String,
        private val mLastTransactionDate: Daystamp) : TransitData() {
    override val serialNumber get() = formatSerial(mSerial)
    override val balance get() = TransitCurrency.NZD(mBalance)
}

class WaikatoCardTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(ROTORUA_CARD_INFO, BUSIT_CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            getName(card), formatSerial(getSerial(card)))

    override fun parseTransitData(card: ClassicCard): TransitData {
        val balSec = if (card[0][1].data[5].toInt() and 0x10 == 0) 1 else 5
        val lastTrip = WaikatoCardTrip.parse(card[balSec + 2, 0].data.sliceOffLen(0, 7), Trip.Mode.BUS)
        val lastRefill = WaikatoCardTrip.parse(card[balSec + 2, 0].data.sliceOffLen(7, 7), Trip.Mode.TICKET_MACHINE)
        return WaikatoCardTransitData(
                mSerial = getSerial(card),
                mBalance = card[balSec + 1, 1].data.byteArrayToIntReversed(9, 2),
                trips = listOfNotNull(lastRefill, lastTrip),
                cardName = getName(card),
                mLastTransactionDate = parseDate(card[balSec + 1, 1].data, 7)
        )
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            sectors[0][1].data.sliceArray(0..4) in listOf(ImmutableByteArray.fromASCII("Valid"),
                    ImmutableByteArray.fromASCII("Panda")) &&
                    sectors[1][0].data.byteArrayToInt(2, 2) == 0x4850

    override val earlySectors get() = 2
}
