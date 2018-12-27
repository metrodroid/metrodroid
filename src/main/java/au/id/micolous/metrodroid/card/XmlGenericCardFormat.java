package au.id.micolous.metrodroid.card;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Xml;

import org.simpleframework.xml.Serializer;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.util.IteratorTransformer;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.XmlPullParserIterator;

public class XmlGenericCardFormat<T extends Card> extends CardsExporter<T> implements CardImporter.Text<T> {
    private static final byte[] CARDS_HEADER =
            Utils.stringToByteArray("<?xml version=\"1.0\" encoding=\"UTF-8\"?><cards>\n");
    private static final byte[] CARDS_FOOTER =
            Utils.stringToByteArray("</cards>\n");
    private static final byte[] CARDS_SEPARATOR = { 10 }; //  \n

    private final Serializer mSerializer;
    private final Class<T> mCardClass;

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public XmlGenericCardFormat(@NonNull Class<T> cardClass) {
        this(MetrodroidApplication.getInstance().getSerializer(), cardClass);
    }

    @VisibleForTesting
    public XmlGenericCardFormat(@NonNull Serializer serializer, @NonNull Class<T> cardClass) {
        mSerializer = serializer;
        mCardClass = cardClass;
    }

    @Nullable
    @Override
    public Iterator<T> readCards(@NonNull Reader reader) throws Exception {
        XmlPullParser xpp = Xml.newPullParser();
        xpp.setInput(reader);
        XmlPullParserIterator it = new XmlPullParserIterator(xpp);

        return new IteratorTransformer<>(it, c -> {
            try {
                return readCard(c);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Nullable
    @Override
    public T readCard(@NonNull Reader reader) throws Exception {
        return mSerializer.read(mCardClass, reader);
    }

    @Override
    public T readCard(@NonNull final String xml) throws Exception {
        return mSerializer.read(mCardClass, xml);
    }

    @Override
    public void writeCard(OutputStream os, T card) throws Exception {
        mSerializer.write(card, os);
    }

    public String writeCard(T card) throws Exception {
        StringWriter sw = new StringWriter();
        mSerializer.write(card, sw);
        return sw.toString();
    }

    @Override
    public void writeCards(OutputStream os, Iterator<T> cards) throws Exception {
        writeCardsFromString(os, new IteratorTransformer<>(cards, input -> {
            try {
                return writeCard(input);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void writeCardsFromString(OutputStream os,
                                     Iterator<? extends String> cards) throws IOException {
        os.write(CARDS_HEADER);

        while (cards.hasNext()) {
            final String s = cards.next();
            os.write(Utils.stringToUtf8(cutXmlDef(s)));
            os.write(CARDS_SEPARATOR);
        }

        os.write(CARDS_FOOTER);

    }

    private static String cutXmlDef(String data) {
        if (!data.startsWith("<?"))
            return data;
        return data.substring(data.indexOf("?>")+2);
    }
}
