package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.transit.edy.EdyTransitData
import au.id.micolous.metrodroid.transit.kmt.KMTTransitData
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData

actual object FelicaRegistry {
    actual val allFactories: List<FelicaCardTransitFactory> = listOf(
                SuicaTransitData.FACTORY,
                EdyTransitData.FACTORY,
                KMTTransitData.FACTORY,
                OctopusTransitData.FACTORY)
}
