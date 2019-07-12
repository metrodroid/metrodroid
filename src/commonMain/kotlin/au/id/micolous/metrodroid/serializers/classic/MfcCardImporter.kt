package au.id.micolous.metrodroid.serializers.classic

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.card.classic.ClassicSectorRaw
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.serializers.CardImporter
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import kotlinx.io.IOException
import kotlinx.io.InputStream

class MfcCardImporter : CardImporter {
    override fun readCard(stream: InputStream): Card {
        // Read the blocks of the card.
        val sectors = mutableListOf<ClassicSector>()
        var uid: ImmutableByteArray? = null
        var maxSector = 0

        sectorloop@ for (sectorNum in 0 until MAX_SECTORS) {
            val blocks = mutableListOf<ImmutableByteArray>()
            var keyA: ByteArray? = null
            var keyB: ByteArray? = null

            var blockCount = 4
            if (sectorNum >= 32) {
                blockCount = 16
            }

            for (blockNum in 0 until blockCount) {
                val blockData = ByteArray(16)
                var readBytes = 0
                while(readBytes < blockData.size){
                    val buf = if (readBytes == 0) blockData else ByteArray(blockData.size - readBytes)
                    val r = stream.read(buf)
                    if (r <= 0 && blockNum == 0) {
                        // We got to the end of the file.
                        break@sectorloop
                    }
                    if (r <= 0) {
                        throw IOException("Incomplete MFC read at sector $sectorNum block $blockNum ($readBytes bytes)")
                    }

                    if (buf != blockData)
                        buf.copyInto(blockData, readBytes, 0, r)

                    readBytes += r
                }

                if (sectorNum == 0 && blockNum == 0) {
                    // Manufacturer data
                    uid = block0ToUid(blockData.toImmutable())
                } else if (blockNum == blockCount - 1) {
                    keyA = blockData.sliceArray(0..5)
                    keyB = blockData.sliceArray(10..15)
                }
                blocks.add(blockData.toImmutable())

            }

            val raw = ClassicSectorRaw(blocks, keyA?.toImmutable(),
                    keyB?.toImmutable(), false, null)
            sectors.add(ClassicSector.create(raw))
            maxSector = sectorNum
        }

        // End of file, now see how many blocks we get
        when {
            maxSector <= 15 -> maxSector = 15 // 1K
            maxSector <= 31 -> maxSector = 31 // 2K
            maxSector <= 39 -> maxSector = 39 // 4K
        }

            // Fill missing sectors as "unauthorised".

        // Fill missing sectors as "unauthorised".
        while (sectors.size <= maxSector) {
            sectors.add(UnauthorizedClassicSector())
        }

        return Card(uid!!,
                TimestampFull.now(), mifareClassic = ClassicCard(sectors))
    }

    companion object {
        private const val MAX_SECTORS = 40

        fun block0ToUid(block0: ImmutableByteArray) =
                if (block0[0] == 4.toByte() && block0.byteArrayToInt(8, 2) in listOf(0x0400, 0x4400))
                    block0.copyOfRange(0, 7)
                else
                    block0.copyOfRange(0, 4)
    }
}
