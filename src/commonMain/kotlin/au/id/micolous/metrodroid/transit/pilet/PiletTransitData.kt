package au.id.micolous.metrodroid.transit.pilet

import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.card.iso7816.TagContents
import au.id.micolous.metrodroid.card.iso7816.TagDesc
import au.id.micolous.metrodroid.card.iso7816.TagHiding
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.emv.EmvData
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Preferences

@Parcelize
data class PiletTransitData (
    private val berTlv: ImmutableByteArray,
    override val serialNumber: String,
    override val cardName: String,
    override val reason: Reason
): SerialOnlyTransitData() {
    override val extraInfo: List<ListItemInterface>
        get() = ISO7816TLV.infoBerTLVs(listOf(berTlv),
            TAG_MAP,
            hideThings = Preferences.obfuscateTripDates || Preferences.hideCardNumbers,
            multihead = true
        )

    companion object {
        private val TAG_MAP = mapOf(
            EmvData.TAG_PAN to TagDesc(
                R.string.full_serial_number,
                TagContents.ASCII, TagHiding.CARD_NUMBER
            ),
            EmvData.TAG_ISSUER_COUNTRY to TagDesc(
                R.string.issuer_country,
                TagContents.COUNTRY_ASCIINUM
            ),
            EmvData.TAG_CARD_EXPIRATION_DATE to TagDesc(
                R.string.emv_card_expiration_date,
                TagContents.ASCII, TagHiding.DATE
            ),
            EmvData.TAG_CARD_EFFECTIVE to TagDesc(
                R.string.emv_card_effective, TagContents.ASCII,
                TagHiding.DATE
            ), // TODO: show as date
            EmvData.TAG_INTERCHANGE_PROTOCOL to TagDesc(
                R.string.emv_interchange_control,
                TagContents.ASCII
            ),
            "53" to TagDesc(R.string.kiev_digital_uid, TagContents.DUMP_SHORT)
        )
    }
}
