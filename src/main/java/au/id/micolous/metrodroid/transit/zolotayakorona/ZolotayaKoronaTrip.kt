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

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

private const val DEFAULT_FARE = 1300

@Parcelize
internal data class ZolotayaKoronaTrip(private val mValidator: String,
                                       internal val mTime: Int,
                                       private val mCardType: Int,
        // sequential number of round trips that bus makes
                                       private val mTrackNumber: Int,
                                       private val mPreviousBalance: Int,
                                       private val mNextBalance: Int?) : Trip() {
    private val estimatedFare
        get() = when (mCardType) {
            0x760500 -> 1150
            0x230100 -> 1275
            else -> null
        }
    internal val estimatedBalance
        get() = mPreviousBalance - (estimatedFare ?: DEFAULT_FARE)

    override fun getStartTimestamp() = ZolotayaKoronaTransitData.parseTime(mTime, mCardType)

    override fun getVehicleID() = "J$mValidator"

    override fun getFare(): TransitCurrency? {
        if (mNextBalance != null) {
            // Happens if one trip is followed by more than one refill
            if (mPreviousBalance - mNextBalance < -500)
                return null
            return TransitCurrency.RUB(mPreviousBalance - mNextBalance)
        }
        return TransitCurrency.RUB(estimatedFare ?: return null)
    }

    override fun getMode() = Trip.Mode.BUS

    companion object {
        fun parse(block: ByteArray, cardType: Int, refill: ZolotayaKoronaRefill?, balance: Int?): ZolotayaKoronaTrip? {
            if (Utils.isAllZero(block))
                return null
            val time = Utils.byteArrayToIntReversed(block, 6, 4)
            var balanceAfter: Int? = null
            if (balance != null) {
                balanceAfter = balance
                if (refill != null && refill.mTime > time)
                    balanceAfter -= refill.mAmount
            }
            return ZolotayaKoronaTrip(
                    mValidator = Utils.getHexString(block, 2, 3),
                    mTime = time,
                    mTrackNumber = Utils.byteArrayToInt(block, 10, 1),
                    mPreviousBalance = Utils.byteArrayToIntReversed(block, 11, 4),
                    mNextBalance = balanceAfter,
                    mCardType = cardType
            )
        }
    }
}
