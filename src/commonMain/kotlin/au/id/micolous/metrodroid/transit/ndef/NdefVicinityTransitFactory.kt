package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.card.nfcv.NFCVCardTransitFactory
import au.id.micolous.metrodroid.card.nfcv.NFCVCard
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

object NdefVicinityTransitFactory : NFCVCardTransitFactory {
    override fun parseTransitIdentity(card: NFCVCard) =
        TransitIdentity(NdefData.NAME, null)

    override fun parseTransitData(card: NFCVCard): TransitData? =
        NdefData.parseNFCV(card)

    override fun check(card: NFCVCard): Boolean = NdefData.checkNFCV(card)

    override val allCards: List<CardInfo>
        get() = listOf(NdefData.CARD_INFO)
}