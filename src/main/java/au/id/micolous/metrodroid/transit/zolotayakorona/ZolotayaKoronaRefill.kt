/*
 * ZolotayaKoronaRefill.kt
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
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class ZolotayaKoronaRefill(internal val mTime: Int,
                                         internal val mAmount: Int,
                                         internal val mCounter: Int,
                                         private val mCardType: Int,
                                         private val mMachineID: Int) : Trip() {
    override fun getStartTimestamp() = ZolotayaKoronaTransitData.parseTime(mTime, mCardType)

    override fun getMachineID() = "J$mMachineID"

    override fun getFare() = TransitCurrency.RUB(-mAmount)

    override fun getMode() = Trip.Mode.TICKET_MACHINE

    companion object {
        fun parse(block: ImmutableByteArray, cardType: Int): ZolotayaKoronaRefill? {
            if (Utils.isAllZero(block))
                return null
            val region = Utils.convertBCDtoInteger(cardType shr 16)
            // known values:
            // 23 -> 1
            // 76 -> 2
            val guessedHighBits = (region + 28) / 39
            return ZolotayaKoronaRefill(
                    // Where are higher bits?
                    // We guess it but we don't know yet
                    mMachineID = Utils.byteArrayToIntReversed(block, 1, 2)
                            or (guessedHighBits shl 16),
                    mTime = Utils.byteArrayToIntReversed(block, 3, 4),
                    mAmount = Utils.byteArrayToIntReversed(block, 7, 4),
                    mCounter = Utils.byteArrayToIntReversed(block, 11, 2),
                    mCardType = cardType
            )
        }
    }
}
