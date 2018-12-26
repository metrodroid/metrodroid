package au.id.micolous.metrodroid.util;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.jetbrains.annotations.NonNls;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class XmlPullParserIterator implements Iterator<String> {
    //private static final String FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";

    @NonNull
    private final XmlPullParser mxpp;

    @NonNls
    private String mRootTag = null;
    private XmlSerializer mSerializer = null;

    private StringWriter mCurrentCard = null;
    private int mCardDepth = 0;

    public XmlPullParserIterator(@NonNull XmlPullParser xpp) {
        // Not on Android :(
        //xpp.setFeature(FEATURE_XML_ROUNDTRIP, true);
        mxpp = xpp;
    }


    public String next() {
        try {
            if (mSerializer != null || prepareMore()) {
                if (mSerializer == null) {
                    throw new NoSuchElementException();
                }

                final String o = mCurrentCard.toString();
                mCurrentCard = null;
                mSerializer = null;
                return o;
            } else {
                throw new NoSuchElementException();
            }
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return prepareMore();
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    private void newCard() throws IOException {
        mSerializer = Xml.newSerializer();
        mCurrentCard = new StringWriter();
        mSerializer.setOutput(mCurrentCard);
        mSerializer.startDocument(null, false);
        copyStartTag();
    }

    private void copyStartTag() throws IOException {
        mSerializer.startTag(mxpp.getNamespace(), mxpp.getName());
        for (int i=0; i<mxpp.getAttributeCount(); i++) {
            mSerializer.attribute(mxpp.getAttributeNamespace(i),
                    mxpp.getAttributeName(i),
                    Utils.filterBadXMLChars(mxpp.getAttributeValue(i)));
        }
    }

    private void copyEndTag() throws IOException {
        mSerializer.endTag(mxpp.getNamespace(), mxpp.getName());
    }

    private void copyText() throws IOException {
        mSerializer.text(Utils.filterBadXMLChars(mxpp.getText()));
    }


    private boolean prepareMore() throws IOException, XmlPullParserException {
        int eventType = mxpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (mRootTag == null) {
                if (eventType == XmlPullParser.START_TAG) {
                    // We have an root tag!
                    mRootTag = mxpp.getName();

                    if ("card".equalsIgnoreCase(mRootTag)) {
                        newCard();
                    } else if (!"cards".equalsIgnoreCase(mRootTag)) {
                        // Unexpected content
                        throw new XmlPullParserException("Unexpected document root: " + mxpp.getName());
                    }

                    // Handled
                }
                // Ignore other events.
            } else if (mSerializer == null) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("card".equalsIgnoreCase(mxpp.getName())) {
                            newCard();
                        } else {
                            // Unexpected start tag
                            throw new XmlPullParserException("Unexpected start tag: " + mxpp.getName());
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        // We got to the end!
                        break;


                }
            } else {
                // There is a card currently processing.
                switch (eventType) {
                    case XmlPullParser.END_TAG:
                        copyEndTag();
                        if ("card".equalsIgnoreCase(mxpp.getName())) {
                            if (mCardDepth > 0) {
                                mCardDepth--;
                            } else {
                                // End tag for card
                                mSerializer.endDocument();
                                mxpp.next();
                                return true;
                            }
                        }
                        break;

                    case XmlPullParser.START_TAG:
                        copyStartTag();
                        if ("card".equalsIgnoreCase(mxpp.getName())) {
                            mCardDepth++;
                        }
                        break;

                    case XmlPullParser.TEXT:
                        copyText();
                        break;
                }
            }

            eventType = mxpp.next();
        }

        // End of document
        if (mCardDepth > 0 || mCurrentCard != null) {
            throw new XmlPullParserException("unexpected document end");
        }

        return false;
    }


}
