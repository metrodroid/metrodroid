/*
 * NextfareUltralightTrip.kt
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

package au.id.micolous.metrodroid.transit.nextfareul

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.util.ImmutableByteArray

abstract class NextfareUltralightTransaction : Transaction {
    private val mTime: Int
    private val mDate: Int
    protected val mRoute: Int
    protected val mLocation: Int
    private val mBaseDate: Int
    private val mMachineCode: Int
    private val mRecordType: Int
    private val mSeqNo: Int
    val balance: Int
    val expiry: Int

    override val routeNames: List<String>
        get() = listOf(mRoute.toString(16))

    override val station: Station?
        get() = if (mLocation == 0) {
            null
        } else Station.unknown(mLocation)

    override val timestamp: Timestamp?
        get() = NextfareUltralightTransitData.parseDateTime(timezone, mBaseDate, mDate, mTime)

    protected abstract val timezone: MetroTimeZone

    protected abstract val isBus: Boolean

    override val isTapOff: Boolean
        get() = mRecordType == 6 && !isBus

    override val isTapOn: Boolean
        get() = (mRecordType == 2
                || mRecordType == 4
                || mRecordType == 6 && isBus
                || mRecordType == 0x12
                || mRecordType == 0x16)

    override val fare: TransitCurrency?
        get() = null

    constructor(raw: ImmutableByteArray, baseDate: Int) {
        mBaseDate = baseDate
        val timeField = raw.byteArrayToIntReversed(0, 2)
        mRecordType = timeField and 0x1f
        mTime = timeField shr 5
        mDate = raw[2].toInt() and 0xff
        val seqnofield = raw.byteArrayToIntReversed(4, 3)
        mSeqNo = seqnofield and 0x7f
        balance = seqnofield shr 5 and 0x7ff
        expiry = raw[8].toInt()
        mLocation = raw.byteArrayToIntReversed(9, 2)
        mRoute = raw[11].toInt()
        mMachineCode = raw.byteArrayToInt(12, 2)
    }

    // handle wraparound correctly
    fun isSeqNoGreater(other: NextfareUltralightTransaction) =
            mSeqNo - other.mSeqNo and 0x7f < 0x3f

    override fun shouldBeMerged(other: Transaction) = (other is NextfareUltralightTransaction
                && other.mSeqNo == mSeqNo + 1 and 0x7f
                && super.shouldBeMerged(other))

    override fun isSameTrip(other: Transaction) =
            (other is NextfareUltralightTransaction
                && !isBus && !other.isBus
                && mRoute == other.mRoute)

    override fun getAgencyName(isShort: Boolean): String? = null
}
