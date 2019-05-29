/*
 * CEPASTransaction.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.ezlink

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class CEPASTransaction(private val mType: Byte,
                       val amount: Int,
                       val timestamp: Timestamp,
                       val userData: String) : Parcelable {

    val type: TransactionType
        get() = getType(mType)

    constructor(rawData: ImmutableByteArray) : this(
            mType = rawData[0],
            amount = rawData.getBitsFromBufferSigned(8, 24),
            /* Date is expressed "in seconds", but the epoch is January 1 1995, SGT */
            timestamp = EZLinkTransitData.timestampToCalendar(rawData.byteArrayToLong(4, 4)),
            userData = rawData.sliceOffLen(8, 8).readASCII()
    )

    enum class TransactionType {
        MRT,
        TOP_UP, /* Old MRT transit info is unhyphenated - renamed from OLD_MRT to TOP_UP, as it seems like the code has been repurposed. */
        BUS,
        BUS_REFUND,
        CREATION,
        RETAIL,
        SERVICE,
        UNKNOWN
    }

    companion object {
        fun getType(type: Byte): TransactionType = when (type.toInt()) {
            48 -> TransactionType.MRT
            117, 3 -> TransactionType.TOP_UP
            49 -> TransactionType.BUS
            118 -> TransactionType.BUS_REFUND
            -16, 5 -> TransactionType.CREATION
            4 -> TransactionType.SERVICE
            1 -> TransactionType.RETAIL
            else -> TransactionType.UNKNOWN
        }
    }
}
