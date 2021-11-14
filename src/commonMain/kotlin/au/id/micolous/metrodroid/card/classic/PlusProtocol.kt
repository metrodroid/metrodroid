/*
 * PlusProtocol.kt
 *
 * Copyright 2018 Merlok
 * Copyright 2018 drHatson
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.Aes
import au.id.micolous.metrodroid.util.Cmac
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.card.CardTransceiver

/**
 * Protocol implementation for MIFARE Plus in SL0 / SL3 mode.
 *
 * This is adapted from the implementation in Proxmark3.
 *
 * NOTE: This module is insecure, but it is enough to read cards.
 */
class PlusProtocol private constructor(private val tag: CardTransceiver,
                                       override val sectorCount: Int) : ClassicCardTech {
    var kmac: ImmutableByteArray? = null
    var ti: ImmutableByteArray? = null
    var rctr: Int = 0
    
    override fun getBlockCountInSector(sectorIndex: Int) = if (sectorIndex >= 32) 16 else 4

    override fun sectorToBlock(sectorIndex: Int) = if (sectorIndex < 32) sectorIndex * 4 else
        (16 * sectorIndex - 32 * 12)

    override val tagId: ImmutableByteArray
        get() = tag.uid!!

    private fun aesCmac8(macdata: ImmutableByteArray, key: ImmutableByteArray): ImmutableByteArray {
        val cmac = Cmac.aesCmac(macdata, key)
        return ImmutableByteArray(8) { cmac[2 * it + 1] }
    }

    private fun rotate(input: ImmutableByteArray) = input.drop(1) + input.sliceOffLen(0,1)

    override fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean {
        if (key.key.size != ClassicSectorKey.AES_KEY_LEN) {
            return false
        }
        val keyNum = 2 * sectorIndex + (if (key.type == ClassicSectorKey.KeyType.B) 1 else 0) + 0x4000
        val cmd = ImmutableByteArray.of(0x70, keyNum.toByte(), (keyNum shr 8).toByte(), 0x06,
                0, 0, 0, 0, 0, 0)
        val reply = tag.transceive(cmd)
        if (reply.size != 17 || reply[0] != 0x90.toByte()) {
            Log.w(TAG, "Card response error $reply")
            return false
        }

        val rndA = ImmutableByteArray(16) { (it * it).toByte() } // Doesn't matter
        val rndB = Aes.decryptCbc(reply.sliceOffLen(1, 16), key.key)

        val raw = rndA + rotate(rndB)

        val cmd2 = ImmutableByteArray.of(0x72) + Aes.encryptCbc(raw, key.key)
        val reply2 = tag.transceive(cmd2)

        if (reply2.size < 33) {
            Log.w(TAG, "Card response error $reply2")
            return false
        }

        val raw2 = Aes.decryptCbc(reply2.sliceOffLen(1, 32), key.key)
        val rndAPrime = raw2.sliceOffLen(4, 16)

        if (rndAPrime != rotate(rndA)) {
            return false
        }

        val kmacPlain = rndA.sliceOffLen(7, 5) + rndB.sliceOffLen(7, 5) +
                (rndA.sliceOffLen(0, 5) xor rndB.sliceOffLen(0, 5)) + ImmutableByteArray.of(0x22)
        ti = raw2.sliceOffLen(0, 4)
        kmac = Aes.encryptCbc(kmacPlain, key.key)
        rctr = 0
        return true
    }

    private fun computeMac(input: ImmutableByteArray, ctr: Int): ImmutableByteArray {
        val macdata = ImmutableByteArray.of(input[0], ctr.toByte(), (ctr shr 8).toByte()) +
                ti!! + input.drop(1)
        return aesCmac8(key=kmac!!, macdata=macdata)
    }

    override fun readBlock(block: Int): ImmutableByteArray {
        val cmd = ImmutableByteArray.of(0x33, block.toByte(), 0, 1)
        val reply = tag.transceive(cmd + computeMac(cmd, rctr))
        rctr++

        return reply.sliceOffLen(1, 16)
    }

    override val subType: ClassicCard.SubType
        get() = ClassicCard.SubType.PLUS

    companion object {
        private const val TAG = "PlusProtocol"

        private fun checkSectorPresence(tag: CardTransceiver, sectorIndex: Int): Boolean {
            val keyNum = 2 * sectorIndex + 0x4000
            val cmd = ImmutableByteArray.of(0x70, keyNum.toByte(), (keyNum shr 8).toByte(), 0x06,
                                            0, 0, 0, 0, 0, 0)
            val reply = tag.transceive(cmd)
            return (reply.size == 17 && reply[0] == 0x90.toByte())
        }

        fun connect(tag: CardTransceiver): PlusProtocol? {
            try {
                if (!checkSectorPresence(tag, 0)) {
                    return null
                }
                val capacity = if (checkSectorPresence(tag, 32)) 40 else 32
                return PlusProtocol(tag = tag, sectorCount = capacity)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
