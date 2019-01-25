package au.id.micolous.metrodroid.card.classic

import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.toImmutable
import java.io.IOException

class ClassicCardTechAndroid(private val tech: MifareClassic,
                             override val tagId: ImmutableByteArray) : ClassicCardTech {
    override fun sectorToBlock(sectorIndex: Int) = tech.sectorToBlock(sectorIndex)

    override fun getBlockCountInSector(sectorIndex: Int) = tech.getBlockCountInSector(sectorIndex)

    override fun authenticate(sectorIndex: Int, key: ClassicSectorKey): Boolean {
        try {
            if (key.type === ClassicSectorKey.KeyType.A || key.type === ClassicSectorKey.KeyType.UNKNOWN) {
                return tech.authenticateSectorWithKeyA(sectorIndex, key.key.dataCopy)
            }
            return tech.authenticateSectorWithKeyB(sectorIndex, key.key.dataCopy)
        } catch (e: TagLostException) {
            throw CardLostException(Utils.getErrorMessage(e))
        } catch (e: IOException) {
            throw CardTransceiveException(e, Utils.getErrorMessage(e))
        }
    }

    override val sectorCount = tech.sectorCount

    override fun readBlock(block: Int): ImmutableByteArray {
        try {
            return tech.readBlock(block).toImmutable()
        } catch (e: TagLostException) {
            throw CardLostException(Utils.getErrorMessage(e))
        } catch (e: IOException) {
            throw CardTransceiveException(e, Utils.getErrorMessage(e))
        }
    }
}