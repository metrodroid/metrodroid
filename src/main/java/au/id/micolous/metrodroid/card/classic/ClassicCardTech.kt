package au.id.micolous.metrodroid.card.classic

import android.nfc.tech.MifareClassic
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import au.id.micolous.metrodroid.xml.toImmutable

interface ClassicCardTech {
    fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean
    val sectorCount: Int
    val tagId: ImmutableByteArray
    fun readBlock(block: Int): ImmutableByteArray
    fun getBlockCountInSector(sectorIndex: Int): Int
    fun sectorToBlock(sectorIndex: Int): Int
}

class ClassicCardTechAndroid (private val tech: MifareClassic,
                              override val tagId: ImmutableByteArray): ClassicCardTech {
    constructor(tech: MifareClassic, tagId: ByteArray) : this(tech = tech, tagId = tagId.toImmutable())

    override fun sectorToBlock(sectorIndex: Int) = tech.sectorToBlock(sectorIndex)

    override fun getBlockCountInSector(sectorIndex: Int) = tech.getBlockCountInSector(sectorIndex)

    override fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean {
        if (key.type === ClassicSectorKey.KeyType.A || key.type === ClassicSectorKey.KeyType.UNKNOWN) {
            return tech.authenticateSectorWithKeyA(sectorIndex, key.key.dataCopy)
        }
        return tech.authenticateSectorWithKeyB(sectorIndex, key.key.dataCopy)
    }

    override val sectorCount = tech.sectorCount

    override fun readBlock(block: Int) = tech.readBlock(block).toImmutable()
}