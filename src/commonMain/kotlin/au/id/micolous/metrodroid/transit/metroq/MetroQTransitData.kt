/*
 * MetroQTransitData.kt
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

package au.id.micolous.metrodroid.transit.metroq

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicBlock
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class MetroQTransitData (private val mSerial: Long,
                         private val mBalance: Int,
                         private val mProduct: Int,
                         private val mExpiry: Timestamp?,
                         private val mDate1: Timestamp): TransitData() {

    override val serialNumber: String?
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = NAME

    override val balance: TransitBalance?
        get() {
            val name = when (mProduct) {
                501 -> Localizer.localizeString(R.string.metroq_fare_card)
                401 -> Localizer.localizeString(R.string.metroq_day_pass)
                else -> mProduct.toString()
            }
            return TransitBalanceStored(TransitCurrency.USD(mBalance), name, mExpiry)
        }

    override val info: List<ListItem>?
        get() = listOf(ListItem(FormattedString("Date 1"),
                TimestampFormatter.longDateFormat(mDate1)))

    companion object {
        private const val NAME = "Metro Q"
        private const val METRO_Q_ID = 0x5420
        private val TZ = MetroTimeZone.HOUSTON
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_houston,
                imageId = R.drawable.metroq,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                preview = true)

        private fun parseTimestamp(data: ImmutableByteArray, off: Int): Timestamp {
            return Daystamp(data.getBitsFromBuffer(off, 8) + 2000,
                    data.getBitsFromBuffer(off + 8, 4) - 1,
                    data.getBitsFromBuffer(off + 12, 5))
        }

        private fun parse(card: ClassicCard): MetroQTransitData {
            val balanceSector = card.getSector(8)
            val balanceBlock0 = balanceSector.getBlock(0)
            val balanceBlock1 = balanceSector.getBlock(1)
            val balanceBlock: ClassicBlock
            if (balanceBlock0.data.getBitsFromBuffer(93, 8) > balanceBlock1.data.getBitsFromBuffer(93, 8))
                balanceBlock = balanceBlock0
            else
                balanceBlock = balanceBlock1
            return MetroQTransitData(
                    mBalance = balanceBlock.data.getBitsFromBuffer(77, 16),
                    mProduct = balanceBlock.data.getBitsFromBuffer(8, 12),
                    mExpiry = parseTimestamp(card.getSector(1).getBlock(0).data, 0),
                    mDate1 = parseTimestamp(card.getSector(1).getBlock(0).data, 24),
                    mSerial = getSerial(card)
            )
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {

            override val earlySectors: Int
                get() = 1

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val sector = sectors[0]
                for (i in 1..2) {
                    val block = sector.getBlock(i).data
                    for (j in (if (i == 1) 1 else 0)..7)
                        if (block.byteArrayToInt(j * 2, 2) != METRO_Q_ID && (i != 2 || j != 6))
                            return false
                }
                return true
            }

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                return TransitIdentity(NAME, formatSerial(getSerial(card)))
            }

            override fun parseTransitData(card: ClassicCard) = parse(card)
        }

        private fun getSerial(card: ClassicCard): Long {
            return card.getSector(1).getBlock(2).data.byteArrayToLong(0, 4)
        }

        private fun formatSerial(serial: Long): String {
            return NumberUtils.zeroPad(serial, 8)
        }
    }
}
