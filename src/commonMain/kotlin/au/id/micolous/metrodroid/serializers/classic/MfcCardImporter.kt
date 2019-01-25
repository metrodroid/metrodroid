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
        var uid: ByteArray? = null
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
                val r = stream.read(blockData)
                if (r <= 0 && blockNum == 0) {
                    // We got to the end of the file.
                    break@sectorloop
                } else if (r != blockData.size) {
                    throw IOException("Incomplete MFC read at sector $sectorNum block $blockNum ($r bytes)")
                }

                if (sectorNum == 0 && blockNum == 0) {
                    // Manufacturer data
                    uid = blockData.sliceArray(0..3)
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
        if (maxSector <= 15) {
            maxSector = 15 // 1K
        } else if (maxSector <= 31) {
            maxSector = 31 // 2K
        } else if (maxSector <= 39) {
            maxSector = 39 // 4K
        }

        // Fill missing sectors as "unauthorised".
        while (sectors.size <= maxSector) {
            sectors.add(UnauthorizedClassicSector())
        }

        return Card(uid!!.toImmutable(),
                TimestampFull.now(), mifareClassic = ClassicCard(sectors))
    }

    companion object {
        private const val MAX_SECTORS = 40
    }
}
