package au.id.micolous.metrodroid.card.classic

import android.content.Context
import android.nfc.TagLostException
import android.util.Log
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.key.CardKeysRetriever
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.util.Utils
import java.io.IOException
import java.util.*


object ClassicReader {
    private fun readSectorWithKey(tech: ClassicCardTech, sectorIndex: Int,
                                  correctKey: ClassicSectorKey): ClassicSector {
        val blocks = mutableListOf<ClassicBlock>()
        // FIXME: First read trailer block to get type of other blocks.
        val firstBlockIndex = tech.sectorToBlock(sectorIndex)
        for (blockIndex in 0 until tech.getBlockCountInSector(sectorIndex)) {
            var data = tech.readBlock(firstBlockIndex + blockIndex)
            val type = ClassicBlock.TYPE_DATA // FIXME

            // Sometimes the result is just a single byte 04
            // Reauthenticate if that happens
            repeat(3) {
                if (data.size == 1) {
                    tech.authenticate(sectorIndex, correctKey)
                    data = tech.readBlock(firstBlockIndex + blockIndex)
                }
            }

            blocks.add(ClassicBlock.create(type, blockIndex, data))
        }
        return ClassicSector(sectorIndex,
                blocks.toTypedArray(),
                correctKey)
    }

    private const val TAG = "ClassicReader"

    private fun earlyCheck(sectors: List<ClassicSector>, feedbackInterface: TagReaderFeedbackInterface): ClassicCardTransitFactory? {
        val secnum = sectors.size
        ClassicCard.allFactories.filter { factory -> factory.earlySectors() == secnum }
                .forEach lambda@{ factory ->
                    val ci = try {
                        if (!factory.earlyCheck(sectors))
                            return@lambda
                        factory.earlyCardInfo(sectors) ?: return@lambda
                    } catch (e: Exception) {
                        return@lambda
                    }

                    feedbackInterface.showCardType(ci)
                    feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type, ci.name))
                    return factory
                }
        return null
    }

    fun readCard(context: Context, retriever: CardKeysRetriever, tech: ClassicCardTech,
                 feedbackInterface: TagReaderFeedbackInterface): ClassicCard {
        val sectorCount = tech.sectorCount
        val sectors = mutableListOf<ClassicSector>()
        val maxProgress = sectorCount * 5
        var cardType: ClassicCardTransitFactory? = null
        val tagId = tech.tagId
        val auth = ClassicAuthenticator.makeAuthenticator(tagId = tagId, maxProgress = maxProgress,
                context = context, retriever = retriever)

        for (sectorIndex in 0 until sectorCount) {
            try {
                val correctKey = auth.authenticate(
                        tech, feedbackInterface,
                        sectorIndex, cardType?.isDynamicKeys(sectors, sectorIndex,
                        ClassicSectorKey.KeyType.UNKNOWN) ?: false,
                        ClassicSectorKey.KeyType.UNKNOWN)

                feedbackInterface.updateProgressBar(sectorIndex * 5 + 3, maxProgress)

                // Fallback if no key is found
                if (correctKey == null) {
                    sectors.add(UnauthorizedClassicSector(sectorIndex))
                    continue
                }

                feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfc_reading_blocks, sectorIndex))
                var sector = ClassicReader.readSectorWithKey(tech, sectorIndex, correctKey)

                // If we used keyA and it wasn't enough try finding B
                if (sector.key?.type == ClassicSectorKey.KeyType.A
                        && sector.blocks.any { it.isUnauthorized }) {
                    val correctKeyB = auth.authenticate(
                            tech, feedbackInterface,
                            sectorIndex, cardType?.isDynamicKeys(sectors, sectorIndex,
                            ClassicSectorKey.KeyType.B) ?: false,
                            ClassicSectorKey.KeyType.B)
                    if (correctKeyB != null)
                        sector = ClassicReader.readSectorWithKey(tech, sectorIndex, correctKey)
                }

                sectors.add(sector)

                if (cardType == null)
                    cardType = earlyCheck(sectors, feedbackInterface)

                feedbackInterface.updateProgressBar(sectorIndex * 5 + 4, maxProgress)
            } catch (ex: TagLostException) {
                Log.w(TAG, "tag lost!", ex)
                sectors.add(InvalidClassicSector(sectorIndex, Utils.getErrorMessage(ex)))
                return ClassicCard(tagId = tagId,
                        scannedAt = GregorianCalendar.getInstance(),
                        sectors = sectors, partialRead = true)
            } catch (ex: IOException) {
                sectors.add(InvalidClassicSector(sectorIndex, Utils.getErrorMessage(ex)))
            }
        }

        return ClassicCard(tagId = tagId,
                scannedAt = GregorianCalendar.getInstance(), sectors = sectors, partialRead = false)
    }
}