/*
 * ImmutableByteArray.kt
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2019 Google
 * Copyright (C) 2019 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.multi.nativeFreeze
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.experimental.xor

fun ByteArray.toImmutable(): ImmutableByteArray = ImmutableByteArray.fromByteArray(this)
fun Array<out Number>.toImmutable(): ImmutableByteArray = ImmutableByteArray.ofB(*this)

@Parcelize
@Serializable(with = ImmutableByteArray.Companion::class)
class ImmutableByteArray private constructor(
        private val mData: ByteArray) :
        Parcelable, Comparable<ImmutableByteArray>, Collection<Byte> {
    constructor(len: Int, function: (Int) -> Byte) : this(mData = ByteArray(len, function))
    constructor(imm: ImmutableByteArray): this(mData = imm.mData)
    constructor(len: Int) : this(mData = ByteArray(len))

    init {
        nativeFreeze()
    }

    @Transient
    val dataCopy: ByteArray
        get() = mData.copyOf()
    @Transient
    override val size
        get() = mData.size
    @Transient
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
    fun isAllFF(): Boolean = mData.all { it == 0xff.toByte() }
    fun getBitsFromBuffer(offset: Int, len: Int): Int = getBitsFromBuffer(mData, offset, len)
    fun getBitsFromBufferLeBits(off: Int, len: Int) = getBitsFromBufferLeBits(mData, off, len)
    fun getBitsFromBufferSigned(off: Int, len: Int): Int {
        val unsigned = getBitsFromBuffer(off, len)
        return unsignedToTwoComplement(unsigned, len - 1)
    }
    fun getBitsFromBufferSignedLeBits(off: Int, len: Int): Int {
        val unsigned = getBitsFromBufferLeBits(off, len)
        return unsignedToTwoComplement(unsigned, len - 1)
    }
    fun convertBCDtoInteger() : Int = fold(0) {
        x, y -> (x * 100) + NumberUtils.convertBCDtoInteger(y)
    }
    fun convertBCDtoInteger(offset: Int, len: Int) : Int = sliceOffLen(offset, len).convertBCDtoInteger()
    fun convertBCDtoLong() : Long = fold(0L) {
        x, y -> (x * 100L) + NumberUtils.convertBCDtoInteger(y).toLong()
    }
    fun convertBCDtoLong(offset: Int, len: Int) : Long = sliceOffLen(offset, len).convertBCDtoLong()

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
    fun startsWith(other: ByteArray) =
            mData.size >= other.size &&
            mData.sliceArray(other.indices).contentEquals(other)
    fun startsWith(other: ImmutableByteArray) = startsWith(other.mData)

    /**
     * Returns the first index of [needle], or `-1` if it does not contain [needle].
     *
     * @param needle Array to search for
     * @param start Index to start searching from. Defaults to the start of the array.
     * @param end Index to end searching at. Defaults to the end of the array.
     */
    fun indexOf(needle: ImmutableByteArray, start: Int = 0, end: Int = size): Int {
        val needleSize = needle.size

        if (start < 0 || start > lastIndex || end > size || start > end) {
            // Impossible request
            return -1
        } else if (needle.isEmpty()) {
            // We can search for nothing in the space of something
            return start
        } else if ((start + needleSize) == end) {
            // Optimise -- do a substring check
            return if (start == 0 && end == size) {
                // Whole string check
                if (contentEquals(needle)) 0 else -1
            } else {
                // Need to slice first
                if (sliceArray(start until end).contentEquals(needle)) start else -1
            }
        } else if ((start + needleSize) > end) {
            // We can't possibly fulfill that request
            return -1
        }

        var p = 0
        for (i in start until end) {
            if ((i + needleSize - p) > end) {
                // Can't possibly find a good match now.
                break
            }

            if (mData[i] == needle[p]) {
                p++
                if (p == needleSize) {
                    // Success!
                    return i + 1 - needleSize
                } else {
                    // Need more...
                    continue
                }
            } else {
                p = 0
            }
        }

        // Failure
        return -1
    }

    fun map(function: (Byte) -> Byte) = ImmutableByteArray(
            mData = ByteArray(size) { function(mData[it]) }
    )

    override fun isEmpty() = mData.isEmpty()
    fun addSlice(other: ImmutableByteArray, start: Int, len: Int) = this + other.sliceOffLen(start, len)
    fun readASCII() = readLatin1() // ASCII is subset of Latin-1
    fun readLatin1() = mData.map { (it.toInt() and 0xff).toChar() }.filter { it != 0.toChar() }.toCharArray()
        .concatToString()
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

    fun writeTo(os: Output) {
        os.write(mData)
    }

    fun writeTo(os: Output, offset: Int, length: Int) {
        os.write(mData, offset, length)
    }

    fun chunked(size: Int): List<ImmutableByteArray>
            = chunked(size).map {
        it.toByteArray().toImmutable()
    }

    infix fun xor(other: ImmutableByteArray) = ImmutableByteArray(size) {
        mData[it] xor other[it]
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = ImmutableByteArray::class)
    companion object : KSerializer<ImmutableByteArray> {
        operator fun Byte.plus(second: ImmutableByteArray) = ImmutableByteArray(
                mData = byteArrayOf(this) + second.mData)

        fun fromHex(hex: String) = ImmutableByteArray(mData = hexStringToByteArray(hex))
        fun fromByteArray(data: ByteArray) = ImmutableByteArray(mData = data.copyOf())
        fun empty() = ImmutableByteArray(mData = byteArrayOf())
        fun empty(length: Int = 0) = ImmutableByteArray(mData = ByteArray(length))

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

            for (i in (iSByte + 1) until iEByte) {
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

            for (i in (iSByte + 1) until iEByte) {
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

        fun fromASCII(s: String) = ImmutableByteArray(mData = s.map { it.code.toByte() }.toByteArray())

        fun fromUTF8(s: String) = ImmutableByteArray(mData = s.encodeToByteArray())

        override fun serialize(encoder: Encoder, value: ImmutableByteArray) {
            encoder.encodeString(value.toHexString())
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