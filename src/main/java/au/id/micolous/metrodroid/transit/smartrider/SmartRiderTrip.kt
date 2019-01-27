package au.id.micolous.metrodroid.transit.smartrider

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.*

@Parcelize
internal class SmartRiderTrip (override val capsule: TransactionTripCapsule): TransactionTripAbstract() {
    override val fare: TransitCurrency?
        get() = TransitCurrency.AUD(((start as? SmartRiderTagRecord?)?.cost ?: 0) +
                    ((end as? SmartRiderTagRecord?)?.cost ?: 0))

    override val routeLanguage: String?
        get() = "en-AU"

    constructor(el: Transaction) : this(TransactionTripAbstract.makeCapsule(el))
}
