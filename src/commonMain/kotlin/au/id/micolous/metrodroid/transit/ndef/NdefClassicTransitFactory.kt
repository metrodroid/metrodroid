package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

object NdefClassicTransitFactory : ClassicCardTransitFactory {
    override fun parseTransitIdentity(card: ClassicCard) =
        TransitIdentity(NdefData.NAME, null)

    override fun parseTransitData(card: ClassicCard): TransitData? =
        NdefData.parseClassic(card)

    override val earlySectors: Int
        /* 1 is enough to detect most of NDEF cards but we need sector 1 to
           distinguish it from Tartu Bus.  */
        get() = 2

    override fun earlyCheck(sectors: List<ClassicSector>): Boolean =
        MifareClassicAccessDirectory.sector0Contains(sectors[0], MifareClassicAccessDirectory.NFC_AID)

    override fun check(card: ClassicCard): Boolean = NdefData.checkClassic(card)

    override val allCards: List<CardInfo>
        get() = listOf(NdefData.CARD_INFO)
}
