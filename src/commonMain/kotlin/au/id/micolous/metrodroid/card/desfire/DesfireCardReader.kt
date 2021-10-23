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

import au.id.micolous.metrodroid.card.*
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
     fun dumpTag(tech: CardTransceiver,
                        feedbackInterface: TagReaderFeedbackInterface): DesfireCard? {
        val apps = mutableMapOf<Int, DesfireApplication>()

        val manufData: ImmutableByteArray
        var appListLocked: Boolean

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

            var appIds: IntArray
            try {
                appIds = desfireTag.getAppList()
                appListLocked = false
            } catch (e: UnauthorizedException) {
                Log.d(TAG, "Application list locked, switching to scanning")
                appIds = DesfireCardTransitRegistry.allFactories.flatMap { it.hiddenAppIds.orEmpty() }.toIntArray()
                appListLocked = true
            }
            var maxProgress = appIds.size
            var progress = 0

            val f = if (appListLocked) null else DesfireCardTransitRegistry.allFactories.find { it.earlyCheck(appIds) }
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
                try {
                    desfireTag.selectApp(appId)
                } catch (e: NotFoundException) {
                    continue
                }
                progress++

                val files = mutableMapOf<Int, RawDesfireFile>()

                val unlocker = f?.createUnlocker(appId, manufData)
                var dirListLocked = false
                val fileIds = try {
                    desfireTag.getFileList()
                } catch (e: UnauthorizedException) {
                    Log.d(TAG, "File list locked, switching to scanning")
                    dirListLocked = true
                    IntArray(0x20) { it }
                }.let {
                    unlocker?.getOrder(desfireTag, it) ?: it
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
                        try {
                            settingsRaw = desfireTag.getFileSettings(fileId)
                        } catch (ex: UnauthorizedException) {
                            settingsRaw = null
                        }
                        if (settingsRaw == null) {
                            files[fileId] = tryAllCommands(desfireTag, fileId)
                        } else {
                            files[fileId] = when (DesfireFileSettings.create(settingsRaw)) {
                                is StandardDesfireFileSettings ->
                                    RawDesfireFile(settingsRaw, desfireTag.readFile(fileId), readCommand=DesfireProtocol.READ_DATA)
                                is ValueDesfireFileSettings ->
                                    RawDesfireFile(settingsRaw, desfireTag.getValue(fileId), readCommand=DesfireProtocol.GET_VALUE)
                                else -> RawDesfireFile(settingsRaw, desfireTag.readRecord(fileId), readCommand=DesfireProtocol.READ_RECORD)
                            }
                        }
                    } catch (e: NotFoundException) {
                        continue
                    } catch (ex: UnauthorizedException) {
                        files[fileId] = RawDesfireFile(settingsRaw, null, ex.message, true)
                    } catch (ex: CardTransceiveException) {
                        throw ex
                    } catch (ex: Exception) {
                        files[fileId] = RawDesfireFile(settingsRaw, null, ex.toString(), false)
                    }

                    progress++
                }

                apps[appId] = DesfireApplication(files = files, authLog = authLog, dirListLocked = dirListLocked)
            }
        } finally {
        }

        return DesfireCard(manufData, apps, isPartialRead = false, appListLocked = appListLocked)
    }

    private fun wrap(cmd: Byte, f: () -> ImmutableByteArray): RawDesfireFile? {
        try {
            return RawDesfireFile(null, f(), readCommand=cmd)
        } catch (e: DesfireProtocol.PermissionDeniedException) {
            return null
        } catch (ex: UnauthorizedException) {
            return RawDesfireFile(null, null, error=ex.message, isUnauthorized=true)
        }
    }

    fun tryAllCommands(desfireTag: DesfireProtocol, fileId: Int): RawDesfireFile {
        wrap (DesfireProtocol.READ_DATA) {
            desfireTag.readFile(fileId)
        }?.let { return it }
        wrap (DesfireProtocol.GET_VALUE) {
            desfireTag.getValue(fileId)
        }?.let { return it }
        wrap (DesfireProtocol.READ_RECORD) {
            desfireTag.readRecord(fileId)
        } ?.let { return it }
        return RawDesfireFile(null, null, error="No command worked")
    }
}
