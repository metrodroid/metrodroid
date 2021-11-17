/*
 * BilheteUnicoSPTransitData.kt
 *
 * Copyright 2013 Marcelo Liberato <mliberato@gmail.com>
 * Copyright 2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.bilhete_unico

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicBlock
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class BilheteUnicoSPTransitData (private val mCredit: Int,
                                 private val mTransactionCounter: Int,
                                 private val mRefillTransactionCounter: Int,
                                 override val trips: List<Trip>,
                                 private val mDay2: Int,
                                 private val mSerial: Long): TransitData() {

    override val info: List<ListItem>?
        get() = listOf(ListItem(R.string.trip_counter,
                    mTransactionCounter.toString()),
            ListItem(R.string.refill_counter,
                    mRefillTransactionCounter.toString()),
                // It looks like issue date but on some dumps it's after the trips, so it can't be.
                ListItem(FormattedString("Date 1"),
                    BilheteUnicoSPTrip.EPOCH.days(mDay2).format()))

    override val cardName: String
        get() = NAME

    public override val balance: TransitCurrency?
        get() = TransitCurrency.BRL(mCredit)


    override val serialNumber: String?
        get() = formatSerial(mSerial)

    companion object {
        private const val NAME = "Bilhete Único"
        private val CARD_INFO = CardInfo(
                imageId = R.drawable.bilheteunicosp_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                name = NAME,
                locationId = R.string.location_sao_paulo,
                cardType = CardType.MifareClassic,
                region = TransitRegion.BRAZIL,
                keysRequired = true)

        private fun parse(card: ClassicCard): BilheteUnicoSPTransitData {
            val identitySector = card.getSector(2)

            val creditSector = card.getSector(8)

            val creditBlock0 = creditSector.getBlock(0).data

            val lastRefillDay = creditBlock0.getBitsFromBuffer(2, 14)
            val lastRefillAmount = creditBlock0.getBitsFromBuffer(29, 11)

            var lastTripSector = card.getSector(3)
            if (!checkCRC16Sector(lastTripSector))
                lastTripSector = card.getSector(4)

            val tripBlock0 = lastTripSector.getBlock(0).data
            val block1 = lastTripSector.getBlock(1).data
            val day = block1.getBitsFromBuffer(76, 14)
            val time = block1.getBitsFromBuffer(90, 11)
            val block2 = lastTripSector.getBlock(2).data
            val firstTapDay = block2.getBitsFromBuffer(2, 14)
            val firstTapTime = block2.getBitsFromBuffer(16, 11)
            val firstTapLine = block2.getBitsFromBuffer(27, 9)
            val trips = mutableListOf<Trip>()
            if (day != 0)
                trips.add(BilheteUnicoSPTrip.parse(lastTripSector))
            if (firstTapDay != day || firstTapTime != time)
                trips.add(BilheteUnicoSPFirstTap(firstTapDay, firstTapTime, firstTapLine))
            if (lastRefillDay != 0)
                trips.add(BilheteUnicoSPRefill(lastRefillDay, lastRefillAmount))

            return BilheteUnicoSPTransitData(mSerial = getSerial(card),
                    mDay2 = identitySector.getBlock(0).data.getBitsFromBuffer(2, 14),
                    mRefillTransactionCounter = creditBlock0.getBitsFromBuffer(44, 14),
                    mCredit = creditSector.getBlock(1).data.byteArrayToIntReversed(0, 4),
                    mTransactionCounter = tripBlock0.getBitsFromBuffer(48, 14),
                    trips = trips)
        }

        private fun formatSerial(serial: Long): String =
                NumberUtils.zeroPad(serial shr 36, 2) + "0 " +
                        NumberUtils.zeroPad((serial shr 4) and 0xffffffffL, 9)

        private fun getSerial(card: ClassicCard) =
                card[2, 0].data.byteArrayToLong(3, 5)

        private fun checkValueBlock(block: ClassicBlock, addr: Int): Boolean {
            val data = block.data
            return (data.byteArrayToInt(0, 4) == data.byteArrayToInt(4, 4).inv()
                    && data.byteArrayToInt(0, 4) == data.byteArrayToInt(8, 4)
                    && data[12].toInt() == addr && data[14].toInt() == addr && data[13] == data[15]
                    && data[13].toInt() and 0xff == addr.inv() and 0xff)
        }

        private fun checkCRC16Sector(s: ClassicSector): Boolean =
            HashUtils.calculateCRC16IBM(s.allData) == 0

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {

            override val earlySectors: Int
                get() = 9

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                // Normally both sectors are identical but occasionally one of them might get corrupted,
                // so tolerate one failure
                if (!checkCRC16Sector(sectors[3]) && !checkCRC16Sector(sectors[4]))
                    return false
                for (sectoridx in 5..8) {
                    val addr = sectoridx * 4 + 1
                    val sector = sectors[sectoridx]
                    if (!checkValueBlock(sector.getBlock(1), addr))
                        return false
                    if (!checkValueBlock(sector.getBlock(2), addr))
                        return false
                }
                return true
            }

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                return TransitIdentity(NAME, formatSerial(getSerial(card)))
            }

            override fun parseTransitData(card: ClassicCard) = parse(card)
        }
    }
}
