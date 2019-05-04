package au.id.micolous.metrodroid.transit.tmoney

import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.card.ksx6924.KSX6924CardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData

expect class TMoneyTransitData : TransitData {
    constructor(card: KSX6924Application)

    companion object {
        val CARD_INFO: CardInfo
        val FACTORY: KSX6924CardTransitFactory
    }
}
