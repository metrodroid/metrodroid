package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo

internal object IntercodeLookupOura : IntercodeLookupSTR("oura") {
    override val cardInfo: CardInfo
        get() = CardInfo(
                name = "OùRA",
                locationId = R.string.location_grenoble,
                imageId = R.drawable.oura,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816)
}