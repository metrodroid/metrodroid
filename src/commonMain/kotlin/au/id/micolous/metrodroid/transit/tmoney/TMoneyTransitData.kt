package au.id.micolous.metrodroid.transit.tmoney

import au.id.micolous.metrodroid.card.tmoney.TMoneyCard
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

expect class TMoneyTransitData : TransitData {
    constructor(tMoneyCard: TMoneyCard)

    companion object {
        fun parseTransitIdentity(card: TMoneyCard): TransitIdentity
        val CARD_INFO: CardInfo
    }
}
