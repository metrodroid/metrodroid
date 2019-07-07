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
import au.id.micolous.metrodroid.util.Preferences

/**
 * Utilities for decoding BER-TLV values.
 *
 * Reference: https://en.wikipedia.org/wiki/X.690#BER_encoding
 */
object ISO7816TLV {
    private const val TAG = "ISO7816TLV"
    private const val MAX_TLV_FIELD_LENGTH = 0xffff

    /**
     * Gets the _length_ of a TLV tag identifier octets.
     *
     * @param buf TLV data buffer
     * @param p Offset within [buf] to read from
     * @return The number of bytes for this tag's identifier octets.
     */
    private fun getTLVIDLen(buf: ImmutableByteArray, p: Int): Int {
        // One byte version: if the lower 5 bits (tag number) != 0x1f.
        if (buf[p].toInt() and 0x1f != 0x1f)
            return 1

        // Multi-byte version: if the first byte has the lower 5 bits == 0x1f then subsequent
        // bytes contain the tag number. Bit 8 is set when there (is/are) more byte(s) for the
        // tag number.
        var len = 1
        while (buf[p + len++].toInt() and 0x80 != 0);
        return len
    }

    /**
     * Decodes the length octets for a TLV tag.
     *
     * @param buf TLV data buffer
     * @param p Offset within [buf] to start reading the length octets from
     * @return A `[Triple]<[Int], [Int]>`: the number of bytes for this tag's length octets, the
     * length of the _contents octets_, and the length of the _end of contents octets_.
     *
     * Returns `null` if invalid.
     */
    private fun decodeTLVLen(buf: ImmutableByteArray, p: Int): Triple<Int, Int, Int>? {
        val headByte = buf[p].toInt() and 0xff
        if (headByte shr 7 == 0) {
            // Definite, short form (1 byte)
            // Length is the lower 7 bits of the first byte
            return Triple(1, headByte and 0x7f, 0)
        }

        // Decode other forms
        val numfollowingbytes = headByte and 0x7f
        if (numfollowingbytes == 0) {
            // Indefinite form.
            // Value is terminated by two NULL bytes.
            val endPos = buf.indexOf(ImmutableByteArray.empty(2), p + 1)
            return if (endPos == -1) {
                // No null terminators!
                Log.e(TAG, "No null terminator for indef tag at $p!")
                null
            } else {
                Triple(1, endPos - p - 1, 2)
            }
        } else if (numfollowingbytes >= 8) {
            // Definite, long form
            //
            // We got 8 or more following bytes for storing the length.  We can only decode
            // this if all bytes but the last 8 are NULL, and the 8th-to-last top bit is also 0.
            val topBytes = buf.sliceOffLen(p + 1, numfollowingbytes - 8)
            if (!(topBytes.isEmpty() || topBytes.isAllZero()) ||
                    buf[p + 1 + numfollowingbytes - 7].toInt() and 0x80 == 0x80) {
                // We can't decode
                Log.e(TAG, "Definite long form at $p is too big for signed long " +
                        "($numfollowingbytes), cannot decode!")
                return null
            }
        }

        // Definite form, long form
        val length = buf.byteArrayToInt(p + 1, numfollowingbytes)

        if (length > MAX_TLV_FIELD_LENGTH) {
            // 64 KiB field length is enough for anyone(tm). :)

            Log.e(TAG, "Definite long form at $p of > $MAX_TLV_FIELD_LENGTH bytes " +
                    "($length)")
            return null
        } else if (length < 0) {
            // Shouldn't get negative values either.
            Log.e(TAG, "Definite log form at $p has negative value? ($length)")
            return null
        }

        return Triple(1 + numfollowingbytes, length, 0)
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
            // Skip null bytes at start:
            // "Before, between, or after TLV-coded data objects, '00' bytes without any meaning may
            // occur (for example, due to erased or modified TLV-coded data objects)."
            var p = buf.indexOfFirst { it != 0.toByte() }

            if (p == -1) {
                // No non-null bytes
                return@sequence
            }

            // Skip ID
            p = getTLVIDLen(buf, 0)
            val (startoffset, alldatalen, alleoclen) = decodeTLVLen(buf, p) ?: return@sequence
            if (p < 0 || startoffset < 0 || alldatalen < 0 || alleoclen < 0) {
                Log.w(TAG, "Invalid TLV reading header: p=$p, startoffset=$startoffset, " +
                        "alldatalen=$alldatalen, alleoclen=$alleoclen")
                return@sequence
            }

            p += startoffset
            val fulllen = p + alldatalen

            while (p < fulllen) {
                // Skip null bytes
                if (buf[p] == 0.toByte()) {
                    p++
                    continue
                }

                val idlen = getTLVIDLen(buf, p)

                if (p + idlen >= buf.size) break // EOF
                val id = buf.sliceOffLenSafe(p, idlen)
                if (id == null) {
                    Log.w(TAG, "Invalid TLV ID data at $p: out of bounds")
                    break
                }

                // Log.d(TAG, "($p) id=${id.getHexString()}")

                val (hlen, datalen, eoclen) = decodeTLVLen(buf, p + idlen) ?: break

                if (idlen < 0 || hlen < 0 || datalen < 0 || eoclen < 0) {
                    // Invalid lengths, abort!
                    Log.w(TAG, "Invalid TLV data at $p (<0): idlen=$idlen, hlen=$hlen, " +
                            "datalen=$datalen, eoclen=$eoclen")
                    break
                }

                val header = buf.sliceOffLenSafe(p, idlen + hlen)
                val data = buf.sliceOffLenSafe(p + idlen + hlen, datalen)

                if (header == null || data == null) {
                    // Invalid ranges, abort!
                    Log.w(TAG, "Invalid TLV data at $p: out of bounds")
                    break
                }

                if ((id.isAllZero() || id.isEmpty()) && (header.isEmpty() || header.isAllZero())
                        && data.isEmpty()) {
                    // Skip empty tag
                    continue
                }

                // Log.d(TAG, "($p) id=${id.toHexString()}, header=${header.getHexString()}, " +
                //         "data=${data.getHexString()}")
                yield(Triple(id, header, data))
                p += idlen + hlen + datalen + eoclen
            }
        }
    }

    // TODO: Replace with Sequence
    /**
     * Iterates over Processing Options Data Object List (PDOL), tag 9f38.
     *
     * This is a list of tags needed by the ICC for the GET PROCESSING OPTIONS (GPO) command.
     *
     * The lengths in this context are the expected length in the request.
     */
    fun pdolIterate(buf: ImmutableByteArray,
                    iterator: (id: ImmutableByteArray,
                               len: Int) -> Unit) {
        var p = 0

        while (p < buf.size) {
            val idlen = getTLVIDLen(buf, p)
            if (idlen < 0) break
            val (lenlen, datalen, eoclen) = decodeTLVLen(buf, p + idlen) ?: break
            if (lenlen < 0 || datalen < 0 || eoclen != 0) break
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
        val (startoffset, datalen, _) = decodeTLVLen(buf, p) ?: return ImmutableByteArray.empty()
        return buf.sliceOffLen(p+startoffset, datalen)
    }

    private fun makeListItem(tagDesc: TagDesc, data: ImmutableByteArray) : ListItem? {
        return when (tagDesc.contents) {
            TagContents.HIDE -> null
            // TODO: Move into TagDesc
            TagContents.DUMP_LONG -> if (Preferences.hideCardNumbers) {
                null
            } else {
                ListItem(tagDesc.name, data.toHexDump())
            }
            else -> {
                val v = tagDesc.interpretTag(data)
                when {
                    v.isEmpty() -> null
                    else -> ListItem(tagDesc.name, v)
                }
            }
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
