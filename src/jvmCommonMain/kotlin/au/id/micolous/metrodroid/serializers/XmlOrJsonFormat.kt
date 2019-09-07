package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets
import java.io.PushbackInputStream
import java.util.zip.ZipInputStream

class XmlCardFormat : CardImporter {
    override fun readCard(stream: InputStream): Card = readCardXML(stream)

    override fun readCards(stream: InputStream): Iterator<Card> {
        return iterateXmlCards(stream) { readCard(it)!! }
    }
}

class XmlOrJsonCardFormat : CardImporter {
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
            'P' -> return readZip(pb).iterator()
            else -> return null
        }
    }

    private fun readZip(stream: InputStream): List<Card> {
        val zi = ZipInputStream(stream)
        val m = mutableListOf<Card>()
        while (true) {
            val ze = zi.nextEntry ?: break
            Log.d("Importer", "Importing ${ze.name}")
            when {
                ze.name.endsWith(".json") -> m += jsonKotlinFormat.readCard(zi.bufferedReader().readText())
                ze.name.endsWith(".xml") -> m += readCardXML(zi)
                ze.name.endsWith(".mfc") -> m += mfcFormat.readCard(zi)
            }
        }
        return m
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

    companion object {
        private val jsonKotlinFormat = JsonKotlinFormat()
        private val xmlFormat = XmlCardFormat()

        fun parseString(xml: String) = try {
            when (xml.find { it !in listOf('\n', '\r', '\t', ' ') }) {
                '<' -> xmlFormat.readCard(xml)
                '{', '[' -> jsonKotlinFormat.readCard(xml)
                else -> null
            }
        } catch (ex: Exception) {
            Log.e("Card", "Failed to deserialize", ex)
            throw RuntimeException(ex)
        }
    }
}
