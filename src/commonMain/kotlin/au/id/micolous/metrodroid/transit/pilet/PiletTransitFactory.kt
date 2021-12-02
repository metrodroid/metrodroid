package au.id.micolous.metrodroid.transit.pilet

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.emv.EmvData
import au.id.micolous.metrodroid.transit.ndef.NdefData
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

abstract class PiletTransitFactory: ClassicCardTransitFactory {
    abstract val cardName: String
    abstract val ndefType: String
    abstract val ndefAid: Int
    abstract val reason: SerialOnlyTransitData.Reason
    abstract val serialPrefixLen: Int
    override fun parseTransitIdentity(card: ClassicCard) : TransitIdentity? {
        val tlv = getTlv(card) ?: return null
        val serial = getSerial(tlv) ?: return null
        return TransitIdentity(cardName, serial)
    }

    override fun parseTransitData(card: ClassicCard): TransitData? {
        val tlv = getTlv(card) ?: return null
        val serial = getSerial(tlv) ?: return null
        return PiletTransitData(berTlv = tlv, cardName = cardName,
            serialNumber = serial, reason = reason)
    }

    override fun check(card: ClassicCard): Boolean = parseTransitIdentity(card) != null

    private fun getTlv(card: ClassicCard): ImmutableByteArray? {
        val ndef = NdefData.parseClassic(card, ndefAid) ?: return null
        val entry = ndef.getEntryExtType(ndefType) ?: return null
        return entry.payload
    }

    private fun getSerial(tlv: ImmutableByteArray): String? =
        ISO7816TLV.findBERTLV(tlv, EmvData.TAG_PAN,
            keepHeader = false, multihead = true)
            ?.readASCII()?.substring(serialPrefixLen)
}