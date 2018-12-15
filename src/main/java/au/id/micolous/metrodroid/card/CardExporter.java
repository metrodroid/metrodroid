package au.id.micolous.metrodroid.card;

import java.io.OutputStream;

public interface CardExporter<T extends Card> {
    void writeCard(OutputStream s, T card) throws Exception;
}
