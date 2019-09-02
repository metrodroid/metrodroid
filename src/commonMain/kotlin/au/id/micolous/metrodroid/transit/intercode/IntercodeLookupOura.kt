package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.transit.CardInfo

internal object IntercodeLookupOura : IntercodeLookupSTR("oura"), IntercodeLookupSingle {
    override val cardInfo: CardInfo
        get() = CardInfo(
                name = "OùRA",
                locationId = R.string.location_grenoble,
                imageId = R.drawable.oura,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816)

    override val subscriptionMapByAgency: Map<Pair<Int?, Int>, StringResource> = mapOf(
            Pair(2, 0x6601) to R.string.oura_billet_tarif_normal
    )
}
