package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.CardTransitFactory

@JvmSuppressWildcards(true)
interface ClassicCardTransitFactory : CardTransitFactory<ClassicCard> {
    /**
     * The number of sectors from the MIFARE Classic card that must be read, before
     * [.earlyCheck] or [.earlyCardInfo] may be called.
     *
     * @return -1 if earlyCheck is not supported (default), 1 if sector 0 must be read, and so on.
     */
    @JvmDefault
    val earlySectors: Int
        get() = -1

    /**
     * Check if a card is supported by this reader. This check must operate when only
     * [.earlySectors] sectors have been read from the card.
     *
     * @see .check
     * @param sectors Sectors that have been retrieved from the card so far.
     * @return True if the card is supported by this reader.
     */
    @JvmDefault
    fun earlyCheck(sectors: List<ClassicSector>): Boolean = false

    /**
     * A [CardInfo] for the card that has been read by the reader.
     *
     * This is called only after [.earlyCheck] has returned True
     *
     * By default, this returns
     * the first entry of [.getAllCards]. This is normally sufficient for most readers.
     *
     * Note: This can return null if [.getAllCards] returns an empty collection.
     *
     * @param sectors Sectors that have been retrieved from the card so far.
     * @return A [CardInfo] for the card, or null if the info is not available.
     */
    @JvmDefault
    fun earlyCardInfo(sectors: List<ClassicSector>): CardInfo? = allCards.firstOrNull()

    /**
     * Checks if a [ClassicCard] is supported by this reader.
     *
     * Data checked here contains a complete [ClassicCard] structure, with all possible
     * sectors read. By default, this calls [.earlyCheck].
     *
     * @see CardTransitFactory.check
     * @param card A card to check.
     * @return true if this reader can decode this card.
     */
    @JvmDefault
    override fun check(card: ClassicCard): Boolean = earlyCheck(card.sectors)

    /**
     * Check if the sector is dynamic.
     *
     * This is called only after [.earlyCheck] has returned True
     *
     * By default, this returns false.
     *
     * If reader has only static keys and this returns False then reader will skip most
     * of the keyfinding and will declare the sector as unauthorized much earlier
     *
     * @param sectors Sectors that have been retrieved from the card so far.
     * @param keyType
     * @return A [CardInfo] for the card, or null if the info is not available.
     */
    @JvmDefault
    fun isDynamicKeys(sectors: List<ClassicSector>, sectorIndex: Int, keyType: ClassicSectorKey.KeyType): Boolean = false
}
