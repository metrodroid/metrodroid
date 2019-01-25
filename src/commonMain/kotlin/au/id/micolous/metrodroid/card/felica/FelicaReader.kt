/*
 * FelicaReader.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

object FelicaReader {
    private const val TAG = "FelicaReader"

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
    suspend fun dumpTag(tag: CardTransceiver, feedbackInterface: TagReaderFeedbackInterface): FelicaCard {
        var octopusMagic = false
        var sztMagic = false
        var liteMagic = false
        var partialRead = false

        val ft = FeliCaTag(tag)

        var idm: ImmutableByteArray? = null

        try {
            idm = ft.pollingAndGetIDm(FelicaConsts.SYSTEMCODE_ANY)
        } catch (e: CardLostException) {
            Log.w(TAG, "Failed to get system code! can't return partial response.")
        }

        if (idm == null) {
            throw Exception("Failed to read IDm")
        }

        val pmm = ft.pMm

        val systems = mutableMapOf<Int, FelicaSystem>()

        try {
            // FIXME: Enumerate "areas" inside of systems ???
            val codes = ft.getSystemCodeList().toMutableList()

            // Check if we failed to get a System Code
            if (codes.isEmpty()) {
                // Lite has no system code list
                val liteSystem = ft.pollingAndGetIDm(FelicaConsts.SYSTEMCODE_FELICA_LITE)
                if (liteSystem != null) {
                    Log.d(TAG, "Detected Felica Lite card")
                    codes.add(FeliCaLib.SystemCode(FelicaConsts.SYSTEMCODE_FELICA_LITE))
                    liteMagic = true
                }

                // Lets try to ping for an Octopus anyway
                // Don't do it on lite as it may respond to any code
                val octopusSystem = if (liteMagic) null else ft.pollingAndGetIDm(OctopusTransitData.SYSTEMCODE_OCTOPUS)
                if (octopusSystem != null) {
                    Log.d(TAG, "Detected Octopus card")
                    // Octopus has a special knocking sequence to allow unprotected reads, and does not
                    // respond to the normal system code listing.
                    codes.add(FeliCaLib.SystemCode(OctopusTransitData.SYSTEMCODE_OCTOPUS))
                    octopusMagic = true
                }

                val sztSystem = if (liteMagic) null else ft.pollingAndGetIDm(OctopusTransitData.SYSTEMCODE_SZT)
                if (sztSystem != null) {
                    Log.d(TAG, "Detected Shenzhen Tong card")
                    // Because Octopus and SZT are similar systems, use the same knocking sequence in
                    // case they have the same bugs with system code listing.
                    codes.add(FeliCaLib.SystemCode(OctopusTransitData.SYSTEMCODE_SZT))
                    sztMagic = true
                }
            }

            val i = parseEarlyCardInfo(codes.map { it.code })
            if (i != null) {
                Log.d(TAG, "Early Card Info: %${i.name}")
                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type, i.name))
                feedbackInterface.showCardType(i)
            }

            for (code in codes) {
                Log.d(TAG, "Got system code: ${code.bytes}")

                val systemCode = code.code
                //ft.polling(systemCode);

                val thisIdm = ft.pollingAndGetIDm(systemCode)

                Log.d(TAG, " - Got IDm: $thisIdm  compare: $idm")
                Log.d(TAG, " - Got PMm: ${ft.pMm}  compare: $pmm")

                val services = mutableMapOf<Int, FelicaService>()
                val serviceCodes: List<FeliCaLib.ServiceCode>

                if (octopusMagic && code.code == OctopusTransitData.SYSTEMCODE_OCTOPUS) {
                    Log.d(TAG, "Stuffing in Octopus magic service code")
                    serviceCodes = listOf(FeliCaLib.ServiceCode(OctopusTransitData.SERVICE_OCTOPUS))
                } else if (sztMagic && code.code == OctopusTransitData.SYSTEMCODE_SZT) {
                    Log.d(TAG, "Stuffing in SZT magic service code")
                    serviceCodes = listOf(FeliCaLib.ServiceCode(OctopusTransitData.SERVICE_SZT))
                } else if (liteMagic && code.code == FelicaConsts.SYSTEMCODE_FELICA_LITE) {
                    Log.d(TAG, "Stuffing in Felica Lite magic service code")
                    serviceCodes = listOf(FeliCaLib.ServiceCode(FelicaConsts.SERVICE_FELICA_LITE_READONLY))
                } else {
                    serviceCodes = ft.getServiceCodeList()
                }

                // Brute Forcer (DEBUG ONLY)
                //if (octopusMagic)
                //for (int serviceCodeInt=0; serviceCodeInt<0xffff; serviceCodeInt++) {
                //    Log.d(TAG, "Trying to read from service code " + serviceCodeInt);
                //    FeliCaLib.ServiceCode serviceCode = new FeliCaLib.ServiceCode(serviceCodeInt);

                for (serviceCode in serviceCodes) {
                    val serviceCodeInt = serviceCode.bytes.byteArrayToIntReversed()

                    val blocks = mutableListOf<FelicaBlock>()

                    ft.polling(systemCode)

                    try {
                        var addr: Byte = 0
                        var result: FeliCaLib.ReadResponse? = ft.readWithoutEncryption(serviceCode, addr)
                        while (result != null && result.statusFlag1 == 0) {
                            blocks += FelicaBlock(result.blockData!!)
                            addr++
                            if (addr >= 0x20 && liteMagic)
                                break
                            result = ft.readWithoutEncryption(serviceCode, addr)
                        }
                    } catch (tl: CardLostException) {
                        partialRead = true
                    }

                    if (!blocks.isEmpty()) { // Most service codes appear to be empty...
                        services[serviceCodeInt] = FelicaService(blocks)

                        Log.d(TAG, "- Service code " + serviceCodeInt + " had " + blocks.size + " blocks")
                    }
                    if (partialRead)
                        break
                }

                systems[code.code] = FelicaSystem(services)
                if (partialRead)
                    break
            }

        } catch (e: CardLostException) {
            Log.w(TAG, "Tag was lost! Returning a partial read.")
            partialRead = true
        }

        return FelicaCard(idm, pmm!!, systems, isPartialRead = partialRead)
    }
}
