package au.id.micolous.metrodroid.transit

interface CardTransitFactory<T> {
    val allCards: List<CardInfo>

    fun parseTransitIdentity(card: T): TransitIdentity?

    fun parseTransitData(card: T): TransitData?

    fun check(card: T): Boolean

    val notice: String?
        get() = null
}
