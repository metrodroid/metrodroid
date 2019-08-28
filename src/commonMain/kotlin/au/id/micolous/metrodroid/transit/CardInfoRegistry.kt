package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.card.calypso.CalypsoRegistry
import au.id.micolous.metrodroid.card.china.ChinaRegistry
import au.id.micolous.metrodroid.card.classic.ClassicCardFactoryRegistry
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitRegistry
import au.id.micolous.metrodroid.card.felica.FelicaRegistry
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Registry
import au.id.micolous.metrodroid.card.ultralight.UltralightTransitRegistry
import au.id.micolous.metrodroid.transit.emv.EmvTransitFactory
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitFactory
import au.id.micolous.metrodroid.util.collatedBy

object CardInfoRegistry {
    val allFactories = ClassicCardFactoryRegistry.allFactories +
            CalypsoRegistry.allFactories +
            DesfireCardTransitRegistry.allFactories +
            FelicaRegistry.allFactories +
            UltralightTransitRegistry.allFactories +
            ChinaRegistry.allFactories +
            KSX6924Registry.allFactories +
            EmvTransitFactory +
            EZLinkTransitFactory

    val allCards = allFactories.flatMap { it.allCards }

    val allCardsAlphabetical: List<CardInfo>
        get () = allCards.collatedBy { it.name }
}
