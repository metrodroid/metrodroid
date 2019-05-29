/*
 * SmartRiderTransitData.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.smartrider

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.HashUtils

/**
 * Reader for SmartRider (Western Australia) and MyWay (Australian Capital Territory / Canberra)
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */

@Parcelize
class SmartRiderTransitData (override val serialNumber: String?,
                             private val mBalance: Int,
                             override val trips: List<TransactionTripAbstract>,
                             private val mCardType: CardType): TransitData() {

    public override val balance: TransitCurrency?
        get() = TransitCurrency.AUD(mBalance)

    override val cardName: String
        get() = mCardType.friendlyName

    enum class CardType constructor(val friendlyName: String) {
        UNKNOWN("Unknown SmartRider"),
        SMARTRIDER(SMARTRIDER_NAME),
        MYWAY(MYWAY_NAME)
    }

    companion object {
        private const val SMARTRIDER_NAME = "SmartRider"
        private const val MYWAY_NAME = "MyWay"
        private const val TAG = "SmartRiderTransitData"

        private val SMARTRIDER_CARD_INFO = CardInfo(
                imageId = R.drawable.smartrider_card,
                name = SMARTRIDER_NAME,
                locationId = R.string.location_wa_australia,
                cardType = au.id.micolous.metrodroid.card.CardType.MifareClassic,
                keysRequired = true,
                preview = true) // We don't know about ferries.

        private val MYWAY_CARD_INFO = CardInfo(
                imageId = R.drawable.myway_card,
                name = MYWAY_NAME,
                locationId = R.string.location_act_australia,
                cardType = au.id.micolous.metrodroid.card.CardType.MifareClassic,
                keysRequired = true)

        private fun parse(card: ClassicCard): SmartRiderTransitData {
            val mCardType = detectKeyType(card.sectors)
            val serialNumber = getSerialData(card)

            // Read trips.
            val tagBlocks = (10..13).flatMap { s -> (0..2).map { b -> card[s,b] } }
            val tagRecords = tagBlocks.map  { b ->
                SmartRiderTagRecord.parse(mCardType, b.data) }.filter { it.isValid }

            // Build the Tag events into trips.
            val trips = TransactionTripAbstract.merge(tagRecords) { el: Transaction -> SmartRiderTrip(el) }

            // TODO: Figure out balance priorities properly.

            // This presently only picks whatever balance is lowest. Recharge events aren't understood,
            // and parking fees (SmartRider only) are also not understood.  So just pick whatever is
            // the lowest balance, and we're probably right, unless the user has just topped up their
            // card.
            val recordA = card.getSector(2).getBlock(2).data
            val recordB = card.getSector(3).getBlock(2).data

            val balanceA = recordA.byteArrayToIntReversed(7, 2)
            val balanceB = recordB.byteArrayToIntReversed(7, 2)

            Log.d(TAG, "balanceA = $balanceA, balanceB = $balanceB")
            val mBalance = if (balanceA < balanceB) balanceA else balanceB

            return SmartRiderTransitData(mBalance = mBalance, trips = trips, mCardType = mCardType,
                    serialNumber = serialNumber)
        }

        // Unfortunately, there's no way to reliably identify these cards except for the "standard" keys
        // which are used for some empty sectors.  It is not enough to read the whole card (most data is
        // protected by a unique key).
        //
        // We don't want to actually include these keys in the program, so include a hashed version of
        // this key.
        private const val MYWAY_KEY_SALT = "myway"
        // md5sum of Salt + Common Key 2 + Salt, used on sector 7 key A and B.
        private const val MYWAY_KEY_DIGEST = "29a61b3a4d5c818415350804c82cd834"

        private const val SMARTRIDER_KEY_SALT = "smartrider"
        // md5sum of Salt + Common Key 2 + Salt, used on Sector 7 key A.
        private const val SMARTRIDER_KEY2_DIGEST = "e0913518a5008c03e1b3f2bb3a43ff78"
        // md5sum of Salt + Common Key 3 + Salt, used on Sector 7 key B.
        private const val SMARTRIDER_KEY3_DIGEST = "bc510c0183d2c0316533436038679620"

        private fun detectKeyType(sectors: List<ClassicSector>): CardType {
            try {
                val key = sectors[7].key

                Log.d(TAG, "Checking for MyWay key...")
                if (HashUtils.checkKeyHash(key, MYWAY_KEY_SALT, MYWAY_KEY_DIGEST) >= 0) {
                    return CardType.MYWAY
                }

                Log.d(TAG, "Checking for SmartRider key...")
                if (HashUtils.checkKeyHash(key, SMARTRIDER_KEY_SALT,
                                SMARTRIDER_KEY2_DIGEST, SMARTRIDER_KEY3_DIGEST) >= 0) {
                    return CardType.SMARTRIDER
                }
            } catch (ignored: IndexOutOfBoundsException) {
                // If that sector number is too high, then it's not for us.
            }

            return CardType.UNKNOWN
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(SMARTRIDER_CARD_INFO, MYWAY_CARD_INFO)

            override val earlySectors: Int
                get() = 8

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean =
                    detectKeyType(sectors) != CardType.UNKNOWN

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity =
                    TransitIdentity(detectKeyType(card.sectors).friendlyName, getSerialData(card))

            override fun earlyCardInfo(sectors: List<ClassicSector>): CardInfo? =
                    when (detectKeyType(sectors)) {
                        SmartRiderTransitData.CardType.MYWAY -> MYWAY_CARD_INFO
                        SmartRiderTransitData.CardType.SMARTRIDER -> SMARTRIDER_CARD_INFO
                        else -> null
                    }

            override fun parseTransitData(card: ClassicCard) = parse(card)
        }

        private fun getSerialData(card: ClassicCard): String {
            val serialData = card.getSector(0).getBlock(1).data
            var serial = serialData.getHexString(6, 5)
            if (serial.startsWith("0")) {
                serial = serial.substring(1)
            }
            return serial
        }
    }
}
