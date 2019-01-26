/*
 * ImmutableByteArray.kt
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2019 Google
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
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import kotlinx.io.OutputStream
import kotlinx.io.charsets.Charset
import kotlinx.io.core.String
import kotlinx.serialization.*

fun ByteArray.toImmutable(): ImmutableByteArray = ImmutableByteArray.fromByteArray(this)

@Parcelize
@Serializable
class ImmutableByteArray private constructor(private val mData: ByteArray) :
        Parcelable, Comparable<ImmutableByteArray>, Collection<Byte> {
    constructor(len: Int, function: (Int) -> Byte) : this(mData = ByteArray(len, function))
    constructor(imm: ImmutableByteArray): this(mData = imm.mData)
    constructor(len: Int) : this(mData = ByteArray(len))

    val dataCopy: ByteArray
        get() = mData.copyOf()
    override val size
        get() = mData.size
    val lastIndex
        get() = mData.lastIndex

    override fun equals(other: Any?) = other is ImmutableByteArray && mData.contentEquals(other.mData)

    override fun hashCode() = mData.contentHashCode()

    fun toHexString() = getHexString(0, size)
    fun getHexString() = getHexString(0, size)

    override fun toString() = "<${toHexString()}>"

    override fun compareTo(other: ImmutableByteArray) = toHexString().compareTo(other.toHexString())
    fun byteArrayToInt(offset: Int, len: Int): Int = byteArrayToLong(offset, len).toInt()
    fun isAllZero(): Boolean = mData.all { it == 0.toByte() }
    fun getBitsFromBuffer(offset: Int, len: Int): Int = getBitsFromBuffer(mData, offset, len)
    fun getBitsFromBufferLeBits(off: Int, len: Int) = getBitsFromBufferLeBits(mData, off, len)
    fun getBitsFromBufferSigned(off: Int, len: Int): Int {
        val unsigned = getBitsFromBuffer(off, len)
        return unsignedToTwoComplement(unsigned, len - 1)
    }

    operator fun get(i: Int) = mData[i]
    fun isNotEmpty() = mData.isNotEmpty()
    operator fun plus(second: ImmutableByteArray) = ImmutableByteArray(this.mData + second.mData)
    operator fun plus(second: ByteArray) = ImmutableByteArray(this.mData + second)
    operator fun plus(second: Byte) = ImmutableByteArray(this.mData + second)
    fun sliceArray(intRange: IntRange) = ImmutableByteArray(mData = mData.sliceArray(intRange))
    fun sliceOffLen(off: Int, datalen: Int) = sliceArray(off until (off + datalen))
    fun toHexDump() = getHexDump(mData, 0, mData.size)
    fun byteArrayToInt() = byteArrayToInt(0, mData.size)
    fun <T> fold(l: T, function: (T, Byte) -> T): T = mData.fold(l, function)
    fun all(function: (Byte) -> Boolean): Boolean = mData.all(function)
    fun any(function: (Byte) -> Boolean): Boolean = mData.any(function)
    fun getHexString(offset: Int, length: Int): String {
        val result = StringBuilder()
        for (i in offset until offset + length) {
            result.append(((mData[i].toInt() and 0xff) or 0x100).toString(16).substring(1))
        }
        return result.toString()
    }

    fun byteArrayToIntReversed(off: Int, len: Int) = byteArrayToLongReversed(off, len).toInt()
    fun byteArrayToIntReversed() = byteArrayToIntReversed(0, size)
    fun byteArrayToLongReversed(off: Int, len: Int) = byteArrayToLong(
            ByteArray(len) { mData[off + len - 1 - it] }, 0, len)

    fun byteArrayToLongReversed() = byteArrayToLongReversed(0, size)
    fun isASCII() = mData.all {
        (it in 0x20..0x7f) || it == 0xd.toByte() || it == 0xa.toByte()
    }

    fun byteArrayToLong(off: Int, len: Int) = byteArrayToLong(mData, off, len)
    fun byteArrayToLong() = byteArrayToLong(0, mData.size)
    fun copyOfRange(start: Int, end: Int) = ImmutableByteArray(mData.copyOfRange(start, end))
    fun contentEquals(other: ImmutableByteArray) = mData.contentEquals(other.mData)
    fun reverseBuffer() =
            ImmutableByteArray(ByteArray(mData.size) { x -> mData[mData.size - x - 1] })

    fun contentEquals(other: ByteArray) = mData.contentEquals(other)

    fun map(function: (Byte) -> Byte) = ImmutableByteArray(
            mData = ByteArray(size) { it -> function(mData[it]) }
    )

    override fun isEmpty() = mData.isEmpty()
    fun addSlice(other: ImmutableByteArray, start: Int, len: Int) = this + other.sliceOffLen(start, len)
    fun readASCII() = readLatin1() // ASCII is subset of Latin-1
    fun readLatin1() = String(mData.map { (it.toInt() and 0xff).toChar() }.filter { it != 0.toChar() }.toCharArray())
    fun sliceOffLenSafe(off: Int, len: Int): ImmutableByteArray? {
        if (off < 0 || len < 0 || off >= size)
            return null
        return sliceOffLen(off, minOf(len, size - off))
    }

    fun last() = mData.last()
    fun copyInto(destination: ByteArray,
                 destinationOffset: Int = 0,
                 startIndex: Int = 0,
                 endIndex: Int = size) {
        mData.copyInto(destination, destinationOffset, startIndex, endIndex)
    }

    override fun contains(element: Byte): Boolean = mData.contains(element)
    override fun containsAll(elements: Collection<Byte>): Boolean =
            elements.all { mData.contains(it) }
    override fun iterator(): Iterator<Byte> = mData.iterator()

    fun readEncoded(cs: Charset) = String(mData, 0, size, cs)

    fun writeTo(os: OutputStream) {
        os.write(mData)
    }

    fun writeTo(os: OutputStream, offset: Int, length: Int) {
        os.write(mData, offset, length)
    }

    @Serializer(forClass = ImmutableByteArray::class)
    companion object : KSerializer<ImmutableByteArray> {
        operator fun Byte.plus(second: ImmutableByteArray) = ImmutableByteArray(
                mData = byteArrayOf(this) + second.mData)

        fun fromHex(hex: String) = ImmutableByteArray(mData = hexStringToByteArray(hex))
        fun fromByteArray(data: ByteArray) = ImmutableByteArray(mData = data.copyOf())
        fun empty() = ImmutableByteArray(mData = byteArrayOf())

        fun of(vararg b: Byte) = ImmutableByteArray(mData = b)
        fun ofB(vararg b: Number) = ImmutableByteArray(b.size) { i -> b[i].toByte() }

        /**
         * Given an unsigned integer value, calculate the two's complement of the value if it is
         * actually a negative value
         *
         * @param input      Input value to convert
         * @param highestBit The position of the highest bit in the number, 0-indexed.
         * @return A signed integer containing it's converted value.
         */
        private fun unsignedToTwoComplement(input: Int, highestBit: Int) =
                if (((input shr highestBit) and 1) == 1)
                    input - (2 shl highestBit)
                else input

        fun getHexDump(b: ByteArray, offset: Int, length: Int): FormattedString {
            val result = StringBuilder()
            for (i in 0 until length) {
                result.append(((b[i + offset].toInt() and 0xff) + 0x100).toString(16).substring(1))
                if (i and 0xf == 0xf)
                    result.append('\n')
                else if (i and 3 == 3 && i and 0xf != 0xf)
                    result.append(' ')
            }
            return FormattedString.monospace(result.toString())
        }

        private fun hexStringToByteArray(s: String): ByteArray {
            if (s.length % 2 != 0) {
                throw IllegalArgumentException("Bad input string: $s")
            }

            return ByteArray(s.length / 2) {
                ((hexDigitToInt(s[2 * it]) shl 4) or hexDigitToInt(s[2 * it + 1])).toByte()
            }
        }

        private fun hexDigitToInt(c: Char): Int = when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            in 'A'..'F' -> c - 'A' + 10
            else -> throw IllegalArgumentException("Bad hex digit $c")
        }

        fun getBitsFromBufferLeBits(buffer: ByteArray, iStartBit: Int, iLength: Int): Int {
            // Note: Assumes little-endian bit-order
            val iEndBit = iStartBit + iLength - 1
            val iSByte = iStartBit / 8
            val iSBit = iStartBit % 8
            val iEByte = iEndBit / 8
            val iEBit = iEndBit % 8

            if (iSByte == iEByte) {
                return (buffer[iEByte].toInt() shr iSBit) and (0xFF shr (8 - iLength))
            }

            var uRet = (buffer[iSByte].toInt() shr iSBit) and (0xFF shr iSBit)

            for (i in (iSByte + 1)..(iEByte - 1)) {
                val t = ((buffer[i].toInt() and 0xFF) shl (((i - iSByte) * 8) - iSBit))
                uRet = uRet or t
            }

            val t = (buffer[iEByte].toInt() and ((1 shl (iEBit + 1)) - 1)) shl (((iEByte - iSByte) * 8) - iSBit)
            uRet = uRet or t

            return uRet
        }

        /* Based on function from mfocGUI by 'Huuf' (http://www.huuf.info/OV/) */
        fun getBitsFromBuffer(buffer: ByteArray, iStartBit: Int, iLength: Int): Int {
            // Note: Assumes big-endian
            val iEndBit = iStartBit + iLength - 1
            val iSByte = iStartBit / 8
            val iSBit = iStartBit % 8
            val iEByte = iEndBit / 8
            val iEBit = iEndBit % 8

            if (iSByte == iEByte) {
                return (buffer[iEByte].toInt() shr (7 - iEBit)) and (0xFF shr (8 - iLength))
            }

            var uRet = (buffer[iSByte].toInt() and (0xFF shr iSBit)) shl (((iEByte - iSByte - 1) * 8) + (iEBit + 1))

            for (i in (iSByte + 1)..(iEByte - 1)) {
                val t = (buffer[i].toInt() and 0xFF) shl (((iEByte - i - 1) * 8) + (iEBit + 1))
                uRet = uRet or t
            }

            val t = (buffer[iEByte].toInt() and 0xFF) shr (7 - iEBit)
            uRet = uRet or t

            return uRet
        }

        fun byteArrayToLong(b: ByteArray, offset: Int, length: Int): Long {
            if (b.size < offset + length)
                throw IllegalArgumentException("offset + length must be less than or equal to b.length")

            var value = 0L
            for (i in 0 until length) {
                val shift = (length - 1 - i) * 8
                value += (b[i + offset].toLong() and 0xFFL) shl shift
            }
            return value
        }

        fun fromASCII(s: String) = ImmutableByteArray(mData = s.map { it.toByte() }.toByteArray())

        override fun serialize(encoder: Encoder, obj: ImmutableByteArray) {
            encoder.encodeString(obj.toHexString())
        }

        override fun deserialize(decoder: Decoder): ImmutableByteArray {
            return fromHex(decoder.decodeString())
        }

        fun fromBase64(input: String) = ImmutableByteArray(mData = decodeBase64(input) ?: throw Exception("Invalid base64: $input"))

        fun getHexString(b: ByteArray): String = getHexString(b, 0, b.size)

        fun getHexString(b: ByteArray, offset: Int, length: Int): String {
            val result = StringBuilder()
            for (i in offset until offset + length) {
                result.append(((b[i].toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
            return result.toString()
        }
    }
}