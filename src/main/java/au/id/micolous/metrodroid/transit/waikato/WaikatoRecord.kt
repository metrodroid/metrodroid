/*
 * WaikatoRecord.kt
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

import android.os.Parcelable
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize

/**
 * Reader for Waikato BUSIT cards.
 * More info: https://github.com/micolous/metrodroid/wiki/BUSIT
 */
@Parcelize
class WaikatoRecord internal constructor (
        internal val serialNumber : String,
        internal val balance : Int,
        internal val txnNumber: Long
) : Parcelable {
    companion object {
        private fun getSerial(block: ImmutableByteArray) =
                block.getHexString(4, 4)

        internal fun getSerial(card: ClassicCard) = getSerial(card[1][0].data)

        internal fun parseRecord(card: ClassicCard, offset: Int) : WaikatoRecord {
            val serial = getSerial(card[offset][0].data)
            val balance = card[offset + 1][1].data.byteArrayToIntReversed(9, 2)
            val txnNumber = card[offset + 2][0].data.byteArrayToLongReversed(2, 4)

            return WaikatoRecord(serial, balance, txnNumber)
        }
    }
}