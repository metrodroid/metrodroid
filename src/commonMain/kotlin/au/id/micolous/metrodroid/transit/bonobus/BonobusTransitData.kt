/*
 * BonobusTransitData.kt
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

package au.id.micolous.metrodroid.transit.bonobus

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

private const val NAME = "Bonobús"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_cadiz,
        cardType = CardType.MifareClassic,
        imageId = R.drawable.cadizcard,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        region = TransitRegion.SPAIN,
        keysRequired = true, keyBundle = "cadiz")

private fun getSerial(card: ClassicCard) = card[0,0].data.byteArrayToLongReversed(0, 4)

fun parseTimestamp(input: Long) =
        TimestampFull(MetroTimeZone.MADRID,
                (input shr 25).toInt() + 2000,
                ((input shr 21).toInt() and 0xf) - 1,
                (input shr 16).toInt() and 0x1f,
                (input shr 11).toInt() and 0x1f,
                (input shr 5).toInt() and 0x3f,
                (input shl 1).toInt() and 0x3f
        )

@Parcelize
data class BonobusTrip internal constructor(
        private val mTimestamp: Long,
        private val mFare: Int,
        private val mA: Int,
        private val mB: Int,
        private val mC: Int,
        private val mD: Int,
        private val mE: Int) : Trip() {
    override val fare get() = TransitCurrency.EUR(mFare)
    override val mode get() = Mode.BUS
    override val startTimestamp get() = parseTimestamp(mTimestamp)
    override fun getRawFields(level: TransitData.RawLevel): String?
            = "A=0x${mA.toString(16)}/B=$mB/C=$mC/D=$mD/E=$mE"

    companion object {
        fun parse(raw: ImmutableByteArray): BonobusTrip? {
            if (raw.isAllZero())
                return null
            return BonobusTrip(raw.byteArrayToLong(0, 4),
                    raw.byteArrayToInt(6, 2),
                    raw.byteArrayToInt(4, 2),
                    raw.byteArrayToInt(8, 2),
                    raw.byteArrayToInt(10, 2),
                    raw.byteArrayToInt(12, 2),
                    raw.byteArrayToInt(14, 2))
        }
    }
}

@Parcelize
data class BonobusTransitData(private val mSerial: Long, private val mBalance: Int,
                              override val trips: List<BonobusTrip>,
                              private val mBlock02: ImmutableByteArray) : TransitData() {
    override val serialNumber get() = mSerial.toString()

    override val cardName get() = NAME

    override val balance get() = TransitCurrency.EUR(mBalance)

    override fun getRawFields(level: RawLevel)= listOf(ListItem(FormattedString("Block 0.2"), mBlock02.toHexDump()))
}

object BonobusTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, getSerial(card).toString())

    override fun parseTransitData(card: ClassicCard): BonobusTransitData {
        val trips = (7..15).flatMap { sec -> card[sec].blocks.dropLast(1) }.mapNotNull { BonobusTrip.parse(it.data) }
        return BonobusTransitData(
                mSerial = getSerial(card),
                trips = trips,
                mBalance = card[4,0].data.byteArrayToIntReversed(0, 4),
                mBlock02 = card[0,2].data)
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            HashUtils.checkKeyHash(sectors[0], "cadiz",
                    // KeyB is readable and so doesn't act as a key
                    "cc2f0d405a4968f95100f776161929f6") >= 0

    override val earlySectors get() = 1
}
