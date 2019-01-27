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

import android.os.Parcel
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Reader for Waikato BUSIT cards.
 * More info: https://github.com/micolous/metrodroid/wiki/BUSIT
 */
@Parcelize
data class WaikatoTransitData internal constructor(
    val records: List<WaikatoRecord>
) : TransitData() {

    @IgnoredOnParcel
    private val newRecord = records.sortedByDescending { it.txnNumber }.first()
    @IgnoredOnParcel
    private val oldRecord = records.sortedBy { it.txnNumber }.first()

    override val cardName get() = NAME
    override val balance get() = TransitCurrency.NZD(newRecord.balance)
    override val serialNumber get() = newRecord.serialNumber

    // TODO: Figure out trips properly
    override val trips get() : List<Trip> = listOf(trip)

    @IgnoredOnParcel
    private val lastTransactionValue =
            TransitCurrency.NZD(oldRecord.balance - newRecord.balance)

    @IgnoredOnParcel
    private val trip = object : Trip() {
        override fun writeToParcel(dest: Parcel?, flags: Int) {}
        override val startTimestamp get(): Timestamp? = null
        override val fare get() = lastTransactionValue
        override val mode get() = Mode.BUS
        override fun describeContents() = 0
    }

    companion object {
        private const val NAME = "BUSIT"

        // BUSIT cards have all default keys, so there is no need to load them!
        private val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_waikato)
                .setCardType(CardType.MifareClassic)
                .setPreview()
                .build()

        private fun getSerial(card: ClassicCard) =
                card[1][0].data.getHexString(4, 4)

        val MAGIC = ImmutableByteArray.fromASCII("Panda")

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory() {
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