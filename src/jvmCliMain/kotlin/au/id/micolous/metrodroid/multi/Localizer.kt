package au.id.micolous.metrodroid.multi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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

actual object Localizer : LocalizerInterface {
    override fun localizeString(res: StringResource, vararg v: Any?): String = res.english.format(*v)
    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString = FormattedString(res.english.format(*v))
    override fun localizeTts(res: StringResource, vararg v: Any?): FormattedString = FormattedString(stripTts(res.english).format(*v))
    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?) = if (count == 1) res.englishOne.format(*v) else res.englishMany.format(*v)
}
