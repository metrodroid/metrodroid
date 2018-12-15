package au.id.micolous.metrodroid.card;

import java.io.OutputStream;
import java.util.Iterator;

/**
 * Interface for exporting multiple cards into a single file.
 *
 * @param <T> An optional subclass of Card to declare specific format emissions. For example, a
 *            format that only supports MIFARE Classic should declare
 *            {@link au.id.micolous.metrodroid.card.classic.ClassicCard} here.
 */
public abstract class CardsExporter<T extends Card> implements CardExporter<T> {
    public abstract void writeCards(OutputStream s, Iterator<T> cards) throws Exception;

    public void writeCards(OutputStream os, Iterable<T> cards) throws Exception {
        writeCards(os, cards.iterator());
    }
}
