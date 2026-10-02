package au.id.micolous.metrodroid.transit.ncmc

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ncmc.NcmcApplication
import au.id.micolous.metrodroid.card.ncmc.NcmcCardTransitFactory
import au.id.micolous.metrodroid.card.ncmc.NcmcParser
import au.id.micolous.metrodroid.card.ncmc.NcmcPendingEntry
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader
import kotlinx.serialization.Serializable

fun TransitCurrency.Companion.inr(paise: Int): TransitCurrency =
    TransitCurrency(paise, "INR")

enum class NcmcTransactionStatus(val code: Int) {
    EXIT(0),
    ENTRY(1),
    PENALTY(2),
    TICKET(3),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == (code and 0x0F) } ?: UNKNOWN
    }
}

object NcmcDisplay {
    const val TABLE_ID = "ncmc"

    fun eventType(statusCode: Int): String = when (statusCode and 0x0F) {
        0 -> "Exit"
        1 -> "Entry"
        2 -> "Penalty"
        3 -> "Ticket"
        else -> "RFU"
    }

    /**
     * Combines Acquirer ID (1 byte) and Operator ID (2 bytes) into a 24-bit operator integer.
     */
    fun makeOperatorId(acquirerId: Int, operatorId: Int): Int {
        return ((acquirerId and 0xFF) shl 16) or (operatorId and 0xFFFF)
    }

    /**
     * Resolves Agency/Operator name from ncmc.mdst via StationTableReader.
     */
    fun resolveAgencyName(acquirerId: Int, operatorId: Int): FormattedString {
        val opId = makeOperatorId(acquirerId, operatorId)
        return StationTableReader.getOperatorName(TABLE_ID, opId, isShort = false)
            ?: FormattedString("Operator 0x${operatorId.toString(16).uppercase()}")
    }

    /**
     * Resolves Transport Mode (METRO/BUS/RAIL/etc.) from ncmc.mdst via StationTableReader.
     */
    fun resolveMode(acquirerId: Int, operatorId: Int): Trip.Mode {
        val opId = makeOperatorId(acquirerId, operatorId)
        return StationTableReader.getOperatorDefaultMode(TABLE_ID, opId)
    }

    /**
     * MdST cannot be used for NCMC as each unique station as 48bit ID (Operator [24b] + Station [24b])
     * Mapping it to 32-bit is not scalable for a large system like NCMC
     */
    fun resolveStation(terminalId: Int, statusCode: Int): Station {
        val statusLabel = eventType(statusCode)
        val fallbackName = "0x${terminalId.toString(16).uppercase().padStart(6, '0')} ($statusLabel)"
        return Station.nameOnly(fallbackName)
    }
}

@Parcelize
@Serializable
data class NcmcEntry(
    val acquirerId: Int,
    val operatorId: Int,
    val terminalId: Int,
    val minutesElapsed: Int
) : Parcelable

@Parcelize
@Serializable
data class NcmcTransaction(
    val acquirerId: Int,
    val operatorId: Int,
    val terminalId: Int,
    val minutesElapsed: Int,
    val transactionSequence: Int,
    val amountUnits: Int,
    val balanceUnits: Int,
    val statusCode: Int,
    val rfu: Int,
    val entry: NcmcEntry? = null
) : Parcelable {
    val amountPaise: Int get() = amountUnits * 10
    val balancePaise: Int get() = balanceUnits * 10
    val status: NcmcTransactionStatus get() = NcmcTransactionStatus.fromCode(statusCode)
}

@Parcelize
class NcmcEntryTrip(
    private val entry: NcmcEntry,
    override val startTimestamp: TimestampFull?
) : Trip() {
    override val fare: TransitCurrency? get() = null
    override val mode: Mode get() = NcmcDisplay.resolveMode(entry.acquirerId, entry.operatorId)

    override fun getAgencyName(isShort: Boolean): FormattedString =
        NcmcDisplay.resolveAgencyName(entry.acquirerId, entry.operatorId)

    override val startStation: Station
        get() = NcmcDisplay.resolveStation(entry.terminalId, 1)

    override val machineID: String? get() = null

    override fun getRawFields(level: TransitData.RawLevel): String =
        "status=Entry, minutesElapsed=${entry.minutesElapsed}, terminal=0x${entry.terminalId.toString(16).uppercase().padStart(6, '0')}"
}

@Parcelize
class NcmcPendingEntryTrip(
    private val entry: NcmcPendingEntry,
    override val startTimestamp: TimestampFull?
) : Trip() {
    override val fare: TransitCurrency? get() = null
    override val mode: Mode get() = NcmcDisplay.resolveMode(entry.acquirerId, entry.operatorId)

    override fun getAgencyName(isShort: Boolean): FormattedString =
        NcmcDisplay.resolveAgencyName(entry.acquirerId, entry.operatorId)

    override val startStation: Station
        get() = NcmcDisplay.resolveStation(entry.terminalId, 1)

    override val machineID: String? get() = null

    override fun getRawFields(level: TransitData.RawLevel): String =
        "status=Pending entry, minutesElapsed=${entry.minutesElapsed}, terminal=0x${entry.terminalId.toString(16).uppercase().padStart(6, '0')}"
}

@Parcelize
class NcmcTrip(
    val transaction: NcmcTransaction,
    override val endTimestamp: TimestampFull?,
    override val startTimestamp: TimestampFull?
) : Trip() {
    override val fare: TransitCurrency? get() = TransitCurrency.inr(transaction.amountPaise)
    override val mode: Mode get() = NcmcDisplay.resolveMode(transaction.acquirerId, transaction.operatorId)

    override fun getAgencyName(isShort: Boolean): FormattedString =
        NcmcDisplay.resolveAgencyName(transaction.acquirerId, transaction.operatorId)

    override val startStation: Station?
        get() = if (transaction.entry != null) {
            NcmcDisplay.resolveStation(transaction.entry.terminalId, 1)
        } else {
            NcmcDisplay.resolveStation(transaction.terminalId, transaction.statusCode)
        }

    override val endStation: Station?
        get() = if (transaction.entry != null) {
            NcmcDisplay.resolveStation(transaction.terminalId, transaction.statusCode)
        } else {
            null
        }

    override fun getRawFields(level: TransitData.RawLevel): String =
        "status=${transaction.status}, amountPaise=${transaction.amountPaise}, balancePaise=${transaction.balancePaise}"
}

@Parcelize
class NcmcTransitData(
    val pan: String?,
    val balancePaise: Long?,
    val effectiveDate: String?,
    val transactions: List<NcmcTransaction>,
    val pendingEntry: NcmcPendingEntry?
) : TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_ncmc)

    override val serialNumber: String?
        get() = pan

    override val trips: List<Trip>
        get() {
            val tripList = mutableListOf<Trip>()
            pendingEntry?.let {
                tripList.add(NcmcPendingEntryTrip(it, NcmcParser.parseTimestamp(effectiveDate, it.minutesElapsed)))
            }
            for (tx in transactions) {
                val txTs = NcmcParser.parseTimestamp(effectiveDate, tx.minutesElapsed)
                val entryTs = tx.entry?.let { NcmcParser.parseTimestamp(effectiveDate, it.minutesElapsed) }

                val startTs: TimestampFull?
                val endTs: TimestampFull?

                if (tx.entry != null) {
                    if (tx.entry.minutesElapsed > tx.minutesElapsed) {
                        startTs = txTs
                        endTs = entryTs
                    } else {
                        startTs = entryTs
                        endTs = txTs
                    }
                } else {
                    startTs = txTs
                    endTs = null
                }

                tripList.add(NcmcTrip(tx, endTimestamp = endTs, startTimestamp = startTs))
            }
            return tripList
        }

    override val balance: TransitCurrency?
        get() = balancePaise?.let { TransitCurrency.inr(it.toInt()) }

    companion object {
        val CARD_INFO = CardInfo(
            name = R.string.card_name_ncmc,
            cardType = CardType.ISO7816,
            region = TransitRegion.INDIA,
            locationId = R.string.location_india,
            imageId = R.drawable.ncmc
        )
    }
}

object NCMCTransitFactory : NcmcCardTransitFactory {
    override fun check(app: NcmcApplication): Boolean = app.type == NcmcApplication.TYPE
    override fun parseTransitData(app: NcmcApplication): TransitData = app.parseTransitData()
    override fun parseTransitIdentity(app: NcmcApplication): TransitIdentity = app.parseTransitIdentity()
    override val allCards: List<CardInfo> get() = listOf(NcmcTransitData.CARD_INFO)
}