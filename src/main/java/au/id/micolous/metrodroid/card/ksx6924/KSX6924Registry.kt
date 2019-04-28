package au.id.micolous.metrodroid.card.ksx6924

import au.id.micolous.metrodroid.transit.snapper.SnapperTransitData
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData

actual object KSX6924Registry {
    actual val allFactories: List<KSX6924CardTransitFactory> = listOf(
            SnapperTransitData.FACTORY,
            TMoneyTransitData.FACTORY)
}
