package au.id.micolous.metrodroid.card.ncmc

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application
import au.id.micolous.metrodroid.card.iso7816.ISO7816ApplicationCapsule
import au.id.micolous.metrodroid.card.iso7816.ISO7816ApplicationFactory
import au.id.micolous.metrodroid.card.iso7816.ISO7816ApplicationMutableCapsule
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardTransitFactory
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.ncmc.NCMCTransitFactory
import au.id.micolous.metrodroid.transit.ncmc.NcmcTransaction
import au.id.micolous.metrodroid.transit.ncmc.NcmcTransitData
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface NcmcCardTransitFactory : CardTransitFactory<NcmcApplication>

object NCMCRegistry {
    val allFactories: List<CardTransitFactory<*>> = listOf(
        NCMCTransitFactory
    )
}

@Parcelize
@Serializable
data class NcmcPendingEntry(
    val acquirerId: Int,
    val operatorId: Int,
    val terminalId: Int,
    val minutesElapsed: Int
) : Parcelable

@Serializable
data class NcmcApplication(
    override val generic: ISO7816ApplicationCapsule,
    val pan: String? = null,
    val cardAtc: Long? = null,
    val balancePaise: Long? = null,
    val effectiveDate: String? = null,
    val transactions: List<NcmcTransaction> = emptyList(),
    val ncmcVersion: String? = null,
    val cardLanguage: String? = null,
    val expiryDate: String? = null,
    val pendingEntry: NcmcPendingEntry? = null
) : ISO7816Application() {

    override val type: String
        get() = TYPE

    override val manufacturingInfo: List<ListItemInterface>?
        get() = listOf(
            HeaderListItem(R.string.ncmc_card_info),
            ListItem(R.string.ncmc_pan, pan ?: "Unknown"),
            ListItem(R.string.ncmc_version, ncmcVersion ?: "Unknown"),
            ListItem(R.string.ncmc_language, cardLanguage ?: "Unknown"),
            ListItem(R.string.ncmc_effective_date, NcmcParser.formatNcmcDate(effectiveDate)),
            ListItem(R.string.expiry_date, NcmcParser.formatNcmcDate(expiryDate)),
            ListItem(R.string.ncmc_atc, cardAtc?.toString() ?: "Unknown")
        )

    fun parseTransitIdentity(): TransitIdentity =
        TransitIdentity(name = Localizer.localizeString(R.string.card_name_ncmc), serialNumber = pan)

    override fun parseTransitIdentity(card: ISO7816Card): TransitIdentity =
        parseTransitIdentity()

    fun parseTransitData(): TransitData =
        NcmcTransitData(
            pan = pan,
            balancePaise = balancePaise,
            effectiveDate = effectiveDate,
            transactions = transactions,
            pendingEntry = pendingEntry
        )

    override fun parseTransitData(card: ISO7816Card): TransitData =
        parseTransitData()

    companion object {
        const val TYPE = "ncmc"
        val FACTORY: ISO7816ApplicationFactory = NcmcFactory()
    }
}

class NcmcFactory : ISO7816ApplicationFactory {
    companion object {
        private const val TAG = "NcmcFactory"
        private val NCMC_AID = ImmutableByteArray.fromHex("A0000005241010")
    }

    override val applicationNames: Collection<ImmutableByteArray> get() = listOf(NCMC_AID)
    override val stopAfterFirstApp: Boolean get() = false
    override val fixedAppIds: Boolean get() = true
    override val typeMap: Map<String, KSerializer<out ISO7816Application>>
        get() = mapOf(NcmcApplication.TYPE to NcmcApplication.serializer())

    override fun dumpTag(
        protocol: ISO7816Protocol,
        capsule: ISO7816ApplicationMutableCapsule,
        feedbackInterface: TagReaderFeedbackInterface,
        presentAids: List<ImmutableByteArray?>
    ): List<ISO7816Application>? {

        val cardName = Localizer.localizeString(R.string.card_name_ncmc)
        feedbackInterface.updateStatusText(
            Localizer.localizeString(R.string.card_reading_type, cardName)
        )
        feedbackInterface.showCardType(NcmcTransitData.CARD_INFO)
        feedbackInterface.updateProgressBar(0, 35)

        val cardAtc = findAtc(protocol)
        feedbackInterface.updateProgressBar(1, 35)

        val gpoResponse = try {
            protocol.sendRequest(
                ISO7816Protocol.CLASS_80,
                0xA8.toByte(),
                0x00,
                0x00,
                0x00,
                NcmcParser.buildNcmcGpoData()
            )
        } catch (e: Exception) {
            Log.d(TAG, "NCMC GPO failed", e)
            null
        } ?: return emptyList()

        val df33 = ISO7816TLV.findBERTLV(gpoResponse, "DF33", false) ?: return null
        val balancePaise = NcmcParser.parseDf33Balance(df33)
        val ncmcMetadata = NcmcParser.parseNcmcMetadataFromDf33(df33)

        feedbackInterface.updateProgressBar(2, 35)
        capsule.dumpAllSfis(protocol, feedbackInterface, start = 2, total = 35)

        val pan = NcmcParser.findPanOptimized(protocol)
        val effectiveDate = NcmcParser.findDateOptimized(protocol, "5F25")
        val expiryDate = NcmcParser.findDateOptimized(protocol, "5F24")

        val df33Transactions = NcmcParser.parseHistory(df33, effectiveDate)
        val sfi16Result = NcmcParser.findSfi16Transactions(protocol, effectiveDate)

        val transactionMap = linkedMapOf<Triple<Int, Int, Int>, NcmcTransaction>()
        fun transactionKey(tx: NcmcTransaction) = Triple(tx.transactionSequence, tx.minutesElapsed, tx.terminalId)

        for (tx in df33Transactions) transactionMap[transactionKey(tx)] = tx
        for (tx in sfi16Result.transactions) transactionMap[transactionKey(tx)] = tx

        feedbackInterface.updateProgressBar(35, 35)

        return listOf(
            NcmcApplication(
                generic = capsule.freeze(),
                pan = pan,
                cardAtc = cardAtc,
                effectiveDate = effectiveDate,
                balancePaise = balancePaise,
                transactions = transactionMap.values.toList(),
                ncmcVersion = ncmcMetadata.version,
                cardLanguage = ncmcMetadata.cardLanguage,
                expiryDate = expiryDate,
                pendingEntry = sfi16Result.pendingEntry
            )
        )
    }

    private fun findAtc(protocol: ISO7816Protocol): Long? {
        val response = try {
            protocol.sendRequest(ISO7816Protocol.CLASS_80, 0xCA.toByte(), 0x9F.toByte(), 0x36.toByte(), 0)
        } catch (_: Exception) { return null }

        if (response.size < 5 || (response[0].toInt() and 0xFF != 0x9F) || (response[1].toInt() and 0xFF != 0x36)) return null
        return (((response[3].toInt() and 0xFF) shl 8) or (response[4].toInt() and 0xFF)).toLong()
    }
}