/*
 * $Header: /u/users20/santtu/src/java/MD5/RCS/MD5.java,v 1.5 1996/12/12 10:47:02 santtu Exp $
 *
 * MD5 in Java JDK Beta-2
 * written Santeri Paavolainen, Helsinki Finland 1996
 * (c) Santeri Paavolainen, Helsinki Finland 1996
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * See http://www.cs.hut.fi/~santtu/java/ for more information on this
 * class.
 *
 * This is rather straight re-implementation of the reference implementation
 * given in RFC1321 by RSA.
 *
 * Passes MD5 test suite as defined in RFC1321.
 *
 *
 * This Java class has been derived from the RSA Data Security, Inc. MD5
 * Message-Digest Algorithm and its reference implementation.
 *
 *
 * Revision 1.6 minor changes for j2me, 2003, Matthias Straub
 *
 * $Log: MD5.java,v $
 * Revision 1.5  1996/12/12 10:47:02  santtu
 * Changed GPL to LGPL
 *
 * Revision 1.4  1996/12/12 10:30:02  santtu
 * Some typos, State -> MD5State etc.
 *
 * Revision 1.3  1996/04/15 07:28:09  santtu
 * Added GPL statemets, and RSA derivate stametemetsnnts.
 *
 * Revision 1.2  1996/03/04 08:05:48  santtu
 * Added offsets to Update method
 *
 * Revision 1.1  1996/01/07 20:51:59  santtu
 * Initial revision
 *
 */

/**
 * Contains internal state of the MD5 class
 */

package au.id.micolous.metrodroid.util

@UseExperimental(ExperimentalUnsignedTypes::class)
class MD5State() {
    /**
     * 128-byte state
     */
    val state: UIntArray = uintArrayOf(0x67452301U, 0xefcdab89U, 0x98badcfeU, 0x10325476U)

    /**
     * 64-bit character count (could be true Java long?)
     */
    val count: IntArray = intArrayOf(0, 0)

    /**
     * 64-byte buffer (512 bits) for storing to-be-hashed characters
     */
    val buffer: ByteArray = ByteArray(64)

    /** Create this State as a copy of another state  */
    constructor(from: MD5State) : this() {
        from.buffer.copyInto(this.buffer)
        from.state.copyInto(this.state)
        from.count.copyInto(this.count)
    }
}

/**
 * Implementation of RSA's MD5 hash generator
 *
 * @version    $Revision: 1.5 $
 * @author    Santeri Paavolainen <sjpaavol></sjpaavol>@cc.helsinki.fi>
 */

@UseExperimental(ExperimentalUnsignedTypes::class)
class MD5Ctx {
    /**
     * MD5 state
     */
    internal val state: MD5State = MD5State()

    private fun rotate_left(x: UInt, n: Int): UInt = (x shl n) or (x.shr(32 - n))

    private fun FF(a: UInt, b: UInt, c: UInt, d: UInt, x: UInt, s: Int, ac: UInt) =
            rotate_left(a + (b and c or (b.inv() and d)) + x + ac, s) + b

    private fun GG(a: UInt, b: UInt, c: UInt, d: UInt, x: UInt, s: Int, ac: UInt) =
            rotate_left(a + (b and d or (c and d.inv())) + x + ac, s) + b

    private fun HH(a: UInt, b: UInt, c: UInt, d: UInt, x: UInt, s: Int, ac: UInt) =
            rotate_left(a + (b xor c xor d) + x + ac, s) + b

    private fun II(a: UInt, b: UInt, c: UInt, d: UInt, x: UInt, s: Int, ac: UInt) =
            rotate_left(a + (c xor (b or d.inv())) + x + ac, s) + b

    private fun Decode(buffer: ImmutableByteArray, shift: Int) =
            UIntArray(16) { buffer.byteArrayToIntReversed(shift + 4 * it, 4).toUInt() }

    private fun Transform(state: MD5State, buffer: ImmutableByteArray, shift: Int) {
        var a = state.state[0]
        var b = state.state[1]
        var c = state.state[2]
        var d = state.state[3]
        val x = Decode(buffer, shift)

        /* Round 1 */
        a = FF(a, b, c, d, x[0], 7, 0xd76aa478U) /* 1 */
        d = FF(d, a, b, c, x[1], 12, 0xe8c7b756U) /* 2 */
        c = FF(c, d, a, b, x[2], 17, 0x242070dbU) /* 3 */
        b = FF(b, c, d, a, x[3], 22, 0xc1bdceeeU) /* 4 */
        a = FF(a, b, c, d, x[4], 7, 0xf57c0fafU) /* 5 */
        d = FF(d, a, b, c, x[5], 12, 0x4787c62aU) /* 6 */
        c = FF(c, d, a, b, x[6], 17, 0xa8304613U) /* 7 */
        b = FF(b, c, d, a, x[7], 22, 0xfd469501U) /* 8 */
        a = FF(a, b, c, d, x[8], 7, 0x698098d8U) /* 9 */
        d = FF(d, a, b, c, x[9], 12, 0x8b44f7afU) /* 10 */
        c = FF(c, d, a, b, x[10], 17, 0xffff5bb1U) /* 11 */
        b = FF(b, c, d, a, x[11], 22, 0x895cd7beU) /* 12 */
        a = FF(a, b, c, d, x[12], 7, 0x6b901122U) /* 13 */
        d = FF(d, a, b, c, x[13], 12, 0xfd987193U) /* 14 */
        c = FF(c, d, a, b, x[14], 17, 0xa679438eU) /* 15 */
        b = FF(b, c, d, a, x[15], 22, 0x49b40821U) /* 16 */

        /* Round 2 */
        a = GG(a, b, c, d, x[1], 5, 0xf61e2562U) /* 17 */
        d = GG(d, a, b, c, x[6], 9, 0xc040b340U) /* 18 */
        c = GG(c, d, a, b, x[11], 14, 0x265e5a51U) /* 19 */
        b = GG(b, c, d, a, x[0], 20, 0xe9b6c7aaU) /* 20 */
        a = GG(a, b, c, d, x[5], 5, 0xd62f105dU) /* 21 */
        d = GG(d, a, b, c, x[10], 9, 0x2441453U) /* 22 */
        c = GG(c, d, a, b, x[15], 14, 0xd8a1e681U) /* 23 */
        b = GG(b, c, d, a, x[4], 20, 0xe7d3fbc8U) /* 24 */
        a = GG(a, b, c, d, x[9], 5, 0x21e1cde6U) /* 25 */
        d = GG(d, a, b, c, x[14], 9, 0xc33707d6U) /* 26 */
        c = GG(c, d, a, b, x[3], 14, 0xf4d50d87U) /* 27 */
        b = GG(b, c, d, a, x[8], 20, 0x455a14edU) /* 28 */
        a = GG(a, b, c, d, x[13], 5, 0xa9e3e905U) /* 29 */
        d = GG(d, a, b, c, x[2], 9, 0xfcefa3f8U) /* 30 */
        c = GG(c, d, a, b, x[7], 14, 0x676f02d9U) /* 31 */
        b = GG(b, c, d, a, x[12], 20, 0x8d2a4c8aU) /* 32 */

        /* Round 3 */
        a = HH(a, b, c, d, x[5], 4, 0xfffa3942U) /* 33 */
        d = HH(d, a, b, c, x[8], 11, 0x8771f681U) /* 34 */
        c = HH(c, d, a, b, x[11], 16, 0x6d9d6122U) /* 35 */
        b = HH(b, c, d, a, x[14], 23, 0xfde5380cU) /* 36 */
        a = HH(a, b, c, d, x[1], 4, 0xa4beea44U) /* 37 */
        d = HH(d, a, b, c, x[4], 11, 0x4bdecfa9U) /* 38 */
        c = HH(c, d, a, b, x[7], 16, 0xf6bb4b60U) /* 39 */
        b = HH(b, c, d, a, x[10], 23, 0xbebfbc70U) /* 40 */
        a = HH(a, b, c, d, x[13], 4, 0x289b7ec6U) /* 41 */
        d = HH(d, a, b, c, x[0], 11, 0xeaa127faU) /* 42 */
        c = HH(c, d, a, b, x[3], 16, 0xd4ef3085U) /* 43 */
        b = HH(b, c, d, a, x[6], 23, 0x4881d05U) /* 44 */
        a = HH(a, b, c, d, x[9], 4, 0xd9d4d039U) /* 45 */
        d = HH(d, a, b, c, x[12], 11, 0xe6db99e5U) /* 46 */
        c = HH(c, d, a, b, x[15], 16, 0x1fa27cf8U) /* 47 */
        b = HH(b, c, d, a, x[2], 23, 0xc4ac5665U) /* 48 */

        /* Round 4 */
        a = II(a, b, c, d, x[0], 6, 0xf4292244U) /* 49 */
        d = II(d, a, b, c, x[7], 10, 0x432aff97U) /* 50 */
        c = II(c, d, a, b, x[14], 15, 0xab9423a7U) /* 51 */
        b = II(b, c, d, a, x[5], 21, 0xfc93a039U) /* 52 */
        a = II(a, b, c, d, x[12], 6, 0x655b59c3U) /* 53 */
        d = II(d, a, b, c, x[3], 10, 0x8f0ccc92U) /* 54 */
        c = II(c, d, a, b, x[10], 15, 0xffeff47dU) /* 55 */
        b = II(b, c, d, a, x[1], 21, 0x85845dd1U) /* 56 */
        a = II(a, b, c, d, x[8], 6, 0x6fa87e4fU) /* 57 */
        d = II(d, a, b, c, x[15], 10, 0xfe2ce6e0U) /* 58 */
        c = II(c, d, a, b, x[6], 15, 0xa3014314U) /* 59 */
        b = II(b, c, d, a, x[13], 21, 0x4e0811a1U) /* 60 */
        a = II(a, b, c, d, x[4], 6, 0xf7537e82U) /* 61 */
        d = II(d, a, b, c, x[11], 10, 0xbd3af235U) /* 62 */
        c = II(c, d, a, b, x[2], 15, 0x2ad7d2bbU) /* 63 */
        b = II(b, c, d, a, x[9], 21, 0xeb86d391U) /* 64 */

        state.state[0] += a
        state.state[1] += b
        state.state[2] += c
        state.state[3] += d
    }

    /**
     * Updates hash with the bytebuffer given (using at maximum length bytes from
     * that buffer)
     *
     * @param stat      Which state is updated
     * @param buffer    Array of bytes to be hashed
     * @param offset    Offset to buffer array
     * @param length    Use at maximum `length' bytes (absolute
     * maximum is buffer.length)
     */
    fun Update(stat: MD5State, buffer: ImmutableByteArray, offset: Int, length: Int) {
        var length = length
        var i: Int

        /* Length can be told to be shorter, but not inter */
        if (length - offset > buffer.size)
            length = buffer.size - offset

        /* compute number of bytes mod 64 */
        var index: Int = stat.count[0].ushr(3) and 0x3f
        stat.count[0] += (length shl 3)

        if (stat.count[0] < (length shl 3))
            stat.count[1]++

        stat.count[1] += length.ushr(29)

        val partlen = 64 - index

        if (length >= partlen) {
            buffer.copyInto(stat.buffer, index, offset, offset + partlen)
            Transform(stat, stat.buffer.toImmutable(), 0)

            i = partlen
            while (i + 63 < length) {
                Transform(stat, buffer, i)
                i += 64
            }

            index = 0
        } else
            i = 0

        /* buffer remaining input */
        if (i < length)
            buffer.copyInto(stat.buffer, index, offset + i, offset + length)
    }

    /*
   * Update()s for other datatypes than byte[] also. Update(byte[], int)
   * is only the main driver.
   */

    /**
     * Plain update, updates this object
     */

    fun update(buffer: ImmutableByteArray, offset: Int = 0, length: Int = buffer.size) {
        Update(this.state, buffer, offset, length)
    }

    private fun Encode(input: UIntArray, len: Int) =
            ImmutableByteArray(len) { (input[it / 4].shr(8 * (it % 4)) and 0xffU).toByte() }

    /**
     * Returns array of bytes (16 bytes) representing hash as of the
     * current state of this object. Note: getting a hash does not
     * invalidate the hash object, it only creates a copy of the real
     * state which is finalized.
     *
     * @return    Array of 16 bytes, the hash of all updated bytes
     */
    fun digest(): ImmutableByteArray {
        val fin = MD5State(state)
        val bits = Encode(fin.count.toUIntArray(), 8)
        val index = fin.count[0].ushr(3) and 0x3f
        val padlen = if (index < 56) 56 - index else 120 - index

        Update(fin, padding, 0, padlen)
        Update(fin, bits, 0, 8)

        return Encode(fin.state, 16)
    }

    companion object {
        /**
         * Padding for Final()
         */
        internal val padding = ImmutableByteArray (64) { if (it == 0) 0x80.toByte() else 0x00}
    }
}
