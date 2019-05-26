package au.id.micolous.metrodroid.transit.kr_ocap

import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.card.ksx6924.KROCAPConfigDFApplication
import au.id.micolous.metrodroid.card.ksx6924.KROCAPData
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Reader for South Korean One Card All Pass Config DF FCI.
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/South-Korea#a0000004520001
 */
@Parcelize
class KROCAPTransitData(val pdata: ImmutableByteArray) : SerialOnlyTransitData() {

    override val reason: Reason
        get() = Reason.MORE_RESEARCH_NEEDED

    override val serialNumber get() = getSerial(pdata)

    override val cardName get() = NAME

    override val extraInfo: List<ListItem>?
        get() = ISO7816TLV.infoBerTLV(pdata, KROCAPData.TAGMAP)

    companion object {
        private const val NAME = "One Card All Pass"
        private val TAG_SERIAL_NUMBER = ImmutableByteArray.fromHex("12")

        private fun getSerial(pdata: ImmutableByteArray) =
                ISO7816TLV.findBERTLV(pdata, TAG_SERIAL_NUMBER, false)?.getHexString()


        fun parseTransitIdentity(card: KROCAPConfigDFApplication): TransitIdentity? {
            return card.appProprietaryBerTlv?.let {
                TransitIdentity(NAME, getSerial(it))
            }
        }

        fun parseTransitData(card: KROCAPConfigDFApplication) =
                card.appProprietaryBerTlv?.let { KROCAPTransitData(it) }
    }
}