/*
 * OtagoGoCardTransitData.kt
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

package au.id.micolous.metrodroid.transit.otago

import au.id.micolous.metrodroid.card.CardType

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

private const val NAME = "GoCard (Otago)"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_otago,
        imageId = R.drawable.otago_gocard,
        cardType = CardType.MifareClassic,
        region = TransitRegion.NEW_ZEALAND,
        keysRequired = true, keyBundle = "otago_go")

private fun formatSerial(serial: Long) = serial.toString(16)

private fun getSerial(card: ClassicCard) = card[1, 0].data.byteArrayToLong(4, 4)

private fun parseTimestamp(input: ImmutableByteArray, off: Int): TimestampFull {
    val d = input.getBitsFromBuffer(off * 8, 5)
    val m = input.getBitsFromBuffer(off * 8 + 5, 4)
    val y = input.getBitsFromBuffer(off * 8 + 9, 4) + 2007
    val hm = input.getBitsFromBuffer(off * 8 + 13, 11)
    return TimestampFull(tz = MetroTimeZone.AUCKLAND, year = y, month = m - 1, day = d, hour = hm / 60,
            min = hm % 60)
}

@Parcelize
private data class OtagoGoCardRefill(override val startTimestamp: TimestampFull,
                                     private val mAmount: Int,
                                     override val machineID: String) : Trip() {
    override val fare get() = TransitCurrency.NZD(-mAmount)
    override val mode get() = Mode.TICKET_MACHINE

    companion object {
        fun parse(sector: ClassicSector): OtagoGoCardRefill? {
            return OtagoGoCardRefill(
                    mAmount = sector[0].data.byteArrayToIntReversed(12, 2),
                    startTimestamp = parseTimestamp(sector[0].data, 8),
                    machineID = sector[1].data.sliceOffLen(0, 2).toHexString()
            )
        }
    }
}

@Parcelize
private data class OtagoGoCardTrip(override val startTimestamp: TimestampFull,
                                   private val mCost: Int,
                                   override val machineID: String) : Trip() {
    override val fare get() = TransitCurrency.NZD(mCost)
    override val mode get() = Mode.BUS

    companion object {
        fun parse(sector: ImmutableByteArray): OtagoGoCardTrip? {
            if (sector.byteArrayToInt(3, 3) in listOf(0, 0xffffff))
                return null
            val timestamp = parseTimestamp(sector, 3)
            val cost = sector.byteArrayToIntReversed(7, 2)
            val machine = sector.sliceOffLen(11, 2).toHexString()
            return OtagoGoCardTrip(startTimestamp = timestamp, mCost = cost, machineID = machine)
        }
    }
}

@Parcelize
private data class OtagoGoCardTransitData internal constructor(
        private val mSerial: Long,
        private val mBalance: Int,
        private val mRefill: OtagoGoCardRefill?,
        private val mTrips: List<OtagoGoCardTrip>) : TransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val balance get() = TransitCurrency.NZD(mBalance)

    override val trips: List<Trip>
        get() = listOfNotNull(mRefill) + mTrips
}

object OtagoGoCardTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, formatSerial(getSerial(card)))

    override fun parseTransitData(card: ClassicCard): TransitData {
        val balSec = if (card[0][1].data[5].toInt() and 0x10 == 0) 1 else 5
        val tripData = card[balSec + 1].readBlocks(1, 2) + card[balSec + 2].readBlocks(0, 3)
        val trips = (0..3).map { tripData.sliceOffLen(it * 17, 17) }.mapNotNull { OtagoGoCardTrip.parse(it) }
        return OtagoGoCardTransitData(
                mSerial = getSerial(card),
                mBalance = card[balSec, 2].data.byteArrayToIntReversed(8, 3),
                mRefill = OtagoGoCardRefill.parse(sector = card[balSec + 3]),
                mTrips = trips
        )
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            sectors[0][1].data.sliceArray(0..4) == ImmutableByteArray.fromASCII("Valid") &&
                    sectors[1][0].data.byteArrayToInt(2, 2) == 0x4321

    override val earlySectors get() = 2
}
