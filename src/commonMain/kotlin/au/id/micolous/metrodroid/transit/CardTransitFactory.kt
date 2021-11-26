package au.id.micolous.metrodroid.transit

interface CardTransitFactory<T> {
    val allCards: List<CardInfo>
        get() = emptyList()

    fun parseTransitIdentity(card: T): TransitIdentity?

    fun parseTransitData(card: T): TransitData?

    fun check(card: T): Boolean
}
