package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log

import org.jetbrains.annotations.NonNls
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.util.*

object XmlPullFactory {
    private val factory: XmlPullParserFactory? by lazy {
        try {
            XmlPullParserFactory.newInstance()
                ?.also {
                    it.isNamespaceAware = true
                }
        } catch (e: Exception) {
            Log.e("XMLPullParser", "Error initing XmlPullParserFactory: $e")
            null
        }
    }

    fun newPullParser(): XmlPullParser = factory!!.newPullParser()
    fun newSerializer(): XmlSerializer = factory!!.newSerializer()
}

internal fun iterateXmlCards(stream: InputStream, iter: (String) -> Card?): Iterator<Card> {
    val xpp = XmlPullFactory.newPullParser()
    val reader = stream.reader(Charsets.UTF_8)
    xpp.setInput(reader)

    return IteratorTransformerNotNull(XmlPullParserIterator(xpp), iter)
}

private class XmlPullParserIterator(
        //private static final String FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";

        private val mxpp: XmlPullParser)// Not on Android :(
//xpp.setFeature(FEATURE_XML_ROUNDTRIP, true);
    : Iterator<String> {

    @NonNls
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

    private fun newCard() {
        mCurrentCard = StringWriter()
        mSerializer = XmlPullFactory.newSerializer().also {
            it.setOutput(mCurrentCard)
            it.startDocument(null, false)
        }
        copyStartTag()
    }

    private fun copyStartTag() {
        mSerializer!!.startTag(mxpp.namespace, mxpp.name)
        for (i in 0 until mxpp.attributeCount) {
            mSerializer!!.attribute(mxpp.getAttributeNamespace(i),
                    mxpp.getAttributeName(i),
                    filterBadXMLChars(mxpp.getAttributeValue(i)))
        }
    }

    private fun copyEndTag() {
        mSerializer!!.endTag(mxpp.namespace, mxpp.name)
    }

    private fun copyText() {
        mSerializer!!.text(filterBadXMLChars(mxpp.text))
    }

    private fun isCard(s: String) = s.lowercase(Locale.US) == "card"

    @SuppressWarnings("CallToSuspiciousStringMethod")
    private fun prepareMore(): Boolean {
        var eventType = mxpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (mRootTag == null) {
                if (eventType == XmlPullParser.START_TAG) {
                    // We have an root tag!
                    mRootTag = mxpp.name

                    when (mRootTag?.lowercase(Locale.US)) {
                        "card" -> newCard()
                        "cards" -> {}
                        else -> {
                            // Unexpected content
                            throw XmlPullParserException("Unexpected document root: ${mxpp.name}")
                        }
                    }
                    // Handled
                }
                // Ignore other events.
            } else if (mSerializer == null) {
                when (eventType) {
                    XmlPullParser.START_TAG -> if (isCard(mxpp.name)) {
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
                        if (isCard(mxpp.name)) {
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
                        if (isCard(mxpp.name)) {
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
