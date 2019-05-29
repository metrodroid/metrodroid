/*
 * EmvCard.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.card.emv

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.card.iso7816.ISO7816Data.TAG_DISCRETIONARY_DATA
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.transit.emv.EmvData.TAG_PDOL
import au.id.micolous.metrodroid.transit.emv.parseEmvTransitData
import au.id.micolous.metrodroid.transit.emv.parseEmvTransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

private fun getMainAppName(appProprietaryBerTlv: ImmutableByteArray?): ImmutableByteArray? {
    val l2 = ISO7816TLV.findBERTLV(appProprietaryBerTlv ?: return null,
            TAG_DISCRETIONARY_DATA, true) ?: return null
    val l3 = ISO7816TLV.findBERTLV(l2, "61", true) ?: return null
    return ISO7816TLV.findBERTLV(l3, "4f", false)
}

private const val LOG_FORMAT = 0x9f4f
private const val PIN_RETRY = 0x9f17
private const val TYPE_MAIN = "emv-main"
private const val TYPE_ANCHOR = "emv-anchor"

@Serializable
class EmvCard(override val generic: ISO7816ApplicationCapsule) : ISO7816Application() {
    @Transient
    override val type: String
        get() = TYPE_ANCHOR
}

@Serializable
class EmvCardMain internal constructor(
        override val generic: ISO7816ApplicationCapsule,
        private val dataResponse: Map<String, ImmutableByteArray>,
        val gpoResponse: ImmutableByteArray?) : ISO7816Application() {
    @Transient
    override val type: String
        get() = TYPE_MAIN

    @Transient
    val logFormat: ImmutableByteArray?
        get() = dataResponse[LOG_FORMAT.toString(16)]
    @Transient
    val pinTriesRemaining: ImmutableByteArray?
        get() = dataResponse[PIN_RETRY.toString(16)]

    override fun parseTransitData(card: ISO7816Card) = parseEmvTransitData(this)

    override fun parseTransitIdentity(card: ISO7816Card) = parseEmvTransitIdentity(this)

    @Transient
    override val rawData get() = super.rawData.orEmpty() +
            ListItem("GPO-response", "${gpoResponse?.toHexString()}") +
            (dataResponse.map { (k, v) -> ListItem("Data $k", v.toHexString()) })


    fun getAllTlv(): List<ImmutableByteArray> {
        val res = mutableListOf<ImmutableByteArray>()
        val a5 = appProprietaryBerTlv
        if (a5 != null) {
            res += listOfNotNull(
                    gpoResponse,
                    a5,
                    ISO7816TLV.findBERTLV(a5, TAG_DISCRETIONARY_DATA, true))
        }

        // SFI's
        res += (1..10).flatMap { file ->
            getSfiFile(file)?.recordList
                    ?: emptyList()
        }
        return res.filter { it.isNotEmpty() }
    }
}

class EmvFactory : ISO7816ApplicationFactory {
    override val typeMap: Map<String, KSerializer<out ISO7816Application>>
        get() = mapOf(TYPE_ANCHOR to EmvCard.serializer(), TYPE_MAIN to EmvCardMain.serializer())
    override val applicationNames get() = listOf(
            ImmutableByteArray.fromASCII("1PAY.SYS.DDF01"),
            ImmutableByteArray.fromASCII("2PAY.SYS.DDF01")
    )

    override suspend fun dumpTag(protocol: ISO7816Protocol,
                                 capsule: ISO7816ApplicationMutableCapsule,
                                 feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application>? {
        feedbackInterface.updateStatusText("Reading EMV")
        feedbackInterface.updateProgressBar(0, 32)
        capsule.dumpAllSfis(protocol, feedbackInterface, 0, 64)

        val fci = capsule.appFci

        Log.d("EMV", "EMV: FCI=$fci")

        val mainAppName = getMainAppName(capsule.appProprietaryBerTlv)

        if (mainAppName != null) {
            Log.d("EMV", "EMV: MAN=$mainAppName")

            val mainAppFci = protocol.selectByNameOrNull(mainAppName)
            val mainAppCapsule = ISO7816ApplicationMutableCapsule(appFci = mainAppFci, appName = mainAppName)
            mainAppCapsule.dumpAllSfis(protocol, feedbackInterface, 32, 64)
            val gpoResponse = readGpo(protocol, mainAppFci)

            val dr = mutableMapOf<String, ImmutableByteArray>()
            for (p1p2 in listOf(0x9f13, PIN_RETRY, 0x9f36, LOG_FORMAT)) {
                val r = readData(protocol, p1p2)
                if (r != null)
                    dr[p1p2.toString(16)] = r
            }

            return listOf(EmvCard(generic = capsule.freeze()),
                    EmvCardMain(generic = mainAppCapsule.freeze(),
                    dataResponse = dr,
                    gpoResponse = gpoResponse))
        }

        return listOf(EmvCard(generic = capsule.freeze()))
    }

    private suspend fun readData(tag: ISO7816Protocol, p1p2: Int) = try {
        tag.sendRequest(ISO7816Protocol.CLASS_80, 0xca.toByte(),
                (p1p2 shr 8).toByte(), p1p2.toByte(), 0)
    } catch (e: Exception) {
        null
    }

    private suspend fun readGpo(tag: ISO7816Protocol, mainAppData: ImmutableByteArray?): ImmutableByteArray? {
        if (mainAppData == null)
            return null
        Log.d("EMV", "AD = $mainAppData")

        val a5 = getProprietaryBerTlv(mainAppData) ?: return null

        val pdolTemplate = ISO7816TLV.findBERTLV(a5, TAG_PDOL, false) ?: return null

        Log.d("EMV", "PDOL = $pdolTemplate")

        var pdolFilled = ImmutableByteArray.empty()
        ISO7816TLV.pdolIterate(pdolTemplate) { id, len ->
            val contents = when (id.toHexString()) {
                // Currency: USD
                "5f2a" -> ImmutableByteArray.fromHex("0840")
                // Terminal Verification Results (TVR)
                "95" -> ImmutableByteArray(5)
                // Transaction Date: 1.1.2018
                "9a" -> ImmutableByteArray.fromHex("180101")
                // Transaction Type
                "9c" -> ImmutableByteArray(1)
                // Amount, Authorised: 1 cent
                "9f02" -> ImmutableByteArray.fromHex("000000000001")
                // Amount, Other: 0 cents
                "9f03" -> ImmutableByteArray(6)
                // Country: USA
                "9f1a" -> ImmutableByteArray.fromHex("0840")
                // "Unpredictable" number
                "9f37" -> ImmutableByteArray.fromHex("08130CE7")
                // Terminal Transaction Qualifiers
                "9f66" -> ImmutableByteArray.fromHex("F620C000")
                else -> ImmutableByteArray.empty()
            }
            val adjusted = when {
                contents.size > len -> contents.sliceArray((contents.size - len)..contents.size)
                contents.size < len -> ImmutableByteArray(len - contents.size) { 0 } + contents
                else -> contents
            }
            pdolFilled += adjusted
        }

        val gpoRequest = ImmutableByteArray.ofB(0x83, pdolFilled.size) + pdolFilled
        try {
            return tag.sendRequest(ISO7816Protocol.CLASS_80, 0xa8.toByte(),
                    0, 0, 0, gpoRequest)
        } catch (e: Exception) {
            return null
        }
    }
}

