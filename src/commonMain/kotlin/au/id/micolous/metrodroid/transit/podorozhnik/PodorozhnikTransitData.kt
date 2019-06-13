/*
 * PodorozhnikTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.podorozhnik

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Podorozhnik cards.
 */

@Parcelize
private data class PodorozhnikSector4 (
        internal val mBalance: Int,
        internal val mLastTopup: Int,
        internal val mLastTopupTime: Int,
        internal val mLastTopupMachine: Int,
        internal val mLastTopupAgency: Int
): Parcelable

private fun decodeSector4(card: ClassicCard): PodorozhnikSector4? {
    val sector4 = card.getSector(4)

    if (sector4 is UnauthorizedClassicSector)
        return null

    // Block 0 and block 1 are copies. Let's use block 0
    val block0 = sector4.getBlock(0).data
    val block2 = sector4.getBlock(2).data
    return PodorozhnikSector4(mBalance = block0.byteArrayToIntReversed(0, 4),
            mLastTopupTime = block2.byteArrayToIntReversed(2, 3),
            mLastTopupAgency = block2[5].toInt(),
            mLastTopupMachine = block2.byteArrayToIntReversed(6, 2),
            mLastTopup = block2.byteArrayToIntReversed(8, 3))
}

@Parcelize
private data class PodorozhnikSector5 (
        internal val mLastFare: Int,
        internal val mExtraTripTimes: List<Int>,
        internal val mLastValidator: Int,
        internal val mLastTripTime: Int,
        internal val mGroundCounter: Int,
        internal val mSubwayCounter: Int,
        internal val mLastTransport: Int
): Parcelable

private fun decodeSector5(card: ClassicCard): PodorozhnikSector5? {
    val sector5 = card.getSector(5)

    if (sector5 is UnauthorizedClassicSector)
        return null

    val block0 = sector5.getBlock(0).data
    val block1 = sector5.getBlock(1).data
    val block2 = sector5.getBlock(2).data

    val mLastTripTime = block0.byteArrayToIntReversed(0, 3)

    // Usually block1 and block2 are identical. However rarely only one of them
    // gets updated. Pick most recent one for counters but remember both trip
    // timestamps.
    val mSubwayCounter: Int
    val mGroundCounter: Int
    if (block2.byteArrayToIntReversed(2, 3) > block1.byteArrayToIntReversed(2, 3)) {
        mSubwayCounter = block2[0].toInt() and 0xff
        mGroundCounter = block2[1].toInt() and 0xff
    } else {
        mSubwayCounter = block1[0].toInt() and 0xff
        mGroundCounter = block1[1].toInt() and 0xff
    }

    val extraTripTimes = listOf(
            block1.byteArrayToIntReversed(2, 3),
            block2.byteArrayToIntReversed(2, 3)
    ).filter { it != mLastTripTime }.distinct()

    return PodorozhnikSector5(mLastTripTime = mLastTripTime,
            mGroundCounter = mGroundCounter,
            mSubwayCounter = mSubwayCounter,
            mExtraTripTimes = extraTripTimes,
            mLastTransport = block0[3].toInt() and 0xff,
            mLastValidator = block0.byteArrayToIntReversed(4, 2),
            mLastFare = block0.byteArrayToIntReversed(6, 4)
    )
}

@Parcelize
class PodorozhnikTransitData private constructor(private val sector4: PodorozhnikSector4?,
                                                 private val sector5: PodorozhnikSector5?,
                                                 override val serialNumber: String) : TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_podorozhnik)

    override val trips: List<Trip>
        get() {
            val items = mutableListOf<Trip>()
            if (sector4 != null && sector4.mLastTopupTime != 0) {
                items.add(PodorozhnikTopup(sector4.mLastTopupTime, sector4.mLastTopup,
                        sector4.mLastTopupAgency, sector4.mLastTopupMachine))
            }
            if (sector5 != null && sector5.mLastTripTime != 0) {
                items.add(PodorozhnikTrip(sector5.mLastTripTime, sector5.mLastFare,
                        sector5.mLastTransport, sector5.mLastValidator))
            }
            for (timestamp in sector5?.mExtraTripTimes.orEmpty()) {
                items.add(PodorozhnikDetachedTrip(timestamp))
            }
            return items
        }

    override val info: List<ListItem>?
        get() {
            if (sector5 == null)
                return null
            return listOf(
                ListItem(R.string.ground_trips,
                        sector5.mGroundCounter.toString()),
                ListItem(R.string.subway_trips,
                        sector5.mSubwayCounter.toString()))
        }

    public override val balance: TransitBalance?
        get() {
            return TransitBalanceStored(TransitCurrency.RUB(sector4?.mBalance ?: return null),
                    Localizer.localizeString(R.string.card_name_podorozhnik), null)
        }

    constructor(card: ClassicCard) : this(
            serialNumber = getSerial(card[0,0].data),
            sector4 = decodeSector4(card),
            sector5 = decodeSector5(card))

    companion object {
        // We don't want to actually include these keys in the program, so include a hashed version of
        // this key.
        private const val KEY_SALT = "podorozhnik"
        // md5sum of Salt + Common Key + Salt, used on sector 4.
        private const val KEY_DIGEST_A = "f3267ff451b1fc3076ba12dcee2bf803"
        private const val KEY_DIGEST_B = "3823b5f0b45f3519d0ce4a8b5b9f1437"

        private val CARD_INFO = CardInfo(
                // seqgo_card_alpha has identical geometry
                imageId = R.drawable.podorozhnik_card,
                imageAlphaId = R.drawable.seqgo_card_alpha,
                name = Localizer.localizeString(R.string.card_name_podorozhnik),
                locationId = R.string.location_saint_petersburg,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_russia,
                keysRequired = true, keyBundle = "podorozhnik",
                preview = true)

        private val PODOROZHNIK_EPOCH = Epoch.utc(2010, MetroTimeZone.MOSCOW, -3 * 60)

        private const val TAG = "PodorozhnikTransitData"

        private fun getSerial(sec0: ImmutableByteArray): String {
            var sn = "9643 3078 " + NumberUtils.formatNumber(
                    sec0.byteArrayToLongReversed(0, 7),
                    " ", 4, 4, 4, 4, 1)
            sn += NumberUtils.calculateLuhn(sn.replace(" ", ""))// last digit is luhn
            return sn
        }

        fun convertDate(mins: Int): Timestamp = PODOROZHNIK_EPOCH.mins(mins)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override val earlySectors: Int
                get() = 5

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val key = sectors[4].key

                Log.d(TAG, "Checking for Podorozhnik key...")
                return HashUtils.checkKeyHash(key, KEY_SALT, KEY_DIGEST_A, KEY_DIGEST_B) >= 0
            }

            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_podorozhnik),
                        getSerial(card[0,0].data))

            override fun parseTransitData(card: ClassicCard) = PodorozhnikTransitData(card)
        }
    }
}
