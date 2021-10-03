/*
 * ZolotayaKoronaTrip.kt
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

package au.id.micolous.metrodroid.transit.zolotayakorona

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

private const val DEFAULT_FARE = 1300

@Parcelize
internal data class ZolotayaKoronaTrip(private val mValidator: String,
                                       internal val mTime: Int,
                                       private val mCardType: Int,
        // sequential number of round trips that bus makes
                                       private val mTrackNumber: Int,
                                       private val mPreviousBalance: Int,
                                       private val mNextBalance: Int?,
                                       private val mA: Int,
                                       private val mB: Int,
                                       private val mC: Int) : Trip() {
    private val estimatedFare
        get() = when (mCardType) {
            0x760500 -> 1150
            0x230100 -> 1275
            else -> null
        }
    internal val estimatedBalance
        get() = mPreviousBalance - (estimatedFare ?: DEFAULT_FARE)

    override val startTimestamp get() = ZolotayaKoronaTransitData.parseTime(mTime, mCardType)

    override val machineID get() = "J$mValidator"

    override val fare get(): TransitCurrency? {
        if (mNextBalance != null) {
            // Happens if one trip is followed by more than one refill
            if (mPreviousBalance - mNextBalance < -500)
                return null
            return TransitCurrency.RUB(mPreviousBalance - mNextBalance)
        }
        return TransitCurrency.RUB(estimatedFare ?: return null)
    }

    override val mode get() = Mode.BUS

    override fun getRawFields(level: TransitData.RawLevel): String? = "A=${mA.hexString}/B=${mB.hexString}/C=${mC.hexString}" +
            if (level == TransitData.RawLevel.ALL) "/trackNumber=$mTrackNumber/previousBalance=$mPreviousBalance" else ""

    companion object {
        fun parse(block: ImmutableByteArray, cardType: Int, refill: ZolotayaKoronaRefill?, balance: Int?): ZolotayaKoronaTrip? {
            if (block.isAllZero())
                return null
            val time = block.byteArrayToIntReversed(6, 4)
            var balanceAfter: Int? = null
            if (balance != null) {
                balanceAfter = balance
                if (refill != null && refill.mTime > time)
                    balanceAfter -= refill.mAmount
            }
            return ZolotayaKoronaTrip(
                    mA = block.byteArrayToInt(0, 2),
                    mValidator = block.getHexString(2, 3),
                    mB = block[5].toInt() and 0xff,
                    mTime = time,
                    mTrackNumber = block.byteArrayToInt(10, 1),
                    mPreviousBalance = block.byteArrayToIntReversed(11, 4),
                    mC = block[15].toInt() and 0xff,
                    mNextBalance = balanceAfter,
                    mCardType = cardType
            )
        }
    }
}
