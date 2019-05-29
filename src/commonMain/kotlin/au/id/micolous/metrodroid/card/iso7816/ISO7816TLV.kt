/*
 * ISO7816TLV.kt
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

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray

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
            p += startoffset
            val fulllen = p + alldatalen

            while (p < fulllen) {
                val idlen = getTLVIDLen(buf, p)
                val (lenlen, datalen) = decodeTLVLen(buf, p + idlen)
                yield(Triple(buf.sliceOffLen(p, idlen), // id
                        buf.sliceOffLen(p, idlen + lenlen), // header
                        buf.sliceOffLen(p + idlen + lenlen, datalen))) // data

                p += idlen + lenlen + datalen
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
