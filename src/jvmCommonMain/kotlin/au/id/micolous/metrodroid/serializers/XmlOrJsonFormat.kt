package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.*
import java.io.InputStream
import java.io.PushbackInputStream

class XmlCardFormat : CardMultiImporter {
    @OptIn(ExperimentalStdlibApi::class)
    override fun readCards(stream: InputStream): Iterator<Card>
        = readXmlCards(stream)
}

class XmlOrJsonCardFormat : CardMultiImporter {
    @OptIn(ExperimentalStdlibApi::class)
    override fun readCards(stream: InputStream): Iterator<Card>? {
        val pb = PushbackInputStream(stream)
        when (pb.peekAndSkipSpace().toInt().toChar()) {
            '<' -> return readXmlCards(pb)
            '[', '{' -> return AutoJsonFormat.readCardList(pb.readBytes().decodeToString()).iterator()
            'P' -> return readZip(pb)
            else -> return null
        }
    }

    private fun readZip(stream: InputStream): Iterator<Card> =
        ZipIterator(stream).map { (ze, zi) ->
            Log.d("Importer", "Importing ${ze.name}")
            when {
                ze.name.endsWith(".json") -> AutoJsonFormat.readCardList(
                    zi.bufferedReader().readText()
                ).iterator()
                ze.name.endsWith(".xml") -> readXmlCards(zi)
                ze.name.endsWith(".mfc") -> listOf(MfcCardImporter().readCard(JavaStreamInput(zi), TimestampFull(ze.time, MetroTimeZone.LOCAL))).iterator()
                else -> emptyList<Card>().iterator()
            }
        }.flatten()

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
