/*
 * ISO7816TLV.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray

object ISO7816TLV {
    private const val TAG = "ISO7816TLV"

    /**
     * Gets the length of a TLV Tag ID
     *
     */
    private fun getTLVIDLen(buf: ImmutableByteArray, p: Int): Int {
        var s = p
        // Seek past null bytes.
        // "Before, between or after TLV-coded data
        while (buf[s] == 0.toByte()) {
            s++
            if (s > buf.lastIndex) {
                // EOF
                return s
            }
        }

        if (buf[s++].toInt() and 0x1f != 0x1f)
            return s

        while (buf[s++].toInt() and 0x80 != 0) {
            if (s > buf.lastIndex) {
                // EOF
                return s
            }
        }
        return s
    }

    // return lenlen, lenvalue
    private fun decodeTLVLen(buf: ImmutableByteArray, p: Int): IntArray {
        val headByte = buf[p].toInt() and 0xff
        if (headByte shr 7 == 0)
            return intArrayOf(1, headByte and 0x7f)
        val numfollowingbytes = headByte and 0x7f
        if (numfollowingbytes > 2) {
            // We got 2 or more following bytes for storing the length. This is highly unlikely, and
            // it is more likely we parsed some data that is not TLV.
            //
            // 64 KiB field length is likely enough for anyone(tm). :)
            //
            // Lets consume the remaining bytes as a fake header.
            Log.w(TAG, "long length form at position $p of > 2 bytes ($numfollowingbytes)")
            return intArrayOf(buf.size - p, 0)
        }

        val datalen = buf.byteArrayToInt(p + 1, numfollowingbytes)
        if (datalen < 0) {
            // Data lengths less than 0 don't make sense, bail.
            Log.w(TAG, "long length form at position $p parses to < 0 bytes ($datalen)")
            return intArrayOf(buf.size - p, 0)
        }

        return intArrayOf(1 + numfollowingbytes, datalen)
    }

    /**
     * Iterates over BER-TLV encoded data lazily with a [Sequence].
     *
     * @param buf The BER-TLV encoded data to iterate over
     * @return [Sequence] of [Triple] of `id, header, data`
     */
    fun berTlvIterate(buf: ImmutableByteArray) :
            Sequence<Triple<ImmutableByteArray, ImmutableByteArray, ImmutableByteArray>> {
        return sequence {
            // Skip ID
            var p = getTLVIDLen(buf, 0)
            val (startoffset, alldatalen) = decodeTLVLen(buf, p)
            if (p < 0 || startoffset < 0 || alldatalen < 0) {
                Log.w(TAG, "Invalid TLV reading header: p=$p, startoffset=$startoffset, " +
                        "alldatalen=$alldatalen")
                return@sequence
            }

            p += startoffset
            val fulllen = p + alldatalen

            while (p < fulllen) {
                val idlen = getTLVIDLen(buf, p)
                // todo check past eof
                val (hlen, datalen) = decodeTLVLen(buf, p + idlen)

                if (idlen < 0 || hlen < 0 || datalen < 0) {
                    // Invalid lengths, abort!
                    Log.w(TAG, "Invalid TLV data at $p: idlen=$idlen, hlen=$hlen, datalen=$datalen")
                    break
                }

                val id = buf.sliceOffLenSafe(p, idlen)
                val header = buf.sliceOffLenSafe(p, idlen + hlen)
                val data = buf.sliceOffLenSafe(p + idlen + hlen, datalen)

                if (id == null || header == null || data == null) {
                    // Invalid ranges, abort!
                    Log.w(TAG, "Invalid TLV data at $p: out of bounds")
                    break
                }

                // todo: split nulls from start for gettlvidlen change

                if ((id.isAllZero() || id.isEmpty()) && (header.isEmpty() || header.isAllZero())
                        && data
                                .isEmpty()) {
                    // Skip empty tag
                    continue
                }

                Log.d(TAG, "id=${id.toHexString()}, header=${header.getHexString()}, data=${data
                        .getHexString()}")
                yield(Triple(id, header, data))
                p += idlen + hlen + datalen
            }
        }
    }

    // TODO: Replace with Sequence
    fun pdolIterate(buf: ImmutableByteArray,
                    iterator: (id: ImmutableByteArray,
                               len: Int) -> Unit) {
        var p = 0

        while (p < buf.size) {
            val idlen = getTLVIDLen(buf, p)
            // todo check past eof
            val (lenlen, datalen) = decodeTLVLen(buf, p + idlen)
            iterator(buf.sliceOffLen(p, idlen), datalen)

            p += idlen + lenlen
        }
    }

    fun findBERTLV(buf: ImmutableByteArray, target: String, keepHeader: Boolean): ImmutableByteArray? {
        return findBERTLV(buf, ImmutableByteArray.fromHex(target), keepHeader)
    }

    fun findBERTLV(buf: ImmutableByteArray, target: ImmutableByteArray, keepHeader: Boolean):
            ImmutableByteArray? {
        val result = berTlvIterate(buf).firstOrNull { it.first == target } ?: return null

        return if (keepHeader) {
            result.second + result.third
        } else {
            result.third
        }
    }

    fun findRepeatedBERTLV(buf: ImmutableByteArray, target: String, keepHeader: Boolean):
            Sequence<ImmutableByteArray> {
        return findRepeatedBERTLV(buf, ImmutableByteArray.fromHex(target), keepHeader)
    }

    fun findRepeatedBERTLV(
            buf: ImmutableByteArray, target: ImmutableByteArray, keepHeader: Boolean):
            Sequence<ImmutableByteArray> {
        return berTlvIterate(buf).filter { it.first == target }.map {
            if (keepHeader) {
                it.second + it.third
            } else {
                it.third
            }
        }
    }

    /**
     * Parses BER-TLV data, and builds [ListItem] and [ListItemRecursive] for each of the tags.
     */
    fun infoBerTLV(buf: ImmutableByteArray): List<ListItem> {
        return berTlvIterate(buf).map { (id, header, data) ->
            if (id[0].toInt() and 0xe0 == 0xa0) {
                try {
                    ListItemRecursive(id.toHexString(),
                            null, infoBerTLV(header + data))
                } catch (e: Exception) {
                    ListItem(id.toHexDump(), data.toHexDump())
                }
            } else {
                ListItem(id.toHexDump(), data.toHexDump())
            }
        }.toList()
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
        // todo check if past eof
        val (startoffset, datalen) = decodeTLVLen(buf, p)
        return buf.sliceOffLen(p+startoffset, datalen)
    }

    private fun makeListItem(tagDesc: TagDesc, data: ImmutableByteArray) : ListItem? {
        return when (tagDesc.contents) {
            TagContents.HIDE -> null
            TagContents.DUMP_LONG ->
                ListItem(tagDesc.name, data.toHexDump())
            else -> ListItem(tagDesc.name, tagDesc.interpretTag(data))
        }
    }

    /**
     * Parses BER-TLV data, and builds [ListItem] for each of the tags.
     *
     * This replaces the names with human-readable names, and does not operate recursively.
     * @param includeUnknown If `true`, include tags that did not appear in [tagMap]
     */
    fun infoBerTLV(tlv: ImmutableByteArray,
                   tagMap: Map<String, TagDesc>,
                   includeUnknown: Boolean = false) =
            berTlvIterate(tlv).mapNotNull { (id, _, data) ->
                val idStr = id.toHexString()

                val d = tagMap[idStr]
                if (d == null) {
                    if (includeUnknown) {
                        ListItem(FormattedString(idStr), data.toHexDump())
                    } else {
                        null
                    }
                } else {
                    makeListItem(d, data)
                }
            }.toList()

    /**
     * Like [infoBerTLV], but also returns a list of IDs that were unknown in the process.
     */
    fun infoBerTLVWithUnknowns(tlv: ImmutableByteArray, tagMap: Map<String, TagDesc>):
            Pair<List<ListItem>, Set<String>> {
        val unknownIds = mutableSetOf<String>()

        return Pair(berTlvIterate(tlv).mapNotNull { (id, _, data) ->
            val idStr = id.toHexString()

            val d = tagMap[idStr]
            if (d == null) {
                unknownIds.add(idStr)
                ListItem(FormattedString(idStr), data.toHexDump())
            } else {
                makeListItem(d, data)
            }
        }.toList(), unknownIds.toSet())
    }
}
