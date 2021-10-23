/*
 * FelicaReader.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 *
 * Octopus reading code based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General private License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General private License for more details.
 *
 * You should have received a copy of the GNU General private License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString


object FelicaReader {
    private const val TAG = "FelicaReader"

    /**
     * Octopus (so probably also 1st generation SZT cards) have a special knocking sequence to
     * allow unprotected reads, and does not respond to the normal system code listing.
     *
     * These a bit like FeliCa Lite -- they have one system and one service.
     */
    private val DEEP_SYSTEM_CODES = intArrayOf(
            OctopusTransitData.SYSTEMCODE_OCTOPUS,
            OctopusTransitData.SYSTEMCODE_SZT)

    /**
     * Felica has well-known system IDs.  If those system IDs are sufficient to detect
     * a particular type of card (or at least have a really good guess at it), then we should send
     * back a CardInfo.
     *
     * If we have no idea, then send back "null".
     *
     * Each of these checks should be really cheap to run, because this blocks further card
     * reads.
     * @param systemCodes The system codes that exist on the card.
     * @return A CardInfo about the card, or null if we have no idea.
     */
    private fun parseEarlyCardInfo(systemCodes: List<Int>): CardInfo? {
        for (f in FelicaRegistry.allFactories) {
            if (f.earlyCheck(systemCodes))
                return f.getCardInfo(systemCodes)
        }
        return null
    }

    // https://github.com/tmurakam/felicalib/blob/master/src/dump/dump.c
    // https://github.com/tmurakam/felica2money/blob/master/src/card/Suica.cs
    /**
     * Dumps a FeliCa (JIS X 6319-4) tag.
     *
     * Reference: https://github.com/metrodroid/metrodroid/wiki/FeliCa
     *
     * @param tag [FelicaTransceiver] to communicate with the card on
     * @param feedbackInterface [TagReaderFeedbackInterface] to communicate with the UI on
     * @param onlyFirst If `true`, only read the first system code on the card. If not set
     * (`false`), read all system codes. Setting this to `true` will result in an incomplete
     * read, but is needed to work around a bug in iOS.
     */
    fun dumpTag(tag: FelicaTransceiver,
                        feedbackInterface: TagReaderFeedbackInterface,
                        onlyFirst: Boolean = false):
            FelicaCard {
        var magic = false
        var liteMagic = false
        var partialRead = false

        val fp = FelicaProtocol(tag, tag.uid!!)

        Log.d(TAG, "Default system code: ${fp.defaultSystemCode.hexString}")
        val pmm = fp.pmm

        val systems = mutableMapOf<Int, FelicaSystem>()
        var actionsDone = 0
        var totalActions = 1
        feedbackInterface.updateProgressBar(actionsDone, totalActions)
        var specificationVersion: ImmutableByteArray? = null

        try {
            var codes = fp.getSystemCodeList().toMutableList()

            // Check if we failed to get a System Code
            if (codes.isEmpty()) {
                // Lite has no system code list
                if (fp.pollFelicaLite()) {
                    Log.d(TAG, "Detected Felica Lite card")
                    codes.add(FelicaConsts.SYSTEMCODE_FELICA_LITE)
                    liteMagic = true
                } else {
                    // Don't do these on lite as it may respond to any code
                    Log.d(TAG, "Polling for DEEP_SYSTEM_CODES...")
                    codes = fp.pollForSystemCodes(DEEP_SYSTEM_CODES).toMutableList()

                    if (codes.isNotEmpty()) {
                        Log.d(TAG, "Got a DEEP_SYSTEM_CODE!")
                        magic = true
                    } else {
                        Log.w(TAG, "Card does not respond to getSystemCodeList, and no " +
                                "DEEP_SYSTEM_CODE found! Reading will fail.")
                    }
                }
            }

            actionsDone += 1
            totalActions += codes.size
            feedbackInterface.updateProgressBar(actionsDone, totalActions)

            val i = parseEarlyCardInfo(codes.map { it })
            if (i != null) {
                Log.d(TAG, "Early Card Info: ${i.name}")
                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type, i.name))
                feedbackInterface.showCardType(i)
            }

            // Some cards don't support this, we just swallow the exception and move on.
            specificationVersion = fp.requestSpecificationVersion(0)

            for ((systemNumber, systemCode) in codes.withIndex()) {
                Log.d(TAG, "System code #$systemNumber: ${systemCode.hexString}")

                // We can get System Code 0 from DEEP_SYSTEM_CODES -- drop this.
                if (systemCode == 0) continue

                if (onlyFirst && systemNumber > 0) {
                    // We aren't going to read secondary system codes. Instead, insert a dummy
                    // service with no service codes.
                    Log.i(TAG, "Not reading system code ${systemCode.hexString}: " +
                        "onlyFirst = true and systemNumber ($systemNumber) > 0")

                    systems[systemCode] = FelicaSystem(skipped = true)
                    continue
                }

                val services = mutableMapOf<Int, FelicaService>()

                var serviceCodes = when {
                    magic && systemCode == OctopusTransitData.SYSTEMCODE_OCTOPUS -> {
                        Log.d(TAG, "Stuffing in Octopus magic service code")
                        intArrayOf(OctopusTransitData.SERVICE_OCTOPUS)
                    }
                    magic && systemCode == OctopusTransitData.SYSTEMCODE_SZT -> {
                        Log.d(TAG, "Stuffing in SZT magic service code")
                        intArrayOf(OctopusTransitData.SERVICE_SZT)
                    }
                    liteMagic && systemCode == FelicaConsts.SYSTEMCODE_FELICA_LITE -> {
                        Log.d(TAG, "Stuffing in Felica Lite magic service code")
                        intArrayOf(FelicaConsts.SERVICE_FELICA_LITE_READONLY)
                    }
                    else -> null
                }

                if (serviceCodes == null) {
                    Log.d(TAG, "- Requesting service codes for ${systemCode.hexString}...")
                    serviceCodes = fp.getServiceCodeList(systemNumber)
                } else {
                    // Using magic!
                    Log.d(TAG, "- Polling for system code ${systemCode.hexString}...")
                    fp.pollForSystemCode(systemCode)
                }

                val excludedCodes = serviceCodes.filter { it and 0x01 == 0 }
                if (excludedCodes.isNotEmpty()) {
                    Log.d(TAG, "- Excluding ${excludedCodes.size} codes in system " +
                            "${systemCode.hexString} which require authentication: " +
                            excludedCodes.joinToString(limit = 50, transform = Int::hexString))

                    for (serviceCode in excludedCodes) {
                        services[serviceCode] = FelicaService(skipped = true)
                    }

                    serviceCodes = serviceCodes.filter { it and 0x01 == 1 }.toIntArray()
                }

                actionsDone += 1
                totalActions += serviceCodes.size
                feedbackInterface.updateProgressBar(actionsDone, totalActions)

                for (serviceCode in serviceCodes) {
                    val expectedBlocks = FelicaUtils.getBlockSize(systemCode, serviceCode)
                    Log.d(TAG, "- Fetching service code ${serviceCode.hexString}, expecting " +
                        "$expectedBlocks block(s)")
                    val blocks = mutableListOf<FelicaBlock>()
                    var addr = 0

                    try {
                        if (expectedBlocks != null) {
                            // We know what the size will be, fetch multiple blocks at once.
                            val addrs = (0 until expectedBlocks).asSequence()
                            val chunkSize = if (
                                liteMagic || systemCode == FelicaConsts.SYSTEMCODE_FELICA_LITE) {
                                4
                            } else {
                                8
                            }
                            for (c in addrs.chunked(chunkSize)) {
                                val result = fp.readWithoutEncryption(
                                    systemNumber, serviceCode, c.asSequence())
                                if (result == null) {
                                    Log.w(TAG, "Error reading blocks: $c")
                                    break
                                }

                                result.forEach {
                                    if (it.key >= addr) {
                                        addr = it.key + 1
                                    }
                                    blocks.add(FelicaBlock(it.value))
                                }
                            }
                        }

                      if (expectedBlocks == null || addr < expectedBlocks) {
                            Log.d(TAG, "Incorrect or unknown block size, using fallback mode")
                            var result = fp.readWithoutEncryption(systemNumber, serviceCode, addr)
                            while (result != null) {
                                blocks += FelicaBlock(result)
                                addr++
                                if (addr >= 0x20 && liteMagic)
                                    break
                                result = fp.readWithoutEncryption(systemNumber, serviceCode, addr)
                            }
                        }
                    } catch (tl: CardLostException) {
                        partialRead = true
                    }
                    actionsDone += 1
                    feedbackInterface.updateProgressBar(actionsDone, totalActions)

                    Log.d(TAG, "- Service code ${serviceCode.hexString} has ${blocks.size} blocks")

                    services[serviceCode] = FelicaService(blocks)

                    if (partialRead)
                        break
                }

                systems[systemCode] = FelicaSystem(services)
                if (partialRead)
                    break
            }
        } catch (e: CardLostException) {
            Log.w(TAG, "Tag was lost! Returning a partial read.")
            partialRead = true
        }

        if (systems.isEmpty()) {
            Log.w(TAG, "Could not detect any systems on the card!")
        }

        return FelicaCard(pmm, systems, specificationVersion, isPartialRead = partialRead)
    }
}
