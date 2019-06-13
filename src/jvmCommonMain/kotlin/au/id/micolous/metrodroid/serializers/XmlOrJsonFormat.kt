package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets
import java.io.PushbackInputStream
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
    private val mfcFormat = MfcCardImporter()
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
            '[', '{' -> return listOf(jsonKotlinFormat.readCard(pb.bufferedReader().readText())).iterator()
            'P' -> return readZip(pb)
            else -> return null
        }
    }

    class ZipIterator (private val zi: ZipInputStream)
        : Iterator<Pair<ZipEntry, InputStream>> {
        override fun next(): Pair<ZipEntry, InputStream> {
            if (!advanced)
                ze = zi.nextEntry
            advanced = false
            return Pair(ze!!, zi)
        }

        private var ze: ZipEntry? = null
        private var end = false
        private var advanced = false
        override fun hasNext(): Boolean {
            if (end)
                return false
            if (!advanced)
                ze = zi.nextEntry
            advanced = true
            if (ze == null)
                end = true
            return !end
        }
    }

    private fun readZip(stream: InputStream): Iterator<Card>? {
        return IteratorTransformerNotNull(ZipIterator(ZipInputStream(stream))) {
            (ze, zi) ->
            Log.d("Importer", "Importing ${ze.name}")
            when {
                ze.name.endsWith(".json") -> jsonKotlinFormat.readCard(zi.bufferedReader().readText())
                ze.name.endsWith(".xml") -> readCardXML(zi)
                ze.name.endsWith(".mfc") -> mfcFormat.readCard(zi)
                else -> null
            }
        }
    }

    override fun readCard(stream: InputStream): Card {
        val pb = PushbackInputStream(stream)
        if (peek(pb) == '<')
            return readCardXML(pb)
        return jsonKotlinFormat.readCard(pb.bufferedReader().readText())
    }

    override fun readCard(input: String): Card {
        val trimmed = input.trim()
        if (trimmed[0] == '<')
            return readCardXML(trimmed.byteInputStream(Charsets.UTF_8))
        return jsonKotlinFormat.readCard(trimmed)
    }
}