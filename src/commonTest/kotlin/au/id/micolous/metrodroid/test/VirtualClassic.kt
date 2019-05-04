package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCardTech
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable

class VirtualClassic(private val raw: ImmutableByteArray) : ClassicCardTech {
    // Currently authenticated sector, -1 if none
    private var authSector: Int = -1
    // Currently authenticated sector key
    private var authKey: ClassicSectorKey.KeyType = ClassicSectorKey.KeyType.UNKNOWN
    internal var authCounter: Int = 0
        private set
    internal var readCounter: Int = 0
        private set
    override val tagId: ImmutableByteArray
        get() = raw.sliceOffLen(0, if (raw[0] == 4.toByte()) 7 else 4)

    private fun getKey(sectorIndex: Int, keyType: ClassicSectorKey.KeyType): ImmutableByteArray {
        val trailerBlock = sectorToBlock(sectorIndex) + getBlockCountInSector(sectorIndex) - 1
        val trailerOffset = BLOCK_SIZE * trailerBlock
        val keyOffset = when (keyType) {
            ClassicSectorKey.KeyType.A -> 0
            ClassicSectorKey.KeyType.B -> 10
            else -> throw IllegalArgumentException()
        }
        return raw.sliceOffLen(trailerOffset + keyOffset, 6)
    }

    private fun getAccessBits(sectorIndex: Int): ImmutableByteArray {
        val trailerBlock = sectorToBlock(sectorIndex) + getBlockCountInSector(sectorIndex) - 1
        val trailerOffset = BLOCK_SIZE * trailerBlock
        return raw.sliceOffLen(trailerOffset + 6, 4)
    }

    data class AccBits(val C1: Int, val C2: Int, val C3: Int) {
        operator fun get(slot: Int) =
                (((C1 shr slot) and 0x1) shl 2) or
                (((C2 shr slot) and 0x1) shl 1) or
                ((C3 shr slot) and 0x1)

        constructor(raw: ImmutableByteArray) : this(
                C1 = (raw[1].toInt() and 0xf0) shr 4,
                C2 = raw[2].toInt() and 0xf,
                C3 = (raw[2].toInt() and 0xf0) shr 4
        )
    }

    private fun isDataBlockReadable(accBits: ImmutableByteArray, slot: Int,
                                    keyType: ClassicSectorKey.KeyType) =
            when (AccBits(accBits)[slot]) {
                0, 1, 2, 4, 6 -> true
                3, 5 -> keyType == ClassicSectorKey.KeyType.B
                7 -> false
                else -> throw IllegalArgumentException()
            }

    private fun isKeyBReadable(accBits: ImmutableByteArray) = AccBits(accBits)[3] in listOf(0, 1, 2)

    private fun maskOutTrailer(accBits: ImmutableByteArray, block: ImmutableByteArray): ImmutableByteArray {
        val keyZero = ImmutableByteArray(6) { 0 }
        if (isKeyBReadable(accBits))
            return keyZero + block.sliceOffLen(6, 4) + keyZero
        else
            return keyZero + block.sliceOffLen(6, 10)
    }

    private fun isAccBitsValid(accBits: ImmutableByteArray): Boolean {
        val c123inv = (accBits[0].toInt() and 0xff) or ((accBits[1].toInt() and 0xf) shl 8)
        val c123 = ((accBits[1].toInt() and 0xf0) shr 4) or ((accBits[2].toInt() and 0xff) shl 4)
        return c123inv == c123.inv() and 0xfff
    }

    override fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean {
        authCounter++
        val type = key.type
        val accBits = getAccessBits(sectorIndex)
        Log.d(TAG, "auth ${key.key}, ${key.type} vs ${getKey(sectorIndex, type)}, accbits=$accBits")
        if (key.key != getKey(sectorIndex, type)
                // TODO: verify behaviour in these 2 cases on a real card
                || (key.type == ClassicSectorKey.KeyType.B && isKeyBReadable(accBits))
                || !isAccBitsValid(accBits)) {
            authSector = -1
            authKey = ClassicSectorKey.KeyType.UNKNOWN
            return false
        }
        authSector = sectorIndex
        authKey = type
        return true
    }

    override val sectorCount
        get() = when (raw.size) {
            1024 -> 16
            2048 -> 32
            4096 -> 40
            else -> throw IllegalArgumentException()
        }

    override fun readBlock(block: Int): ImmutableByteArray {
        readCounter++
        val sectorIdx = blockToSector(block)
        // TODO: verify behaviour in this case on real card
        if (authSector != sectorIdx)
            return byteArrayOf(4).toImmutable()
        val blockOffset = block - sectorToBlock(sectorIdx)
        val blockContents = raw.sliceOffLen(BLOCK_SIZE * block, BLOCK_SIZE)
        val accBits = getAccessBits(sectorIdx)
        val blkCnt = getBlockCountInSector(sectorIdx)
        if (!isAccBitsValid(accBits))
            return byteArrayOf(4).toImmutable()
        if (blockOffset == blkCnt - 1) {
            return maskOutTrailer(accBits, blockContents)
        }
        val slot = when(blkCnt) {
            4 -> blockOffset
            16 -> blockOffset / 5
            else -> throw IllegalArgumentException()
        }
        if (!isDataBlockReadable(accBits, slot, authKey))
            return byteArrayOf(4).toImmutable()
        return blockContents
    }

    private fun blockToSector(block: Int): Int =
            if (block < 128) block / 4 else ((block + 32*12) / 16)

    override fun getBlockCountInSector(sectorIndex: Int) = if (sectorIndex >= 32) 16 else 4

    override fun sectorToBlock(sectorIndex: Int) = if (sectorIndex < 32) sectorIndex * 4 else
        (16 * sectorIndex - 32 * 12)

    companion object {
        internal const val BLOCK_SIZE = 16
        private const val TAG = "VirtualClassic"
    }
}