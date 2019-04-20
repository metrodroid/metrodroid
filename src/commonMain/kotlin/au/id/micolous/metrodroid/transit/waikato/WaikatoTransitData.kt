/*
 * WaikatoTransitData.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class WaikatoTrip (private val lastTransactionValue: Int): Trip() {
    override val startTimestamp get(): Timestamp? = null
    override val fare get() = TransitCurrency.NZD(lastTransactionValue)
    override val mode get() = Mode.BUS
}

/**
 * Reader for Waikato BUSIT cards.
 * More info: https://github.com/micolous/metrodroid/wiki/BUSIT
 */
@Parcelize
data class WaikatoTransitData internal constructor(
    val records: List<WaikatoRecord>
) : TransitData() {

    private val newRecord = records.sortedByDescending { it.txnNumber }.first()
    private val oldRecord = records.sortedBy { it.txnNumber }.first()

    override val cardName get() = NAME
    override val balance get() = TransitCurrency.NZD(newRecord.balance)
    override val serialNumber get() = newRecord.serialNumber

    // TODO: Figure out trips properly
    override val trips get() : List<Trip> = listOf(trip)

    private val trip = WaikatoTrip(oldRecord.balance - newRecord.balance)

    companion object {
        private const val NAME = "BUSIT"

        // BUSIT cards have all default keys, so there is no need to load them!
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_waikato,
                cardType = CardType.MifareClassic,
                preview = true)

        private fun getSerial(card: ClassicCard) =
                card[1][0].data.getHexString(4, 4)

        val MAGIC = ImmutableByteArray.fromASCII("Panda")

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override val earlySectors get() = 1

            override fun earlyCheck(sectors: List<ClassicSector>) =
                    sectors[0].blocks[1].data.startsWith(MAGIC)

            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(NAME, getSerial(card))

            override fun parseTransitData(card: ClassicCard) =
                    WaikatoTransitData(listOf(
                            WaikatoRecord.parseRecord(card, 1),
                            WaikatoRecord.parseRecord(card, 5)
                    ))

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}