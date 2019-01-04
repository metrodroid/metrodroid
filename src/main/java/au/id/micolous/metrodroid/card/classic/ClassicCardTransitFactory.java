package au.id.micolous.metrodroid.card.classic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;

public interface ClassicCardTransitFactory extends CardTransitFactory<ClassicCard> {
    /**
     * The number of sectors from the MIFARE Classic card that must be read, before
     * {@link #earlyCheck(List)} or {@link #earlyCardInfo(List)} may be called.
     *
     * @return -1 if earlyCheck is not supported (default), 1 if sector 0 must be read, and so on.
     */
    default int earlySectors() {
        return -1;
    }

    /**
     * Check if a card is supported by this reader. This check must operate when only
     * {@link #earlySectors()} sectors have been read from the card.
     *
     * @see #check(ClassicCard)
     * @param sectors Sectors that have been retrieved from the card so far.
     * @return True if the card is supported by this reader.
     */
    default boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
        return false;
    }

    /**
     * A {@link CardInfo} for the card that has been read by the reader.
     *
     * This is called only after {@link #earlyCheck(List)} has returned True
     *
     * By default, this returns
     * the first entry of {@link #getAllCards()}. This is normally sufficient for most readers.
     *
     * Note: This can return null if {@link #getAllCards()} returns an empty collection.
     *
     * @param sectors Sectors that have been retrieved from the card so far.
     * @return A {@link CardInfo} for the card, or null if the info is not available.
     */
    @Nullable
    default CardInfo earlyCardInfo(@NonNull List<ClassicSector> sectors) {
        final List<CardInfo> cards = getAllCards();
        if (!cards.isEmpty()) {
            return cards.get(0);
        }

        return null;
    }

    /**
     * Checks if a {@link ClassicCard} is supported by this reader.
     *
     * Data checked here contains a complete {@link ClassicCard} structure, with all possible
     * sectors read. By default, this calls {@link #earlyCheck(List)}.
     *
     * @see CardTransitFactory#check(Object)
     * @param card A card to check.
     * @return true if this reader can decode this card.
     */
    @Override
    default boolean check(@NonNull ClassicCard card) {
        return earlyCheck(card.getSectors());
    }

    /**
     * Check if the sector is dynamic.
     *
     * This is called only after {@link #earlyCheck(List)} has returned True
     *
     * By default, this returns false.
     *
     * If reader has only static keys and this returns False then reader will skip most
     * of the keyfinding and will declare the sector as unauthorized much earlier
     *
     * @param sectors Sectors that have been retrieved from the card so far.
     * @param keyType
     * @return A {@link CardInfo} for the card, or null if the info is not available.
     */
    default boolean isDynamicKeys(@NonNull List<ClassicSector> sectors, int sectorIndex, ClassicSectorKey.KeyType keyType) {
        return false;
    }
}
