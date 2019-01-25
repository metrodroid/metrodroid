package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets
import java.io.PushbackInputStream
import org.apache.commons.io.IOUtils
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

actual class XmlCardFormat : CardImporter {
    override fun readCard(stream: InputStream): Card = readCardXML(stream)

    override fun readCards(stream: InputStream): Iterator<Card> {
        return iterateXmlCards(stream) { readCard(it)!! }
    }
}

class XmlOrJsonCardFormat : CardImporter {
    private val jsonKotlinFormat = JsonKotlinFormat()
    private fun peek(pb: PushbackInputStream): Char {
        var c: Int
        while (true) {
            c = pb.read()
            if (!Character.isSpaceChar(c.toChar()))
                break
        }
        pb.unread(c)
        return c.toChar()
    }

    override fun readCards(stream: InputStream): Iterator<Card>? {
        val pb = PushbackInputStream(stream)
        when (peek(pb)) {
            '<' -> return iterateXmlCards(pb) { readCard(it) }
            '[', '{' -> return listOf(jsonKotlinFormat.readCard(IOUtils.toString(pb, Charsets.UTF_8))).iterator()
            'P' -> return readZip(pb)
            else -> return null
        }
    }

    class ZipIterator (private val zi: ZipInputStream)
        : Iterator<Pair<ZipEntry, InputStream>> {
        override fun next(): Pair<ZipEntry, InputStream> {
            if (ze == null)
                ze = zi.nextEntry
            return Pair(ze!!, zi)
        }

        private var ze: ZipEntry? = null
        private var end = false
        override fun hasNext(): Boolean {
            if (end)
                return false
            if (ze == null)
                ze = zi.nextEntry
            if (ze == null)
                end = true
            return !end
        }
    }

    private fun readZip(stream: InputStream): Iterator<Card>? {
        return IteratorTransformerNotNull<Pair<ZipEntry, InputStream>, Card>(ZipIterator(ZipInputStream(stream))) {
            (ze, zi) ->
            if (ze.name.endsWith(".json"))
                jsonKotlinFormat.readCard(IOUtils.toString(zi, Charsets.UTF_8))
            else if (ze.name.endsWith(".xml"))
                readCardXML(zi)
            else
                null
        }
    }

    override fun readCard(stream: InputStream): Card {
        val pb = PushbackInputStream(stream)
        if (peek(pb) == '<')
            return readCardXML(pb)
        return jsonKotlinFormat.readCard(IOUtils.toString(pb, Charsets.UTF_8))
    }

    override fun readCard(input: String): Card {
        val trimmed = input.trim()
        if (trimmed[0] == '<')
            return readCardXML(trimmed.byteInputStream(Charsets.UTF_8))
        return jsonKotlinFormat.readCard(trimmed)
    }
}