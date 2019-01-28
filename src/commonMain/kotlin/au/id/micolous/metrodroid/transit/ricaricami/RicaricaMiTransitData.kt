/*
 * RicaricaMiTransitData.java
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

package au.id.micolous.metrodroid.transit.ricaricami

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransactionTripAbstract
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class RicaricaMiTransitData(private val mTrips: List<TransactionTripAbstract>,
                                 private val mSubscriptions: List<En1545Subscription>,
                                 private val ticketEnvParsed: En1545Parsed,
                                 private val contractList1: En1545Parsed,
                                 private val contractList2: En1545Parsed) : En1545TransitData(ticketEnvParsed) {
    override val lookup get() = RicaricaMiLookup.instance

    override val trips get() = mTrips

    override val subscriptions get() = mSubscriptions

    override val serialNumber: String? get() = null

    override val cardName get() = NAME

    companion object {
        private const val RICARICA_MI_ID = 0x0221
        private const val NAME = "RicaricaMi"

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_milan,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                preview = true)

        private val CONTRACT_LIST_FIELDS = En1545Container(
                En1545Repeat(4, En1545Container(
                        En1545FixedInteger(CONTRACTS_UNKNOWN_A, 3), // Always 3 so far
                        En1545FixedInteger(CONTRACTS_TARIFF, 16),
                        En1545FixedInteger(CONTRACTS_UNKNOWN_B, 5), // No idea
                        En1545FixedInteger(CONTRACTS_POINTER, 4)
                ))
        )

        private val BLOCK_1_0_FIELDS = En1545Container(
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_A, 9),
                En1545FixedInteger.BCDdate(En1545TransitData.HOLDER_BIRTH_DATE),
                En1545FixedHex(En1545TransitData.ENV_UNKNOWN_B, 47),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                En1545FixedInteger(En1545TransitData.ENV_UNKNOWN_C, 26)
        )
        private val BLOCK_1_1_FIELDS = En1545Container(
                En1545FixedHex(En1545TransitData.ENV_UNKNOWN_D, 64),
                En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_ISSUE),
                En1545FixedHex(En1545TransitData.ENV_UNKNOWN_E, 49)
        )

        private fun selectSubData(subData0: ImmutableByteArray, subData1: ImmutableByteArray): Int {
            val date0 = subData0.getBitsFromBuffer(6, 14)
            val date1 = subData1.getBitsFromBuffer(6, 14)

            if (date0 > date1)
                return 0
            if (date0 < date1)
                return 1

            val tapno0 = subData0.getBitsFromBuffer(0, 6)
            val tapno1 = subData1.getBitsFromBuffer(0, 6)

            if (tapno0 > tapno1)
                return 0
            if (tapno0 < tapno1)
                return 1

            if (subData1.isAllZero())
                return 0
            if (subData0.isAllZero())
                return 1

            return if (subData0 > subData1) 0 else 1
        }

        private fun parse(card: ClassicCard): En1545TransitData {
            val sector1 = card.getSector(1)
            val ticketEnvParsed = En1545Parser.parse(sector1.getBlock(0).data, BLOCK_1_0_FIELDS)
            ticketEnvParsed.append(sector1.getBlock(1).data, BLOCK_1_1_FIELDS)

            val trips = (0..5).mapNotNull { i ->
                val base = 0xa * 3 + 2 + i * 2
                val tripData = card.getSector(base / 3).getBlock(base % 3).data +
                        card.getSector((base + 1) / 3).getBlock((base + 1) % 3).data
                if (tripData.isAllZero()) {
                    null
                } else
                    RicaricaMiTransaction.parse(tripData)
            }
            val mergedTrips = TransactionTrip.merge(trips)
            val subscriptions = mutableListOf<RicaricaMiSubscription>()
            for (i in 0..2) {
                val sec = card.getSector(i + 6)
                if (sec.getBlock(0).data.isAllZero()
                        && sec.getBlock(1).data.isAllZero()
                        && sec.getBlock(2).data.isAllZero())
                    continue
                val subData = arrayOf(sec.getBlock(0).data, sec.getBlock(1).data)
                val sel = selectSubData(subData[0], subData[1])
                subscriptions.add(RicaricaMiSubscription.parse(subData[sel],
                        card.getSector(i + 2).getBlock(sel).data,
                        card.getSector(i + 2).getBlock(2).data))
            }
            // TODO: check following. It might have more to do with subscription type
            // than slot
            val sec = card.getSector(9)
            val subData = arrayOf(sec.getBlock(1).data, sec.getBlock(2).data)
            if (!subData[0].isAllZero() || !subData[1].isAllZero()) {
                val sel = selectSubData(subData[0], subData[1])
                subscriptions.add(RicaricaMiSubscription.parse(subData[sel],
                        card.getSector(5).getBlock(1).data,
                        card.getSector(5).getBlock(2).data))
            }
            val constractList1 = En1545Parser.parse(card[14,2].data, CONTRACT_LIST_FIELDS)
            val constractList2 = En1545Parser.parse(card[15,2].data, CONTRACT_LIST_FIELDS)
            return RicaricaMiTransitData(ticketEnvParsed = ticketEnvParsed,
                    mTrips = mergedTrips, mSubscriptions = subscriptions,
                    contractList1 = constractList1,
                    contractList2 = constractList2)
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                for (i in 1..2) {
                    val block = sectors[0].getBlock(i).data
                    for (j in (if (i == 1) 1 else 0)..7)
                        if (block.byteArrayToInt(j * 2, 2) != RICARICA_MI_ID)
                            return false
                }
                return true
            }

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME, null)

            override fun parseTransitData(card: ClassicCard) = parse(card)

            override val earlySectors get() = 1

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
