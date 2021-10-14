package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.transit.edy.EdyTransitData
import au.id.micolous.metrodroid.transit.kmt.KMTTransitData
import au.id.micolous.metrodroid.transit.mrtj.MRTJTransitData
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData

object FelicaRegistry {
    val allFactories: List<FelicaCardTransitFactory> = listOf(
                SuicaTransitData.FACTORY,
                EdyTransitData.FACTORY,
                KMTTransitData.FACTORY,
                MRTJTransitData.FACTORY,
                OctopusTransitData.FACTORY)
}
