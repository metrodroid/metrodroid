/*
 * DesfireCardReader.kt
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings
import au.id.micolous.metrodroid.card.desfire.settings.StandardDesfireFileSettings
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

object DesfireCardReader {
    private const val TAG = "DesfireCardReader"

    /**
     * Dumps a DESFire tag in the field.
     * @param tech Tag to dump.
     * @return DesfireCard of the card contents. Returns null if an unsupported card is in the
     * field.
     * @throws Exception On communication errors.
     */
    suspend fun dumpTag(tech: CardTransceiver,
                        feedbackInterface: TagReaderFeedbackInterface): DesfireCard? {
        val apps = mutableMapOf<Int, DesfireApplication>()

        val manufData: ImmutableByteArray

        try {
            val desfireTag = DesfireProtocol(tech)

            try {
                manufData = desfireTag.getManufacturingData()
            } catch (e: IllegalArgumentException) {
                // Credit cards tend to fail at this point.
                Log.w(TAG, "Card responded with invalid response, may not be DESFire?", e)
                return null
            }

            feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfd_reading))
            feedbackInterface.updateProgressBar(0, 1)

            val appIds = desfireTag.getAppList()
            var maxProgress = appIds.size
            var progress = 0

            val f = DesfireCardTransitRegistry.allFactories.find { it.earlyCheck(appIds) }
            val i = f?.getCardInfo(appIds)
            if (i != null) {
                Log.d(TAG, "Early Card Info: ${i.name}")
                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type, i.name))
                feedbackInterface.showCardType(i)
            }

            // Uncomment this to test the card type display.
            //Thread.sleep(5000);

            for (appId in appIds) {
                feedbackInterface.updateProgressBar(progress, maxProgress)
                desfireTag.selectApp(appId)
                progress++

                val files = mutableMapOf<Int, RawDesfireFile>()

                val unlocker = f?.createUnlocker(appId, manufData)
                var fileIds = desfireTag.getFileList()
                if (unlocker != null) {
                    fileIds = unlocker.getOrder(desfireTag, fileIds)
                }
                maxProgress += fileIds.size * if (unlocker == null) 1 else 2
                val authLog = mutableListOf<DesfireAuthLog>()
                for (fileId in fileIds) {
                    feedbackInterface.updateProgressBar(progress, maxProgress)
                    if (unlocker != null) {
                        if (i != null) {
                            feedbackInterface.updateStatusText(
                                    Localizer.localizeString(R.string.mfd_unlocking, i.name))
                        }
                        unlocker.unlock(desfireTag, files, fileId, authLog)
                        feedbackInterface.updateProgressBar(++progress, maxProgress)
                    }

                    var settingsRaw: ImmutableByteArray? = null
                    try {
                        settingsRaw = desfireTag.getFileSettings(fileId)
                        val data: ImmutableByteArray
                        val settings = DesfireFileSettings.create(settingsRaw)
                        if (settings is StandardDesfireFileSettings) {
                            data = desfireTag.readFile(fileId)
                        } else if (settings is ValueDesfireFileSettings) {
                            data = desfireTag.getValue(fileId)
                        } else {
                            data = desfireTag.readRecord(fileId)
                        }
                        files[fileId] = RawDesfireFile(settingsRaw, data, null, false)
                    } catch (ex: UnauthorizedException) {
                        files[fileId] = RawDesfireFile(settingsRaw, null, ex.message, true)
                    } catch (ex: CardTransceiveException) {
                        throw ex
                    } catch (ex: Exception) {
                        files[fileId] = RawDesfireFile(settingsRaw, null, ex.toString(), false)
                    }

                    progress++
                }

                apps[appId] = DesfireApplication(files, authLog)
            }
        } finally {
        }

        return DesfireCard(manufData, apps, isPartialRead = false)
    }
}
