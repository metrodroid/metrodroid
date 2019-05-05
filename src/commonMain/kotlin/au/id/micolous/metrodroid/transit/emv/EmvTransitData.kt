package au.id.micolous.metrodroid.transit.emv

import au.id.micolous.metrodroid.card.emv.EmvCardMain
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.countryCodeToName
import au.id.micolous.metrodroid.util.currencyNameByCode

private const val T2Data = "57"
private const val LOG_ENTRY = "9f4d"

private enum class TagContents {
    DUMP_SHORT,
    DUMP_LONG,
    ASCII,
    DUMP_UNKNOWN,
    HIDE,
    CURRENCY,
    COUNTRY
}

private data class TagDesc(val name: String, val contents: TagContents)

private val TAGMAP = mapOf(
        "50" to TagDesc("Name 1", TagContents.ASCII),
        "56" to TagDesc("Track 1", TagContents.ASCII),
        T2Data to TagDesc("Track 2", TagContents.DUMP_SHORT),
        "5a" to TagDesc("PAN", TagContents.DUMP_SHORT), // TODO: group by 4
        "5f20" to TagDesc("Cardholder Name", TagContents.ASCII),
        "5f24" to TagDesc("Expiry", TagContents.DUMP_SHORT), // TODO: show as date
        "5f25" to TagDesc("Effective Date", TagContents.DUMP_SHORT), // TODO: show as date
        "5f28" to TagDesc("Issuer country", TagContents.COUNTRY),
        "5f2d" to TagDesc("Language preference", TagContents.ASCII), // TODO: show language
        "5f34" to TagDesc("PAN sequence number", TagContents.DUMP_SHORT), // TODO: show as int
        "82" to TagDesc("Application Interchange Profile", TagContents.DUMP_SHORT),
        "87" to TagDesc("Application Priority Indicator", TagContents.DUMP_SHORT), // TODO: show as int
        "8c" to TagDesc("CDOL1", TagContents.HIDE),
        "8d" to TagDesc("CDOL2", TagContents.HIDE),
        "8e" to TagDesc("CVM list", TagContents.HIDE),
        "8f" to TagDesc("Certification Authority Public Key Index", TagContents.DUMP_SHORT), // TODO: show as int
        "90" to TagDesc("Issuer Public Key Certificate", TagContents.DUMP_LONG),
        "92" to TagDesc("Issuer Public Key Remainder", TagContents.DUMP_LONG),
        "93" to TagDesc("Signed Static Application Data", TagContents.DUMP_LONG),
        "94" to TagDesc("Application File Locator", TagContents.DUMP_SHORT),
        "9f07" to TagDesc("Application Usage Control", TagContents.HIDE),
        "9f08" to TagDesc("Application Version Number", TagContents.HIDE),
        "9f0d" to TagDesc("Issuer Action Code - Default", TagContents.HIDE),
        "9f0e" to TagDesc("Issuer Action Code - Denial", TagContents.HIDE),
        "9f0f" to TagDesc("Issuer Action Code - Online", TagContents.HIDE),
        "9f10" to TagDesc("Issuer Application Data", TagContents.DUMP_LONG),
        "9f11" to TagDesc("Issuer Code Table Index", TagContents.DUMP_SHORT), // TODO: show as int
        "9f12" to TagDesc("Name 2", TagContents.ASCII),
        "9f1f" to TagDesc("Track 1 Discretionary Data", TagContents.ASCII),
        "9f26" to TagDesc("Application Cryptogram", TagContents.DUMP_LONG),
        "9f27" to TagDesc("Cryptogram Information Data", TagContents.DUMP_LONG),
        "9f32" to TagDesc("Issuer Public Key Exponent", TagContents.DUMP_LONG),
        "9f36" to TagDesc("Application Transaction Counter", TagContents.DUMP_SHORT), // TODO: show as int
        "9f38" to TagDesc("PDOL", TagContents.HIDE),
        "9f42" to TagDesc("Application currency", TagContents.CURRENCY),
        "9f44" to TagDesc("Application currency exponent", TagContents.DUMP_SHORT), // TODO: show currency
        "9f46" to TagDesc("ICC Public Key Certificate", TagContents.DUMP_LONG),
        "9f47" to TagDesc("ICC Public Key Exponent", TagContents.DUMP_LONG),
        "9f48" to TagDesc("ICC Public Key Remainder", TagContents.DUMP_LONG),
        "9f49" to TagDesc("DDOL", TagContents.HIDE),
        "9f4a" to TagDesc("Static Data Authentication Tag List", TagContents.HIDE),
        LOG_ENTRY to TagDesc("Log entry", TagContents.HIDE),
        "bf0c" to TagDesc("Subtag", TagContents.HIDE)
)

private fun getAllTlv(card: EmvCardMain): List<ImmutableByteArray> {
    val res = mutableListOf<ImmutableByteArray>()
    val fci = card.appFci
    if (fci != null) {
        val a5 = ISO7816TLV.findBERTLV(fci, "a5", true)
        res += listOfNotNull(card.gpoResponse)
        if (a5 != null)
            res += listOfNotNull(a5, ISO7816TLV.findBERTLV(a5, "bf0c", true))
    }

    // SFI's
    res += (1..10).flatMap { file ->
        card.getSfiFile(file)?.recordList
                ?: emptyList()
    }
    return res.filter { it.isNotEmpty() }
}

private fun findT2Data(tlvs: List<ImmutableByteArray>): ImmutableByteArray? {
    for (tlv in tlvs) {
        val t2 = ISO7816TLV.findBERTLV(tlv, T2Data, false)
        if (t2 != null)
            return t2
    }

    return null
}

private fun splitby4(input: String?): String? {
    if (input == null)
        return null
    val len = input.length
    val res = (0..len step 4).fold("") { prev, i -> prev + input.substring(i, minOf(i + 4, len)) + " " }
    return if (res.endsWith(' ')) res.substring(0, res.length - 1) else res
}

fun parseEmvTransitData(card: EmvCardMain): EmvTransitData {
    val allTlv = getAllTlv(card)
    val logEntry = getTag(allTlv, LOG_ENTRY)
    val logFormat = card.logFormat
    val logEntries = if (logEntry != null && logFormat != null) {
        val logSfi = logEntry[0]
        val logRecords = card.getSfiFile(logSfi.toInt())
        logRecords?.recordList?.mapNotNull { parseEmvTrip(it, logFormat) }
    } else
        null
    val pinTriesRemaining = card.pinTriesRemaining?.let {
        ISO7816TLV.removeTlvHeader(it).byteArrayToInt()
    }
    return EmvTransitData(
            tlvs = allTlv,
            pinTriesRemaining = pinTriesRemaining,
            logEntries = logEntries,
            t2 = findT2Data(allTlv),
            name = findName(allTlv))
}

@Parcelize
data class EmvLogEntry(private val values: Map<String, ImmutableByteArray>) : Trip() {
    override val startTimestamp get(): Timestamp? {
        val dateBin = values["9a"] ?: return null
        val timeBin = values["9f21"]
        if (timeBin != null)
            return TimestampFull(tz = MetroTimeZone.UNKNOWN,
                    year = 2000 + NumberUtils.convertBCDtoInteger(dateBin[0].toInt()),
                    month = NumberUtils.convertBCDtoInteger(dateBin[1].toInt()) - 1,
                    day = NumberUtils.convertBCDtoInteger(dateBin[2].toInt()),
                    hour = NumberUtils.convertBCDtoInteger(timeBin[0].toInt()),
                    min = NumberUtils.convertBCDtoInteger(timeBin[1].toInt()),
                    sec = NumberUtils.convertBCDtoInteger(timeBin[2].toInt()))
        return Daystamp(year = 2000 + NumberUtils.convertBCDtoInteger(dateBin[0].toInt()),
                month = NumberUtils.convertBCDtoInteger(dateBin[1].toInt()) - 1,
                day = NumberUtils.convertBCDtoInteger(dateBin[2].toInt()))
    }

    override val fare get(): TransitCurrency? {
        val amountBin = values["9f02"] ?: return null
        val amount = amountBin.fold(0L) { acc, b ->
            acc * 100 + NumberUtils.convertBCDtoInteger(b.toInt() and 0xff)
        }

        val codeBin = values["5f2a"] ?: return TransitCurrency.XXX(amount.toInt())
        val code = NumberUtils.convertBCDtoInteger(codeBin.byteArrayToInt())

        return TransitCurrency(amount.toInt(), code)
    }

    override val mode get() = Mode.POS

    override val routeName get() = values.entries.filter {
        it.key != "9f02" && it.key != "5f2a"
                && it.key != "9a" && it.key != "9f21"
    }.joinToString {
        val tag = TAGMAP[it.key]
        if (tag == null)
            it.key + "=" + it.value.toHexString()
        else
            tag.name + "=" + interpretTag(tag.contents, it.value)
    }
}

private fun parseEmvTrip(record: ImmutableByteArray, format: ImmutableByteArray): EmvLogEntry? {
    val values = mutableMapOf<String, ImmutableByteArray>()
    var p = 0
    val dol = ISO7816TLV.removeTlvHeader(format)
    ISO7816TLV.pdolIterate(dol) { id, len ->
        if (p + len <= record.size)
            values[id.toHexString()] = record.sliceArray(p..(p + len - 1))
        p += len
    }
    return EmvLogEntry(values = values)
}

private fun getTag(tlvs: List<ImmutableByteArray>, id: String): ImmutableByteArray? {
    for (tlv in tlvs) {
        return ISO7816TLV.findBERTLV(tlv, id, false) ?: continue
    }
    return null
}

private fun findName(tlvs: List<ImmutableByteArray>): String {
    for (tag in listOf("9f12", "50")) {
        val variant = getTag(tlvs, tag) ?: continue
        return variant.readASCII()
    }
    return "EMV"
}

fun parseEmvTransitIdentity(card: EmvCardMain): TransitIdentity {
    val allTlv = getAllTlv(card)
    return TransitIdentity(
            findName(allTlv),
            splitby4(getPan(findT2Data(allTlv))))
}

private fun getPan(t2: ImmutableByteArray?): String? {
    val t2s = t2?.toHexString() ?: return null
    return t2s.substringBefore('d', t2s)
}

private fun getPostPan(t2: ImmutableByteArray): String? {
    val t2s = t2.toHexString()
    return t2s.substringAfter('d')
}

private fun interpretTag(contents: TagContents, data: ImmutableByteArray) = when (contents) {
    TagContents.ASCII -> data.readASCII()
    TagContents.DUMP_SHORT -> data.toHexString()
    TagContents.DUMP_LONG -> data.toHexString()
    TagContents.CURRENCY -> currencyNameByCode(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
    TagContents.COUNTRY -> countryCodeToName(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
    else -> data.toHexString()
}

private fun interpretTagInfo(id: ImmutableByteArray, data: ImmutableByteArray): ListItem? {
    val idStr = id.toHexString()
    val (name, contents) = TAGMAP[idStr] ?: return ListItem(FormattedString(idStr), data.toHexDump())
    return when (contents) {
        TagContents.HIDE -> null
        TagContents.DUMP_LONG -> ListItem(FormattedString(name), data.toHexDump())
        else -> ListItem(name, interpretTag(contents, data))
    }
}

@Parcelize
data class EmvTransitData(private val tlvs: List<ImmutableByteArray>,
                          private val name: String,
                          private val pinTriesRemaining: Int?,
                          private val t2: ImmutableByteArray?,
                          private val logEntries: List<EmvLogEntry>?) : TransitData() {
    override val serialNumber get() = splitby4(getPan(t2))

    override val info get(): List<ListItem> {
        val res = mutableListOf<ListItem>()
        if (t2 != null) {
            val postPan = getPostPan(t2)
            res += ListItem("PAN", splitby4(getPan(t2)))
            if (postPan != null) {
                res += ListItem("Expiry", "${postPan.substring(2, 4)}/${postPan.substring(0, 2)}")
                val serviceCode = postPan.substring(4, 7)
                res += ListItem("Service code", serviceCode)
                res += ListItem("Discretionary data",
                        postPan.substring(7).let { it.substringBefore('f', it) }
                )
            }
        }
        if (pinTriesRemaining != null)
            res += ListItem("PIN tries remaining", pinTriesRemaining.toString())
        res += listOf(HeaderListItem("TLV tags"))
        val allIds = mutableSetOf<String>()
        for (tlv in tlvs) {
            ISO7816TLV.berTlvIterate(tlv) { id, _, data ->
                res += listOfNotNull(interpretTagInfo(id, data))
                allIds += id.toHexString()
            }
        }

        allIds -= TAGMAP.keys
        res += ListItem("Unparsed IDs", allIds.joinToString(", "))
        return res
    }

    override val trips get() = logEntries

    override val cardName get() = name
}
