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

    override fun getTrips(): MutableList<Trip>? {
        val ret: ArrayList<Trip> = ArrayList()
        ret.addAll(trips)
        ret.add(refill)
        return ret
    }

    companion object {
        private val TZ = TimeZone.getTimeZone("Asia/Taipei")

        internal const val NAME = "EasyCard"
        val CARD_INFO = CardInfo.Builder()
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
            val data = (card.getSector(2))?.getBlock(0)?.data
            return Utils.byteArrayToIntReversed(data, 0, 4)
        }

        internal fun parseTimestamp(ts: Long?): Calendar? {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = (ts ?: return null) * 1000
            return g
        }

        val FACTORY = object: ClassicCardTransitFactory {
            override fun check(card: ClassicCard): Boolean {
                val data: ByteArray? = try {
                    (card.getSector(0))?.getBlock(1)?.data
                } catch (e: Exception) {
                    null
                }

                return data != null && Arrays.equals(data, MAGIC)
            }

            override fun earlySectors() = 0

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME, null)

            override fun parseTransitData(card: ClassicCard) = EasyCardTransitData(card)

            override fun getAllCards(): MutableList<CardInfo> = Collections.singletonList(CARD_INFO)
        }
    }
}