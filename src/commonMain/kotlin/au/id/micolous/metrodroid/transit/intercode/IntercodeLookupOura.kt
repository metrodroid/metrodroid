package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo

internal object IntercodeLookupOura : IntercodeLookupSTR("oura"), IntercodeLookupSingle {
    override val cardInfo: CardInfo
        get() = CardInfo(
                name = "OùRA",
                locationId = R.string.location_grenoble,
                imageId = R.drawable.oura,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816)

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? =
            subs[Pair(agency, contractTariff)]?.let { Localizer.localizeString(it) } ?: Localizer.localizeString(R.string.unknown_format, contractTariff)

    private val subs = mapOf(
            Pair(2, 0x6601) to R.string.oura_billet_tarif_normal
    )
}