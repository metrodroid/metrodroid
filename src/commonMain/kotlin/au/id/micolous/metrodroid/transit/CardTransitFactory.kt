package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.JvmDefault

interface CardTransitFactory<T> {
    @JvmDefault
    val allCards: List<CardInfo>
        get() = emptyList()

    fun parseTransitIdentity(card: T): TransitIdentity?

    fun parseTransitData(card: T): TransitData?

    fun check(card: T): Boolean
}
