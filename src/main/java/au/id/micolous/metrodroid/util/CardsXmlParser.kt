package au.id.micolous.metrodroid.util

import android.util.Xml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer

import java.io.IOException
import java.io.StringWriter
import java.util.NoSuchElementException

class CardsXmlParser(
        //private static final String FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";

        private val mxpp: XmlPullParser)// Not on Android :(
//xpp.setFeature(FEATURE_XML_ROUNDTRIP, true);
    : Iterator<String> {

    private var mRootTag: String? = null
    private var mSerializer: XmlSerializer? = null

    private var mCurrentCard: StringWriter? = null
    private var mCardDepth = 0


    override fun next(): String {
        try {
            if (mSerializer != null || prepareMore()) {
                if (mSerializer == null) {
                    throw NoSuchElementException()
                }

                val o = mCurrentCard!!.toString()
                mCurrentCard = null
                mSerializer = null
                return o
            } else {
                throw NoSuchElementException()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: XmlPullParserException) {
            throw RuntimeException(e)
        }

    }

    override fun hasNext(): Boolean {
        try {
            return prepareMore()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: XmlPullParserException) {
            throw RuntimeException(e)
        }

    }

    @Throws(IOException::class)
    private fun newCard() {
        mSerializer = Xml.newSerializer()
        mCurrentCard = StringWriter()
        mSerializer!!.setOutput(mCurrentCard)
        mSerializer!!.startDocument(null, false)
        copyStartTag()
    }

    @Throws(IOException::class)
    private fun copyStartTag() {
        mSerializer!!.startTag(mxpp.namespace, mxpp.name)
        for (i in 0 until mxpp.attributeCount) {
            mSerializer!!.attribute(mxpp.getAttributeNamespace(i),
                    mxpp.getAttributeName(i),
                    mxpp.getAttributeValue(i))
        }
    }

    @Throws(IOException::class)
    private fun copyEndTag() {
        mSerializer!!.endTag(mxpp.namespace, mxpp.name)
    }

    @Throws(IOException::class)
    private fun copyText() {
        mSerializer!!.text(mxpp.text)
    }


    @Throws(IOException::class, XmlPullParserException::class)
    private fun prepareMore(): Boolean {
        var eventType = mxpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (mRootTag == null) {
                if (eventType == XmlPullParser.START_TAG) {
                    // We have an root tag!
                    mRootTag = mxpp.name

                    if ("card".equals(mRootTag!!, ignoreCase = true)) {
                        newCard()
                    } else if (!"cards".equals(mRootTag!!, ignoreCase = true)) {
                        // Unexpected content
                        throw XmlPullParserException("Unexpected document root: " + mxpp.name)
                    }

                    // Handled
                }
                // Ignore other events.
            } else if (mSerializer == null) {
                when (eventType) {
                    XmlPullParser.START_TAG -> if ("card".equals(mxpp.name, ignoreCase = true)) {
                        newCard()
                    } else {
                        // Unexpected start tag
                        throw XmlPullParserException("Unexpected start tag: " + mxpp.name)
                    }

                    XmlPullParser.END_TAG -> {
                    }
                }// We got to the end!
            } else {
                // There is a card currently processing.
                when (eventType) {
                    XmlPullParser.END_TAG -> {
                        copyEndTag()
                        if ("card".equals(mxpp.name, ignoreCase = true)) {
                            if (mCardDepth > 0) {
                                mCardDepth--
                            } else {
                                // End tag for card
                                mSerializer!!.endDocument()
                                mxpp.next()
                                return true
                            }
                        }
                    }

                    XmlPullParser.START_TAG -> {
                        copyStartTag()
                        if ("card".equals(mxpp.name, ignoreCase = true)) {
                            mCardDepth++
                        }
                    }

                    XmlPullParser.TEXT -> copyText()
                }
            }

            eventType = mxpp.next()
        }

        // End of document
        if (mCardDepth > 0 || mCurrentCard != null) {
            throw XmlPullParserException("unexpected document end")
        }

        return false
    }


}
