package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.card.felica.FelicaConsts
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

object NdefFelicaTransitFactory : FelicaCardTransitFactory {
    override fun parseTransitIdentity(card: FelicaCard) =
        TransitIdentity(NdefData.NAME, null)

    override fun parseTransitData(card: FelicaCard): TransitData? =
        NdefData.parseFelica(card)

    override fun check(card: FelicaCard): Boolean = NdefData.checkFelica(card)
    override fun earlyCheck(systemCodes: List<Int>): Boolean =
        FelicaConsts.SYSTEMCODE_NDEF in systemCodes

    override val allCards: List<CardInfo>
        get() = listOf(NdefData.CARD_INFO)
}