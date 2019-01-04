/*
 * HexString.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.xml

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.nio.charset.Charset

fun ByteArray.toImmutable(): ImmutableByteArray = ImmutableByteArray.fromByteArray(this)

@Parcelize
open class ImmutableByteArray private constructor(private val mData: ByteArray):
        Parcelable, Comparable<ImmutableByteArray> {
    constructor(len: Int, function: (Int) -> Byte) : this(mData = ByteArray(len, function))
    constructor(imm: ImmutableByteArray): this(mData = imm.mData)
    constructor(len: Int) : this(mData = ByteArray(len))

    val dataCopy: ByteArray
        get() = mData.clone()
    val size
        get() = mData.size

    override fun equals(other: Any?) = other is ImmutableByteArray && mData.contentEquals(other.mData)

    override fun hashCode() = mData.contentHashCode()

    fun toHexString() = Utils.getHexString(mData)

    override fun toString() = "<${toHexString()}>"

    override fun compareTo(other: ImmutableByteArray) = toHexString().compareTo(other.toHexString())
    fun byteArrayToInt(offset: Int, len: Int): Int = Utils.byteArrayToInt(mData, offset, len)
    fun isAllZero(): Boolean = mData.all { it == 0.toByte() }
    fun getBitsFromBuffer(offset: Int, len: Int): Int = Utils.getBitsFromBuffer(mData, offset, len)
    fun getBitsFromBufferLeBits(off: Int, len: Int) = Utils.getBitsFromBufferLeBits(mData, off, len)
    fun getBitsFromBufferSigned(off: Int, len: Int) = Utils.getBitsFromBufferSigned(mData, off, len)
    operator fun get(i: Int) = mData[i]
    fun isNotEmpty() = mData.isNotEmpty()
    operator fun plus(second: ImmutableByteArray) = ImmutableByteArray(this.mData + second.mData)
    fun sliceArray(intRange: IntRange) = ImmutableByteArray(mData = mData.sliceArray(intRange))
    fun sliceOffLen(off: Int, datalen: Int) = sliceArray(off until (off + datalen))
    fun toHexDump() = Utils.getHexDump(mData)
    fun byteArrayToInt() = Utils.byteArrayToInt(mData)
    fun <T>fold(l: T, function: (T, Byte) -> T): T = mData.fold(l, function)
    fun all(function: (Byte) -> Boolean): Boolean = mData.all(function)
    fun any(function: (Byte) -> Boolean): Boolean = mData.any(function)
    fun toBase64(): String = Base64.encodeToString(mData, Base64.NO_WRAP)
    fun getHexString(off: Int, len: Int) = Utils.getHexString(mData, off, len)
    fun readASCII() = readEncoded(Utils.getASCII())
    fun readEncoded(cs: Charset) = String(mData, cs)
    fun byteArrayToIntReversed(off: Int, len: Int) = Utils.byteArrayToIntReversed(mData, off, len)
    fun byteArrayToIntReversed() = byteArrayToIntReversed(0, size)
    fun byteArrayToLongReversed(off: Int, len: Int) = Utils.byteArrayToLongReversed(mData, off, len)
    fun isASCII() = Utils.isASCII(mData)
    fun byteArrayToLong(off: Int, len: Int) = Utils.byteArrayToLong(mData, off, len)
    fun byteArrayToLong() = Utils.byteArrayToLong(mData)
    fun copyOfRange(start: Int, end: Int) = ImmutableByteArray(mData.copyOfRange(start, end))
    fun contentEquals(other: ImmutableByteArray) = mData.contentEquals(other.mData)
    fun reverseBuffer() =
            ImmutableByteArray(ByteArray(mData.size) { x-> mData[mData.size - x - 1] })
    fun contentEquals(other: ByteArray) = mData.contentEquals(other)

    fun parcelize(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(this, flags)
    }

    companion object {
        fun fromHex(hex: String) = ImmutableByteArray(mData = Utils.hexStringToByteArray(hex))
        fun fromByteArray(data: ByteArray) = ImmutableByteArray(mData = data.clone())
        fun fromASCII(s: String) = ImmutableByteArray(mData = s.toByteArray(Utils.getASCII()))
        fun empty() = ImmutableByteArray(mData = byteArrayOf())
        fun fromBase64(value: String) = ImmutableByteArray(mData = Base64.decode(value, Base64.DEFAULT))
        fun fromParcel(parcel: Parcel): ImmutableByteArray =
                parcel.readParcelable(ImmutableByteArray::class.java.classLoader)!!
        fun of(vararg b: Byte) = ImmutableByteArray(mData = b)
    }
}

