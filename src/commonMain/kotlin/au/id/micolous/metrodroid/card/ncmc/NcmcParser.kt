package au.id.micolous.metrodroid.card.ncmc

import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.ncmc.NcmcEntry
import au.id.micolous.metrodroid.transit.ncmc.NcmcTransaction
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

object NcmcParser {

    private val TZ = MetroTimeZone.INDIA
    private const val DF33_HEADER_LENGTH = 32
    private const val CSA_LENGTH = 96
    private const val HISTORY_START = 21
    private const val HISTORY_ENTRY_LENGTH = 17
    private const val HISTORY_COUNT = 4

    data class NcmcMetadata(
        val pan: String? = null,
        val effectiveDate: String? = null,
        val expiryDate: String? = null,
        val version: String? = null,
        val cardLanguage: String? = null
    )

    data class Sfi16Result(
        val transactions: List<NcmcTransaction>,
        val pendingEntry: NcmcPendingEntry?
    )

    data class ValidationData(
        val acquirerId: Int,
        val operatorId: Int,
        val terminalId: Int,
        val minutesElapsed: Int,
        val fareUnits: Int,
        val routeNumber: Int,
        val serviceProviderData: Int,
        val statusCode: Int,
        val rfu: Int
    )

    fun formatNcmcDate(date: String?): String {
        if (date == null || date.length != 6) return "Unknown"
        return "${date.substring(4, 6)}/${date.substring(2, 4)}/20${date.substring(0, 2)}"
    }

    fun buildNcmcGpoData(): ImmutableByteArray {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        fun bcd(value: Int): String = value.toString().padStart(2, '0')

        val dateBcd = bcd(now.year % 100) + bcd(now.month.ordinal + 1) + bcd(now.dayOfMonth)
        val timeBcd = bcd(now.hour) + bcd(now.minute) + bcd(now.second)
        val randomHex = Random.nextBytes(4).joinToString("") { (it.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0') }

        return ImmutableByteArray.fromHex(
            "8325FF80F00001004000000000084800020743" + dateBcd + timeBcd + randomHex + "FF013131323834353833"
        )
    }

    fun extractPan(data: ImmutableByteArray): String? {
        ISO7816TLV.findBERTLV(data, "5A", false)?.let { return decodePan(it) }
        ISO7816TLV.findBERTLV(data, "57", false)?.let { return decodeTrack2(it) }
        return null
    }

    fun parseDf33Balance(df33Data: ImmutableByteArray): Long? {
        if (df33Data.size < 29) return null
        val balanceBcd = df33Data.toHexString().substring(46, 58)
        if (balanceBcd.any { it !in '0'..'9' }) return null
        return balanceBcd.toLongOrNull()
    }

    private fun decodePan(data: ImmutableByteArray): String =
        data.toHexString().uppercase().trimEnd('F').chunked(4).joinToString(" ")

    private fun decodeTrack2(data: ImmutableByteArray): String? {
        val digits = data.toHexString().uppercase()
        val separator = digits.indexOf('D')
        return if (separator >= 0) digits.substring(0, separator) else null
    }

    fun extractDate(record: ImmutableByteArray, tag: String): String? {
        val date = ISO7816TLV.findBERTLV(record, tag, false) ?: return null
        return if (date.size >= 3) date.toHexString().uppercase().take(6) else null
    }

    fun findPanOptimized(protocol: ISO7816Protocol): String? {
        for (sfi in listOf(1, 2, 16)) {
            for (recordNumber in 1..10) {
                val record = try { protocol.readRecord(sfi, recordNumber.toByte(), 0) } catch (_: Exception) { null } ?: continue
                val pan = extractPan(record)
                if (!pan.isNullOrBlank()) return pan
            }
        }
        return null
    }

    fun findDateOptimized(protocol: ISO7816Protocol, tag: String): String? {
        for (sfi in listOf(1, 2, 16)) {
            for (recordNumber in 1..10) {
                val record = try { protocol.readRecord(sfi, recordNumber.toByte(), 0) } catch (_: Exception) { null } ?: continue
                val date = extractDate(record, tag)
                if (date != null && isValidEffectiveDate(date)) return date
            }
        }
        return null
    }

    fun parseNcmcMetadataFromDf33(df33: ImmutableByteArray): NcmcMetadata {
        val csaOffset = 32
        if (df33.size < csaOffset + 2) return NcmcMetadata()

        val versionByte = df33[csaOffset].toInt() and 0xFF
        val major = (versionByte ushr 4) and 0x0F
        val minor = versionByte and 0x0F
        val version = if (major <= 9 && minor <= 9) "$major.$minor" else null

        val languageCode = (df33[csaOffset + 1].toInt() and 0xFF) ushr 3
        val language = decodeCardLanguage(languageCode)

        return NcmcMetadata(version = version, cardLanguage = language)
    }

    private fun decodeCardLanguage(code: Int): String? {
        val languages = listOf("English", "Hindi", "Bengali", "Marathi", "Telugu", "Tamil", "Gujarati", "Urdu", "Kannada", "Odia", "Malayalam", "Punjabi")
        return languages.getOrNull(code)
    }

    fun findSfi16Transactions(protocol: ISO7816Protocol, effectiveDate: String?): Sfi16Result {
        val allTransactions = mutableListOf<NcmcTransaction>()
        val validationData = mutableListOf<ValidationData>()

        for (recordNumber in 1..10) {
            val record = try { protocol.readRecord(16, recordNumber.toByte(), 0) } catch (_: Exception) { null } ?: continue
            if (record.size < CSA_LENGTH) continue

            val csa = ImmutableByteArray.fromHex(record.toHexString().takeLast(CSA_LENGTH * 2))
            val validation = parseValidationData(csa)
            if (validation != null) validationData += validation

            if (isPotentialTransactionLog(csa)) {
                allTransactions += parseCsaHistory(csa, effectiveDate)
            }
        }

        return attachEntryData(allTransactions, validationData)
    }

    private fun attachEntryData(transactions: List<NcmcTransaction>, validations: List<ValidationData>): Sfi16Result {
        val entries = validations.filter { it.statusCode == 1 }.sortedBy { it.minutesElapsed }.toMutableList()
        val exits = transactions.filter { it.statusCode == 0 }.sortedBy { it.minutesElapsed }
        val usedEntryIndexes = mutableSetOf<Int>()
        val transactionMap = transactions.associateBy { Triple(it.transactionSequence, it.minutesElapsed, it.terminalId) }.toMutableMap()

        for (exit in exits) {
            val matchingEntryIndex = entries.indices
                .filter { it !in usedEntryIndexes && entries[it].minutesElapsed <= exit.minutesElapsed }
                .maxByOrNull { entries[it].minutesElapsed } ?: continue

            val entry = entries[matchingEntryIndex]
            usedEntryIndexes += matchingEntryIndex
            val key = Triple(exit.transactionSequence, exit.minutesElapsed, exit.terminalId)

            transactionMap[key] = exit.copy(
                entry = NcmcEntry(entry.acquirerId, entry.operatorId, entry.terminalId, entry.minutesElapsed)
            )
        }

        val pendingEntry = entries.indices.firstOrNull { it !in usedEntryIndexes }?.let {
            val entry = entries[it]
            NcmcPendingEntry(entry.acquirerId, entry.operatorId, entry.terminalId, entry.minutesElapsed)
        }

        return Sfi16Result(transactionMap.values.toList(), pendingEntry)
    }

    fun parseValidationData(csa: ImmutableByteArray): ValidationData? {
        if (csa.size < HISTORY_START || (csa[0].toInt() and 0xFF) != 0x31) return null

        return ValidationData(
            acquirerId = csa[4].toInt() and 0xFF,
            operatorId = readUInt16(csa, 5),
            terminalId = readUInt24(csa, 7),
            minutesElapsed = readUInt24(csa, 10),
            fareUnits = readUInt16(csa, 13),
            routeNumber = readUInt16(csa, 15),
            serviceProviderData = readUInt24(csa, 17),
            statusCode = (csa[20].toInt() and 0xFF) ushr 4,
            rfu = (csa[20].toInt() and 0xFF) and 0x0F
        )
    }

    fun parseCsaHistory(csa: ImmutableByteArray, effectiveDate: String?): List<NcmcTransaction> {
        if (csa.size < CSA_LENGTH || !isValidEffectiveDate(effectiveDate)) return emptyList()
        val result = mutableListOf<NcmcTransaction>()
        for (i in 0 until HISTORY_COUNT) {
            val tx = parseHistoryEntry(csa, HISTORY_START + i * HISTORY_ENTRY_LENGTH)
            if (tx != null) result += tx
        }
        return result
    }

    fun parseHistory(df33: ImmutableByteArray, effectiveDate: String?): List<NcmcTransaction> {
        if (df33.size < DF33_HEADER_LENGTH + CSA_LENGTH || !isValidEffectiveDate(effectiveDate)) return emptyList()
        val result = mutableListOf<NcmcTransaction>()
        for (i in 0 until HISTORY_COUNT) {
            val tx = parseHistoryEntry(df33, DF33_HEADER_LENGTH + HISTORY_START + i * HISTORY_ENTRY_LENGTH)
            if (tx != null) result += tx
        }
        return result
    }

    private fun parseHistoryEntry(data: ImmutableByteArray, offset: Int): NcmcTransaction? {
        if (offset + HISTORY_ENTRY_LENGTH > data.size) return null

        val balanceStatus = readUInt24(data, offset + 13)
        return NcmcTransaction(
            acquirerId = data[offset].toInt() and 0xFF,
            operatorId = readUInt16(data, offset + 1),
            terminalId = readUInt24(data, offset + 3),
            minutesElapsed = readUInt24(data, offset + 6),
            transactionSequence = readUInt16(data, offset + 9),
            amountUnits = readUInt16(data, offset + 11),
            balanceUnits = balanceStatus ushr 4,
            statusCode = balanceStatus and 0x0F,
            rfu = data[offset + 16].toInt() and 0xFF
        )
    }

    fun parseTimestamp(effectiveDate: String?, minutesElapsed: Int): TimestampFull? {
        val date = parseEffectiveDate(effectiveDate) ?: return null
        return try {
            val daysToAdd = minutesElapsed / 1440
            val minuteOfDay = minutesElapsed % 1440
            val hh = minuteOfDay / 60
            val min = minuteOfDay % 60

            var curYear = date.first
            var curMonth = date.second
            var curDay = date.third + daysToAdd

            while (true) {
                val dim = daysInMonth(curYear, curMonth)
                if (curDay <= dim) break
                curDay -= dim
                curMonth++
                if (curMonth > 12) {
                    curMonth = 1
                    curYear++
                }
            }

            TimestampFull(
                TZ,
                curYear,
                curMonth - 1, // 0-indexed month for TimestampFull
                curDay,
                hh,
                min,
                0
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        if (month == 2) {
            return if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        }
        return when (month) {
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun parseEffectiveDate(date: String?): Triple<Int, Int, Int>? {
        if (date == null) return null
        val value = date.trim()
        if (value.length != 6 || !value.all { it.isDigit() }) return null
        val year = 2000 + (value.substring(0, 2).toIntOrNull() ?: return null)
        val month = value.substring(2, 4).toIntOrNull() ?: return null
        val day = value.substring(4, 6).toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return Triple(year, month, day)
    }

    private fun readUInt16(data: ImmutableByteArray, offset: Int) = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    private fun readUInt24(data: ImmutableByteArray, offset: Int) = ((data[offset].toInt() and 0xFF) shl 16) or ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset + 2].toInt() and 0xFF)

    fun isValidEffectiveDate(date: String?): Boolean = parseEffectiveDate(date) != null
    private fun isPotentialTransactionLog(csa: ImmutableByteArray) = csa.size >= CSA_LENGTH && (0 until csa.size).count { csa[it].toInt() != 0 } > 4
}