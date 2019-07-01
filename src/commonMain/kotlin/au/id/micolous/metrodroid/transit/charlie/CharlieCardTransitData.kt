/*
 * CharlieCardTransitData.kt
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

package au.id.micolous.metrodroid.transit.charlie

import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class CharlieCardTransitData (private val mSerial: Long,
                              private val mSecondSerial: Long,
                              private val mBalance: Int,
                              private val mStartDate: Int,
                              override val trips: List<CharlieCardTrip>): TransitData() {

    // After 2011, all cards expire 10 years after issue.
    // Cards were first issued in 2006, and would expire after 5 years, and had no printed
    // expiry date.
    // However, currently (2018), all of these have expired anyway.
    // Find the last trip taken on the card.
    // Cards not used for 2 years will also expire
    override val balance: TransitBalance?
        get() {
            val start = parseTimestamp(mStartDate)
            var expiry = start.plus(Duration.yearsLocal(11)).plus(Duration.daysLocal(-1)).toDaystamp()
            val candidate = getLastUseDaystamp()?.plus(Duration.yearsLocal(2))?.toDaystamp()

            if (candidate != null && candidate < expiry) {
                expiry = candidate
            }
            return TransitBalanceStored(TransitCurrency.USD(mBalance), null, start, expiry)
        }

    override val serialNumber: String?
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = NAME

    override val info: List<ListItem>?
        get() = if (mSecondSerial == 0L || mSecondSerial == 0xffffffffL) null else listOf(ListItem(R.string.charlie_2nd_card_number,
                "A" +NumberUtils.zeroPad(mSecondSerial, 10)))

    companion object {
        private const val NAME = "CharlieCard"
        private val CHARLIE_EPOCH = Epoch.utc(2003, MetroTimeZone.NEW_YORK, +5 * 60)
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_boston,
                cardType = CardType.MifareClassic,
                imageId = R.drawable.charlie_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                keysRequired = true, keyBundle = "charlie",
                preview = true)

        private fun parse(card: ClassicCard): CharlieCardTransitData {
            val sector2 = card.getSector(2)
            val sector3 = card.getSector(3)
            val balanceSector: ClassicSector
            if (sector2.getBlock(0).data.getBitsFromBuffer(81, 16) > sector3.getBlock(0).data.getBitsFromBuffer(81, 16))
                balanceSector = sector2
            else
                balanceSector = sector3

            val mTrips = mutableListOf<CharlieCardTrip>()
            for (i in 0..11) {
                val block = card.getSector(6 + i / 6).getBlock(i / 2 % 3)
                if (block.data.byteArrayToInt(7 * (i % 2), 4) == 0)
                    continue
                mTrips.add(CharlieCardTrip(block.data, 7 * (i % 2)))
            }

            return CharlieCardTransitData(
                    trips = mTrips,
                    mSerial = getSerial(card),
                    mSecondSerial = card.getSector(8).getBlock(0).data.byteArrayToLong(0, 4),
                    mBalance = getPrice(balanceSector.getBlock(1).data, 5),
                    mStartDate = balanceSector.getBlock(0).data.byteArrayToInt(6, 3)
            )
        }

        fun getPrice(data: ImmutableByteArray, off: Int): Int {
            var value = data.byteArrayToInt(off, 2)
            if (value and 0x8000 != 0) {
                value = -(value and 0x7fff)
            }
            return value / 2
        }

        internal fun parseTimestamp(timestamp: Int): TimestampFull {
            return CHARLIE_EPOCH.mins(timestamp)
        }

        private fun getSerial(card: ClassicCard): Long {
            return card.getSector(0).getBlock(0).data.byteArrayToLong(0, 4)
        }

        private fun formatSerial(serial: Long) = "5-$serial"

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {

            override val earlySectors: Int
                get() = 1

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean =
                    HashUtils.checkKeyHash(sectors[0], "charlie",
                            "63ee95c7340fceb524cae7aab66fb1f9", "2114a2414d6b378e36a4e9540d1adc9f") >= 0

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                return TransitIdentity(NAME, formatSerial(getSerial(card)))
            }

            override fun parseTransitData(card: ClassicCard) = parse(card)
        }
    }
}
