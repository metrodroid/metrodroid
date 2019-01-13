/*
 * ISO7816TLV.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.xml.ImmutableByteArray

object ISO7816TLV {
    // return: <leadBits, id, idlen>
    private fun getTLVIDLen(buf: ImmutableByteArray, p: Int): Int {
        if (buf[p].toInt() and 0x1f != 0x1f)
            return 1
        var len = 1
        while (buf[p + len++].toInt() and 0x80 != 0);
        return len
    }

    // return lenlen, lenvalue
    private fun decodeTLVLen(buf: ImmutableByteArray, p: Int): IntArray {
        val headByte = buf[p].toInt() and 0xff
        if (headByte shr 7 == 0)
            return intArrayOf(1, headByte and 0x7f)
        val numfollowingbytes = headByte and 0x7f
        return intArrayOf(1 + numfollowingbytes,
                buf.byteArrayToInt(p + 1, numfollowingbytes))
    }

    fun berTlvIterate(buf: ImmutableByteArray,
                      iterator: (id: ImmutableByteArray,
                                 header: ImmutableByteArray,
                                 data: ImmutableByteArray) -> Unit) {
        // Skip ID
        var p = getTLVIDLen(buf, 0)
        val (startoffset, alldatalen) = decodeTLVLen(buf, p)
        p += startoffset
        val fulllen = p + alldatalen

        while (p < fulllen) {
            val idlen = getTLVIDLen(buf, p)
            val (lenlen, datalen) = decodeTLVLen(buf, p + idlen)
            iterator(buf.sliceOffLen(p, idlen),
                    buf.sliceOffLen(p, idlen + lenlen),
                    buf.sliceOffLen(p + idlen + lenlen, datalen))

            p += idlen + lenlen + datalen
        }
    }

    fun pdolIterate(buf: ImmutableByteArray,
                    iterator: (id: ImmutableByteArray,
                               len: Int) -> Unit) {
        var p = 0

        while (p < buf.size) {
            val idlen = getTLVIDLen(buf, p)
            val (lenlen, datalen) = decodeTLVLen(buf, p + idlen)
            iterator(buf.sliceOffLen(p, idlen), datalen)

            p += idlen + lenlen
        }
    }

    fun findBERTLV(buf: ImmutableByteArray, target: String, keepHeader: Boolean): ImmutableByteArray? {
        var result: ImmutableByteArray? = null
        berTlvIterate(buf) { id, header, data ->
            if (id.toHexString() == target) {
                result = if (keepHeader) header + data else data
            }
        }
        return result
    }

    fun infoBerTLV(buf: ImmutableByteArray): List<ListItem> {
        val result = mutableListOf<ListItem>()
        berTlvIterate(buf) { id, header, data ->
            if (id[0].toInt() and 0xe0 == 0xa0)
                try {
                    result.add(ListItemRecursive(id.toHexString(),
                            null, infoBerTLV(header + data)))
                } catch (e: Exception) {
                    result.add(ListItem(id.toHexDump(), data.toHexDump()))
                }
            else
                result.add(ListItem(id.toHexDump(), data.toHexDump()))

        }
        return result
    }

    fun infoWithRaw(buf: ImmutableByteArray) = listOfNotNull(
            ListItemRecursive.collapsedValue("RAW", buf.toHexDump()),
            try {
                ListItemRecursive("TLV", null, infoBerTLV(buf))
            } catch (e: Exception) {
                null
            })

    fun removeTlvHeader(buf: ImmutableByteArray): ImmutableByteArray {
        val p = getTLVIDLen(buf, 0)
        val (startoffset, datalen) = decodeTLVLen(buf, p)
        return buf.sliceOffLen(p+startoffset, datalen)
    }
}
