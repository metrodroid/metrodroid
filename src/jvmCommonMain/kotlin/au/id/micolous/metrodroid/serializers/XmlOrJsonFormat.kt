package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.util.JavaStreamInput
import au.id.micolous.metrodroid.util.peekAndSkipSpace
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.util.zip.ZipInputStream

class XmlCardFormat : CardMultiImporter {
    @OptIn(ExperimentalStdlibApi::class)
    override fun readCards(stream: InputStream): Iterator<Card>
        = iterateXmlCards(stream) { readCardXML(ByteArrayInputStream(it.encodeToByteArray())) }
}

class XmlOrJsonCardFormat : CardMultiImporter {
    private val mfcFormat = MfcCardImporter()

    @OptIn(ExperimentalStdlibApi::class)
    override fun readCards(stream: InputStream): Iterator<Card>? {
        val pb = PushbackInputStream(stream)
        when (pb.peekAndSkipSpace().toInt().toChar()) {
            '<' -> return iterateXmlCards(pb) { readCardXML(ByteArrayInputStream(it.encodeToByteArray())) }
            '[', '{' -> return AutoJsonFormat.readCardList(pb.readBytes().decodeToString()).iterator()
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
                ze.name.endsWith(".json") -> m += AutoJsonFormat.readCardList(zi.bufferedReader().readText())
                ze.name.endsWith(".xml") -> m += readCardXML(zi)
                ze.name.endsWith(".mfc") -> m += mfcFormat.readCard(JavaStreamInput(zi))
            }
        }
        return m
    }

    companion object {
        private val xmlFormat = XmlCardFormat()

        // Used by android variant. Warning gets issued for jvmCli variant
        @Suppress("unused")
        fun parseString(xml: String): Card? = try {
            when (xml.find { it !in listOf('\n', '\r', '\t', ' ') }) {
                '<' -> xmlFormat.readCards(xml.byteInputStream()).let {
                    if (it.hasNext())
                        null
                    else
                        it.next()
                }
                '{', '[' -> AutoJsonFormat.readCard(xml)
                else -> null
            }
        } catch (ex: Exception) {
            Log.e("Card", "Failed to deserialize", ex)
            throw RuntimeException(ex)
        }
    }
}
