/*
 * EasyCardTransitData.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
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
        private val serialNumber: String,
        private val balance: Int,
        private val trips: List<EasyCardTransaction>,
        private val refill: EasyCardTopUp
) : TransitData() {
    constructor(card: ClassicCard) : this(
            parseSerialNumber(card),
            parseBalance(card),
            EasyCardTransaction.parseTrips(card),
            EasyCardTopUp.parse(card)
    )

    override fun getBalance(): TransitCurrency = TransitCurrency.TWD(balance)

    override fun getCardName(): String = NAME

    override fun getSerialNumber(): String? = serialNumber

    override fun getTrips(): Array<out Trip> {
        val ret: ArrayList<Trip> = ArrayList()
        ret.addAll(trips)
        ret.add(refill)
        return ret.toArray(arrayOf())
    }

    companion object {
        internal val TZ: TimeZone = TimeZone.getTimeZone("Asia/Taipei")

        internal const val NAME = "EasyCard"
        val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.easycard)
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

        internal const val EASYCARD_STR: String = "easycard"

        private fun parseSerialNumber(card: ClassicCard): String {
            val data = (card.getSector(0))?.getBlock(0)?.data!!
            return Utils.getHexString(data, 0, 4)
        }

        private fun parseBalance(card: ClassicCard): Int {
            val data = (card.getSector(2))?.getBlock(0)?.data
            return Utils.byteArrayToIntReversed(data, 0, 4)
        }

        val FACTORY = object: ClassicCardTransitFactory() {
            override fun check(card: ClassicCard): Boolean {
                val data: ByteArray? = try {
                    (card.getSector(0))?.getBlock(1)?.data
                } catch (e: Exception) {
                    null
                }

                val x = data != null && Arrays.equals(data, EasyCardTransitData.MAGIC)
                return x
            }

            override fun earlySectors(): Int {
                return 0
            }

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                val uid = EasyCardTransitData.parseSerialNumber(card)
                return TransitIdentity(EasyCardTransitData.NAME, uid)
            }

            override fun parseTransitData(card: ClassicCard): TransitData {
                return EasyCardTransitData(card)
            }
        }
    }
}