/*
 * LaxTapTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.lax_tap

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.USD
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitDataCapsule
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader

/**
 * Los Angeles Transit Access Pass (LAX TAP) card.
 * https://github.com/micolous/metrodroid/wiki/Transit-Access-Pass
 */

@Parcelize
class LaxTapTransitData (override val capsule: NextfareTransitDataCapsule): NextfareTransitData() {
    override val cardName: String
        get() = NAME

    override val onlineServicesPage: String?
        get() = "https://www.taptogo.net/"

    override val timezone: MetroTimeZone
        get() = TIME_ZONE

    override val currency
        get() = ::USD

    companion object {

        private const val NAME = "TAP"
        private const val LONG_NAME = "Transit Access Pass"
        private val BLOCK1 = ImmutableByteArray.fromHex(
                "16181A1B1C1D1E1F010101010101"
        )
        val BLOCK2 = ImmutableByteArray(4)

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.laxtap_card,
                // Using the short name (TAP) may be ambiguous
                name = LONG_NAME,
                locationId = R.string.location_los_angeles,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                preview = true)

        private val TIME_ZONE = MetroTimeZone.LOS_ANGELES

        val FACTORY: ClassicCardTransitFactory = object : NextfareTransitData.NextFareTransitFactory() {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                return super.parseTransitIdentity(card, NAME)
            }

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val sector0 = sectors[0]
                val block1 = sector0.getBlock(1).data
                if (!block1.copyOfRange(1, 15).contentEquals(BLOCK1)) {
                    return false
                }

                val block2 = sector0.getBlock(2).data
                return block2.copyOfRange(0, 4).contentEquals(BLOCK2)
            }

            override fun parseTransitData(card: ClassicCard): TransitData {
                val capsule =  parse(card = card, timeZone = TIME_ZONE,
                        newTrip = ::LaxTapTrip,
                        newRefill = { LaxTapTrip(NextfareTripCapsule(it))},
                        shouldMergeJourneys = false)
                return LaxTapTransitData(capsule)
            }

            override val notice: String?
                get() = StationTableReader.getNotice(LaxTapData.LAX_TAP_STR)
        }
    }
}
