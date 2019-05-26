package au.id.micolous.metrodroid.transit.emv

import au.id.micolous.metrodroid.card.emv.EmvCardMain
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.card.iso7816.TagDesc.Companion.TagContents.DUMP_LONG
import au.id.micolous.metrodroid.card.iso7816.TagDesc.Companion.TagContents.HIDE
import au.id.micolous.metrodroid.card.iso7816.TagDesc.Companion.interpretTag
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.emv.EmvData.LOG_ENTRY
import au.id.micolous.metrodroid.transit.emv.EmvData.T2Data
import au.id.micolous.metrodroid.transit.emv.EmvData.TAGMAP
import au.id.micolous.metrodroid.transit.emv.EmvLogEntry.Companion.parseEmvTrip
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray


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
    val allTlv = card.getAllTlv()
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
    val allTlv = card.getAllTlv()
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


private fun interpretTagInfo(id: ImmutableByteArray, data: ImmutableByteArray): ListItem? {
    val idStr = id.toHexString()
    val (name, contents) = TAGMAP[idStr] ?: return ListItem(FormattedString(idStr), data.toHexDump())
    return when (contents) {
        HIDE -> null
        DUMP_LONG -> ListItem(FormattedString(name), data.toHexDump())
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
        val unknownIds = mutableSetOf<String>()
        for (tlv in tlvs) {
            val (li, unknowns) = ISO7816TLV.infoBerTLVWithUnknowns(tlv, TAGMAP)

            res += li
            unknownIds += unknowns
        }

        res += ListItem("Unparsed IDs", unknownIds.joinToString(", "))
        return res
    }

    override val trips get() = logEntries

    override val cardName get() = name
}
