/*
 * VirtualFelicaLite.kt
 *
 * Copyright 2021 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as pu blished by
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

package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.felica.FelicaConsts
import au.id.micolous.metrodroid.card.felica.FelicaTransceiver
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString
import kotlin.experimental.or
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VirtualFelicaLite(private val card: Card): FelicaTransceiver {
    init {
        assertNotNull(card.felica)
        assertEquals(8, card.tagId.size)
        assertEquals(0, card.tagId[0].toInt() shr 4)
    }

    private val service get() = card.felica?.getSystem(FelicaConsts.SYSTEMCODE_FELICA_LITE)
        ?.getService(FelicaConsts.SERVICE_FELICA_LITE_READONLY)
    private val isNdefCompatimple: Boolean
        get() = service?.getBlock(FelicaConsts.FELICA_LITE_BLOCK_MC)?.data?.get(3) == 1.toByte()
    override val defaultSystemCode: Int
        get() = FelicaConsts.SYSTEMCODE_FELICA_LITE
    override val uid: ImmutableByteArray
        get() = card.tagId
    override val pmm: ImmutableByteArray?
        get() = card.felica?.pMm

    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        assertEquals(data[0].toInt() and 0xff, data.size, "Wrong length byte")
        val command = data[1]
        val args = when (command) {
            FelicaConsts.COMMAND_POLLING -> data.drop(2)
            else -> {
                assertEquals(uid, data.sliceOffLen(2, 8))
                data.drop(10)
            }
        }
        assertNotEquals(FelicaConsts.COMMAND_WRITE_WO_ENCRYPTION, command, "Writing is forbidden")
        when (command) {
            FelicaConsts.COMMAND_POLLING -> when (args) {
                ImmutableByteArray.ofB(
                    FelicaConsts.SYSTEMCODE_FELICA_LITE shr 8,
                    FelicaConsts.SYSTEMCODE_FELICA_LITE and 0xff,
                    0x01, 0x07
                ) -> return ImmutableByteArray.of(10, FelicaConsts.COMMAND_POLLING or 1) + card.tagId
                ImmutableByteArray.ofB(
                    FelicaConsts.SYSTEMCODE_NDEF shr 8,
                    FelicaConsts.SYSTEMCODE_NDEF and 0xff,
                    0x01, 0x07
                ) -> if (isNdefCompatimple)
                    return ImmutableByteArray.of(10, FelicaConsts.COMMAND_POLLING or 1) + card.tagId
                else
                    throw CardLostException("Selecting non-existent service")
                else -> throw CardLostException("Selecting non-existent service")
            }
            FelicaConsts.COMMAND_READ_WO_ENCRYPTION -> {
                assertEquals(6, args.size)
                assertEquals(1, args[0])
                assertEquals(1, args[3])
                assertTrue((args[1] == FelicaConsts.SERVICE_FELICA_LITE_READONLY.toByte() &&
                        args[2] == (FelicaConsts.SERVICE_FELICA_LITE_READONLY shr 8).toByte())
                    || (args[1] == FelicaConsts.SERVICE_FELICA_LITE_READWRITE.toByte() &&
                            args[2] == (FelicaConsts.SERVICE_FELICA_LITE_READWRITE shr 8).toByte()))
                assertEquals(0x80.toByte(), args[4])
                val blockNo = args[5].toInt() and 0xff
                val block = service?.getBlock(blockNo)?.data
                println("service=$service, blockNo=$blockNo, block=$block")
                if (block == null)
                    return ImmutableByteArray.ofB(13, FelicaConsts.COMMAND_READ_WO_ENCRYPTION or 1) +
                            uid + ImmutableByteArray.of(1, 0, 0)
                return ImmutableByteArray.ofB(block.size + 13, FelicaConsts.COMMAND_READ_WO_ENCRYPTION or 1) +
                        uid + ImmutableByteArray.of(0, 0, 0) + block
            }
            else -> throw CardLostException("Invalid command ${command.hexString}")
        }
    }
}