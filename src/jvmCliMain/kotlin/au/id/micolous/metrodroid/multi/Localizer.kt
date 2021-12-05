package au.id.micolous.metrodroid.multi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import au.id.micolous.metrodroid.serializers.NodeWrapper
import au.id.micolous.metrodroid.serializers.NodeWrapperImpl
import java.io.InputStream
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

actual data class StringResource(val id:String, val english: String)
actual data class DrawableResource(val id:String)
actual data class PluralsResource(val id: String, val englishOne: String, val englishMany: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = StringResource::class)
actual class StringResourceSerializer : KSerializer<StringResource> {
    override fun deserialize(decoder: Decoder): StringResource {
        val id = String.serializer().deserialize(decoder)
        return Rmap.strings[id]!!
    }

    override fun serialize(encoder: Encoder, value: StringResource) {
        String.serializer().serialize(encoder, value.id)
    }
}

object DefaultLocalizer: LocalizerInterface {
    override fun localizeString(res: StringResource, vararg v: Any?): String = res.english.format(*v)
    override fun localizeTts(res: StringResource, vararg v: Any?): FormattedString = FormattedString(stripTts(res.english).format(*v))
    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?) = if (count == 1) res.englishOne.format(*v) else res.englishMany.format(*v)
}

class XmlLocalizer private constructor(
    private val strings: Map<String, String>,
    private val plurals: Map<String, Map<String, String>>,
    private val lang: String
): LocalizerInterfaceSkippable {
    override fun localizeString(res: StringResource, vararg v: Any?): String? =
        strings[res.id]?.format(*v)

    override fun localizeTts(res: StringResource, vararg v: Any?): FormattedString? =
        strings[res.id]?.let { FormattedString(stripTts(it).format(*v)) }

    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?): String? {
        val look = plurals[res.id] ?: return null
        val plur = Plurals.getQuantityString(lang, count)
        val lookplur = look[plur] ?: look.values.firstOrNull() ?: return null
        return lookplur.format(*v)
    }

    companion object {
        private fun loadXml(stream: InputStream, lang: String): XmlLocalizer? {
            val dbFactory = DocumentBuilderFactory.newInstance()
            dbFactory.isNamespaceAware = true
            dbFactory.isValidating = false
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(stream)
            val node = NodeWrapperImpl(doc.documentElement)
            if (node.nodeName != "resources")
                return null
            val strings = parseList(node, "string", "name")
            val plurals = node.childNodes.filter { it.nodeName == "plurals" }.mapNotNull {
                parsePlural(it)
            }.toMap()
            return XmlLocalizer(strings=strings, plurals=plurals, lang=lang)
        }

        private fun parsePlural(node: NodeWrapper): Pair<String, Map<String, String>>? {
            return (node.attributes["name"] ?: return null) to parseList(node, "item", "quantity")
        }

        private fun parseList(node: NodeWrapper, elName: String, nameAttr: String) =
            node.childNodes.filter { it.nodeName == elName }.mapNotNull {
                val name = it.attributes[nameAttr] ?: return@mapNotNull null
                val value = it.inner ?: return@mapNotNull null
                name to value
            }.toMap()

        fun loadRes(id: String, lang: String): XmlLocalizer? {
            val stream = XmlLocalizer::class.java.classLoader?.getResourceAsStream(
                "values-$id/strings.xml") ?: return null
            return loadXml(stream, lang)
        }
    }
}

actual object Localizer: LocalizerInterface, LocalizeFallbacker(emptyList(), DefaultLocalizer) {
    private fun loadLocale(lang: String, region: String) {
        plist = listOf("$lang-r$region", lang).mapNotNull { XmlLocalizer.loadRes(it, lang) }
    }

    private fun loadLocale(locale: Locale) {
        loadLocale(locale.language, locale.country)
    }

    fun loadDefaultLocale() {
        loadLocale(Locale.getDefault())
    }
}
