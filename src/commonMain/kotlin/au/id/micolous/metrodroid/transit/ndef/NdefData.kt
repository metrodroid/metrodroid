package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaConsts
import au.id.micolous.metrodroid.card.felica.FelicaSystem
import au.id.micolous.metrodroid.card.nfcv.NFCVCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.sum

@Parcelize
data class NdefData(val entries: List<NdefEntry>) : TransitData() {
    override val serialNumber: String?
        get() = null

    override val cardName: String
        get() = NAME

    override val info: List<ListItemInterface>?
        get() = entries.flatMap { it.info }

    fun getEntryExtType(type: ImmutableByteArray): NdefExtType? =
        entries.filterIsInstance<NdefExtType>().firstOrNull { type == it.type }

    fun getEntryExtType(type: String): NdefExtType? =
        getEntryExtType(ImmutableByteArray.fromASCII(type))

    companion object {
        fun checkClassic(card: ClassicCard): Boolean =
            MifareClassicAccessDirectory.parse(card)
                ?.contains(MifareClassicAccessDirectory.NFC_AID) == true

        fun parseClassic(
            card: ClassicCard,
            aid: Int = MifareClassicAccessDirectory.NFC_AID
        ): NdefData? {
            val mad = MifareClassicAccessDirectory.parse(card) ?: return null
            val sectors = mad.getContiguous(aid)

            if (sectors.isEmpty())
                return null

            return parseTLVNDEF(sectors.map { card[it].allData }.sum())
        }

        fun checkUltralight(card: UltralightCard): Boolean {
            try {
                val cc = card.pages[3].data

                if (cc[0] != 0xe1.toByte())
                    return false
                if (cc[1].toInt() !in listOf(0x10, 0x11))
                    return false
                if (cc[3].toInt() and 0xf0 != 0)
                    return false
                return true
            } catch (e: UnauthorizedException) {
                return false
            }
        }

        private fun getLenFromCCNFCV(card: NFCVCard): Pair<Int, Int>? {
            try {
                var cc = card.pages[0].data

                if (cc[0].toInt() and 0xff !in listOf(0xe1, 0xe2))
                    return null
                if (cc[1].toInt() and 0xfc != 0x40)
                    return null
                if (cc[2].toInt() != 0)
                    return Pair(4, cc[2].toInt() and 0xff)
                if (cc.size < 8)
                    cc += card.pages[1].data
                return Pair(8, cc.byteArrayToInt(6, 2))
            } catch (e: UnauthorizedException) {
                return null
            }
        }

        fun checkNFCV(card: NFCVCard): Boolean =
            getLenFromCCNFCV(card) != null

        fun parseNFCV(card: NFCVCard): NdefData? {
            val l = getLenFromCCNFCV(card) ?: return null
            return parseTLVNDEF(card.readBytes(l.first, l.second))
        }

        private fun getFelicaSystem(card: FelicaCard): FelicaSystem? {
            val ndefService = card.getSystem(FelicaConsts.SYSTEMCODE_NDEF)
            if (ndefService != null)
                return ndefService
            val liteService = card.getSystem(FelicaConsts.SYSTEMCODE_FELICA_LITE) ?: return null
            val mc = liteService.getService(FelicaConsts.SERVICE_FELICA_LITE_READONLY)
                ?.getBlock(FelicaConsts.FELICA_LITE_BLOCK_MC) ?: return null
            if (mc.data[3] == 0x01.toByte())
                return liteService
            return null
        }

        fun checkFelica(card: FelicaCard): Boolean {
            val service = getFelicaSystem(card)?.getService(0xb) ?: return false
            val attributes = service.getBlock(0)?.data ?: return false
            if (attributes[0].toInt() !in listOf(0x10, 0x11))
                return false
            val checksum =
                attributes.sliceOffLen(0, 14).map<Byte, Int> { it.toInt() and 0xff }.sum()
            val storedChecksum = attributes.byteArrayToInt(14, 2)
            return checksum == storedChecksum
        }

        fun parseFelica(card: FelicaCard): NdefData? {
            val service = getFelicaSystem(card)?.getService(0xb) ?: return null
            val attributes = service.getBlock(0)?.data ?: return null
            val ln = attributes.byteArrayToInt(11, 3)
            if (ln == 0) {
                return NdefData(emptyList())
            }
            val allData = (1 .. (ln + 15) / 16).map {
                service.getBlock(it)?.data ?: ImmutableByteArray.empty() }.sum()
                .sliceOffLen(0, ln)
            return parseNDEF(allData)
        }

        fun parseUltralight(card: UltralightCard): NdefData? {
            val cc = card.pages[3].data
            val sz = (cc[2].toInt() and 0xff) shl 1
            val dt = card.readPages(4, sz)

            return parseTLVNDEF(dt)
        }

        private fun parseTLVNDEF(data: ImmutableByteArray): NdefData? {
            var res: NdefData? = null
            for ((t, v) in iterateTLV(data)) {
                if (t == 0x03) {
                    val parsed = parseNDEF(v) ?: continue
                    res = if (res == null) parsed else res + parsed
                }
            }

            return res
        }

        private fun iterateTLV(data: ImmutableByteArray): Sequence<Pair<Int, ImmutableByteArray>> =
            sequence {
                var ptr = 0
                while (ptr < data.size) {
                    val t = data[ptr++]
                    if (t == 0xfe.toByte())
                        break
                    if (t == 0.toByte())
                        continue
                    var l = data[ptr++].toInt() and 0xff
                    if (l == 0xff) {
                        l = data.byteArrayToInt(ptr, 2)
                        ptr += 2
                    }

                    val v = data.sliceOffLen(ptr, l)
                    ptr += l

                    yield(Pair(t.toInt(), v))
                }
            }

        private fun parseNDEF(data: ImmutableByteArray): NdefData? {
            var ptr = 0
            val entries = mutableListOf<NdefEntry>()

            while (ptr < data.size) {
                val (entry, sz, isLast) = parseEntry(data, ptr)

                if (entry == null)
                    break

                entries += entry
                ptr += sz

                if (isLast)
                    break
            }

            return NdefData(entries)
        }

        private fun parseEntry(data: ImmutableByteArray, ptrStart: Int):
                Triple<NdefEntry?, Int, Boolean> {
            var ptr = ptrStart
            val head = NdefHead.parse(data, ptr) ?: return Triple(null, 0, true)
            ptr += head.headLen
            val type = data.sliceOffLen(ptr, head.typeLen)
            ptr += head.typeLen
            val id = if (head.idLen != null) data.sliceOffLen(ptr, head.idLen) else null
            ptr += head.idLen ?: 0
            var payload = data.sliceOffLen(ptr, head.payloadLen)
            ptr += head.payloadLen
            var me = head.me

            if (head.cf) {
                while (true) {
                    val subHead = NdefHead.parse(data, ptr) ?: return Triple(null, 0, true)
                    ptr += head.headLen
                    payload += data.sliceOffLen(ptr, head.payloadLen)
                    ptr += head.payloadLen
                    me = subHead.me
                    if (!subHead.cf)
                        break
                }
            }

            return Triple(
                payloadToEntry(head.tnf, type, id, payload), ptr - ptrStart,
                me
            )
        }

        private fun payloadToEntry(
            tnf: Int,
            type: ImmutableByteArray,
            id: ImmutableByteArray?,
            payload: ImmutableByteArray
        ): NdefEntry? =
            when (tnf) {
                0 -> NdefEmpty(tnf, type, id, payload)
                1 -> when (type) {
                    ImmutableByteArray.fromASCII("T") -> NdefText(tnf, type, id, payload)
                    ImmutableByteArray.fromASCII("U") -> NdefUri(tnf, type, id, payload)
                    else -> NdefUnknownRTD(tnf, type, id, payload)
                }
                2 -> when (type) {
                    ImmutableByteArray.fromASCII("application/vnd.wfa.wsc") -> NdefWifi(
                        tnf,
                        type,
                        id,
                        payload
                    )
                    else -> NdefUnknownMIME(tnf, type, id, payload)
                }
                3 -> NdefUriType(tnf, type, id, payload)
                4 -> when (type) {
                    ImmutableByteArray.fromASCII("android.com:pkg") -> NdefAndroidPkg(
                        tnf,
                        type,
                        id,
                        payload
                    )
                    else -> NdefUnknownExtType(tnf, type, id, payload)
                }
                5 -> NdefBinaryType(tnf, type, id, payload)
                else -> NdefInvalidType(tnf, type, id, payload)
            }

        const val NAME = "NDEF"
        val CARD_INFO = CardInfo(
            name = NAME,
            cardType = CardType.MultiProtocol,
            region = TransitRegion.WORLDWIDE,
            locationId = R.string.location_worldwide,
            preview = true,
            imageId = R.drawable.nmark
        )
    }

    private operator fun plus(second: NdefData) = NdefData(
        entries = this.entries + second.entries
    )
}
