package au.id.micolous.metrodroid.transit.ezlinkcompat

import au.id.micolous.metrodroid.card.cepascompat.CEPASCard
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity

expect class EZLinkCompatTransitData : TransitData {
    constructor(cepasCard: CEPASCard)

    companion object {
        fun parseTransitIdentity(card: CEPASCard): TransitIdentity
    }
}