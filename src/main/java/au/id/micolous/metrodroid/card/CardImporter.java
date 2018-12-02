package au.id.micolous.metrodroid.card;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.Iterators;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * Interface for writing card data importers.
 *
 * By default, this adopts a binary-based ({@link InputStream}) model. If a character (string) model
 * is desired (such as a format which consists only of plain text), then implement {@link Text}
 * instead.
 *
 * @param <T> An optional subclass of Card to declare specific format emissions. For example, a
 *            format that only supports MIFARE Classic should declare
 *            {@link au.id.micolous.metrodroid.card.classic.ClassicCard} here.
 */
public interface CardImporter<T extends Card> {

    /**
     * Reads cards from the given stream.
     *
     * Implementations should read the file incrementally (lazy), to save memory.
     *
     * By default, this tries to read one card (using {@link #readCard(InputStream)}, and returns a
     * singleton iterator.
     *
     * @param stream Stream to read the card content from.
     */
    @Nullable
    default Iterator<T> readCards(@NonNull InputStream stream) throws Exception {
        final T card = readCard(stream);
        if (card == null) {
            return null;
        } else {
            return Iterators.singletonIterator(card);
        }
    }

    /**
     * Reads cards from the given String.
     *
     * This method should only be used for data which is already in memory. If loading data from
     * some other source, use {@link #readCards(InputStream)} instead.
     * @param s String to read from.
     */
    @Nullable
    default Iterator<T> readCards(@NonNull String s) throws Exception {
        return readCards(IOUtils.toInputStream(s, Charset.defaultCharset()));
    }

    /**
     * Reads a single card from the given stream.
     *
     * Implementations should read the card immediately.
     *
     * @param stream Stream to read the card content from.
     */
    @Nullable
    T readCard(@NonNull InputStream stream) throws Exception;


    default T readCard(@NonNull String s) throws Exception {
        return readCard(IOUtils.toInputStream(s, Charset.defaultCharset()));
    }


    /**
     * Wrapper which allows implementors to get {@link Reader} interfaces rather than
     * {@link InputStream}.
     *
     * @see CardImporter
     */
    interface Text<T extends Card> extends CardImporter<T> {
        @Nullable
        @Override
        default Iterator<T> readCards(@NonNull InputStream stream) throws Exception {
            return readCards(new InputStreamReader(stream));
        }

        @Nullable
        default Iterator<T> readCards(@NonNull Reader reader) throws Exception {
            final T card = readCard(reader);
            if (card == null) {
                return null;
            } else {
                return Iterators.singletonIterator(card);
            }
        }

        default Iterator<T> readCards(@NonNull String s) throws Exception {
            return readCards(new StringReader(s));
        }

        @Nullable
        default T readCard(@NonNull InputStream stream) throws Exception {
            return readCard(new InputStreamReader(stream));
        }

        @Nullable
        T readCard(@NonNull Reader reader) throws Exception;

        default T readCard(@NonNull final String s) throws Exception {
            return readCard(new StringReader(s));
        }
    }


}
