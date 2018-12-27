/*
 * EasyCardTransitData.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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
package au.id.micolous.metrodroid.transit.easycard

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class EasyCardTransitData internal constructor(
        private val balance: Int,
        private val trips: List<Trip>,
        private val refill: EasyCardTopUp
) : TransitData() {
    constructor(card: ClassicCard) : this(
            parseBalance(card),
            EasyCardTransaction.parseTrips(card),
            EasyCardTopUp.parse(card)
    )

    override fun getBalance() = TransitCurrency.TWD(balance)

    override fun getCardName() = NAME

    override fun getSerialNumber(): String? = null

    override fun getTrips() = trips + listOf(refill)

    companion object {
        private val TZ = TimeZone.getTimeZone("Asia/Taipei")

        internal const val NAME = "EasyCard"
        private val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.tpe_easy_card, R.drawable.iso7810_id1_alpha)
                .setName(NAME)
                .setLocation(R.string.location_taipei)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .setPreview()
                .build()

        internal val MAGIC = byteArrayOf(
                0x0e, 0x14, 0x00, 0x01,
                0x07, 0x02, 0x08, 0x03,
                0x09, 0x04, 0x08, 0x10,
                0x00, 0x00, 0x00, 0x00)

        internal const val EASYCARD_STR = "easycard"

        private fun parseBalance(card: ClassicCard): Int {
            return Utils.byteArrayToIntReversed(card[2, 0].data, 0, 4)
        }

        internal fun parseTimestamp(ts: Long?): Calendar? {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = (ts ?: return null) * 1000
            return g
        }

        val FACTORY = object : ClassicCardTransitFactory {
            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val data: ByteArray? = try {
                    sectors[0][1].data
                } catch (e: Exception) {
                    null
                }

                return data != null && Arrays.equals(data, MAGIC)
            }

            override fun earlySectors() = 1

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME, null)

            override fun parseTransitData(card: ClassicCard) = EasyCardTransitData(card)

            override fun getAllCards() = listOf(CARD_INFO)
        }
    }
}