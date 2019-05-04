package au.id.micolous.metrodroid.util

/*
 * Base64 encoding/decoding (RFC1341)
 * Copyright (c) 2005-2011, Jouni Malinen <j@w1.fi>
 *
 * This software may be distributed, used, and modified under the terms of
 * BSD license:

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:

 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.

 * 3. Neither the name(s) of the above-listed copyright holder(s) nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

private const val base64_table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val dtable = base64_table.mapIndexed { k, v -> v to k }.toMap() + mapOf('=' to 0)

/**
 * base64_decode - Base64 decode
 * @src: Data to be decoded
 * Returns: Allocated buffer of out_len bytes of decoded data,
 * or %NULL on failure
 *
 * Caller is responsible for freeing the returned buffer.
 */
fun decodeBase64(src: String): ByteArray?
{
    var pad = 0
    val block = IntArray(4)

    val count = src.count { it in dtable }
    if (count == 0)
        return ByteArray(0)
    if (count % 4 != 0)
        return null

    val olen = count / 4 * 3
    val out = ByteArray(olen)
    var pos = 0

    var blkptr = 0

    mainloop@for (c in src) {
        if (c == '=')
            pad++
        block[blkptr] = dtable[c] ?: continue
        blkptr++
        if (blkptr == 4) {
            out[pos++] = ((block[0] shl 2) or (block[1] shr 4)).toByte()
            out[pos++] = ((block[1] shl 4) or (block[2] shr 2)).toByte()
            out[pos++] = ((block[2] shl 6) or block[3]).toByte()
            blkptr = 0
            if (pad != 0)
                break
        }
    }

    when (pad) {
        0 -> {}
        1 -> pos--
        2 -> pos -= 2
        else -> /* Invalid padding */
            return null
    }

    return out.sliceArray(0..(pos - 1))
}