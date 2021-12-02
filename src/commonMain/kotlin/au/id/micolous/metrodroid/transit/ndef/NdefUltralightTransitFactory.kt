package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

object NdefUltralightTransitFactory : UltralightCardTransitFactory {
    override fun parseTransitIdentity(card: UltralightCard) =
        TransitIdentity(NdefData.NAME, null)

    override fun parseTransitData(card: UltralightCard): TransitData? =
        NdefData.parseUltralight(card)

    override fun check(card: UltralightCard): Boolean = NdefData.checkUltralight(card)

    override val allCards: List<CardInfo>
        get() = listOf(NdefData.CARD_INFO)
}