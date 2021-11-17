/*
 * Atr.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card

import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * ATR (Answer To Reset) parser for PC/SC and standard compact-TLV.
 *
 * Reference: https://en.wikipedia.org/wiki/Answer_to_reset#ATR_in_asynchronous_transmission
 */
data class Atr(
    /** A list of supported protocols, eg: T=0, T=1... */
    val protocols: List<Int>,
    /** PC/SC-specific ATR data, null if not present */
    val pcscAtr: PCSCAtr? = null,
    val cardServiceDataByte: Int? = null,
    val preIssuingData: ImmutableByteArray? = null,
    val statusIndicator: ImmutableByteArray? = null,
    /** Checksum; null if not present. Omitted if only T=0 supported. */
    val checksumByte: Int? = null
) {

    companion object {
        val RID_PCSC_WORKGROUP = ImmutableByteArray.fromHex("a000000306")
        val PCSC_RESPONSE_LEN = 3 /* header */ +
            RID_PCSC_WORKGROUP.count() +
            3 /* PIX, excluding RFU bytes */

        /**
         * Parses an ATR.  Returns null if the data could not be handled.
         */
        fun parseAtr(atr: ImmutableByteArray) : Atr? {
            // println("ATR: ${atr.getHexString()}")

            // Initial character TS
            if (atr[0] != 0x3b.toByte()) return null

            var p = 1
            val nibbles = mutableListOf<Int>()

            do {
                val y = atr[p].toInt() and 0xf0
                nibbles.add(atr[p].toInt() and 0xf)

                val ta = y and 0x10 != 0
                val tb = y and 0x20 != 0
                val tc = y and 0x40 != 0
                val td = y and 0x80 != 0
                // println("$ta, $tb, $tc, $td")
                p++

                // TODO: implement t_(n)
                if (ta) p++
                if (tb) p++
                if (tc) p++
            } while (td)

            val historicalByteCount = nibbles[0]
            val protocols = if (nibbles.size > 1) {
                nibbles.drop(1)
            } else {
                // No protocols specified; T=0
                listOf(0)
            }
            val t1 = atr.sliceOffLenSafe(p, historicalByteCount) ?: return null

            val checksum = if (protocols != listOf(0)) atr[atr.lastIndex].toInt() and 0xff else null

            if (t1.isEmpty()) {
                // No historical bytes
                return null
            }

            // Category indicator byte
            if (t1[0] != 0.toByte() && t1[0] != 0x80.toByte()) {
                Log.w("Atr", "can't handle t1=${t1.getHexString()}")
                return null
            }

            // PC/SC Specification treats this as Simple TLV, rather than Compact TLV.
            // Check for its fingerprints...
            if (t1.count() > PCSC_RESPONSE_LEN && t1[1] == 0x4f.toByte()) {
                // PC/SC T1: 80(category byte) 4f(tag) [length] a000000306(PC/SC RID) ...
                // PC/SC calls everything after from the RID bytes the AID
                val aid = t1.sliceOffLenSafe(3, t1[2].toInt() and 0xff)

                if (aid?.startsWith(RID_PCSC_WORKGROUP) == true) {
                    // This is PC/SC!
                    val i = RID_PCSC_WORKGROUP.count()

                    // ... [standard] [cardName(high)] [cardName(low)]
                    val standard = aid.byteArrayToInt(i, 1)
                    val cardName = aid.byteArrayToInt(i + 1, 2)
                    return Atr(
                        protocols = protocols,
                        pcscAtr = PCSCAtr(standard, cardName),
                        checksumByte = checksum)
                }
            }

            // Handle Compact-TLV tag type
            var cardServiceDataByte: Int? = null
            var preIssuingData: ImmutableByteArray? = null
            var statusIndicator: ImmutableByteArray? = null

            if (t1.count() > 2) {
                ISO7816TLV.compactTlvIterate(t1.drop(1)).forEach {
                    when (it.first) {
                        0x3 -> cardServiceDataByte = it.second.byteArrayToInt() and 0xff
                        0x6 -> preIssuingData = it.second
                        0x8 -> statusIndicator = it.second
                    }
                }
            }

            return Atr(
                protocols = protocols,
                cardServiceDataByte = cardServiceDataByte,
                preIssuingData = preIssuingData,
                statusIndicator = statusIndicator,
                checksumByte = checksum
            )
        }
    }
}
