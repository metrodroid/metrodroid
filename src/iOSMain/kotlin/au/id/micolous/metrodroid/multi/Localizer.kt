/*
 * Localizer.kt
 *
 * Copyright 2019 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.multi

import au.id.micolous.metrodroid.util.Preferences
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import platform.Foundation.*

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

actual object Localizer: LocalizerInterface {
    // This is very basic but we have only formats of kind %s, %d, %x with possible positional argument
    private fun format(format: String, args: Array<out Any?>): String {
        if ('%' !in format)
            return format
        return FormattedString(format).format(*args).unformatted
    }
    val language
        get() = getResource("meta.lang", "en")
    private val bundle: NSBundle get() {
        val baseBundle = NSBundle.bundleWithIdentifier("au.id.micolous.metrodroid.metrolib") ?: NSBundle.mainBundle
        val overrideLang = Preferences.languageOverrideForTest.value ?: return baseBundle
        val path = baseBundle.pathForResource(overrideLang, ofType="lproj") ?: return baseBundle
        return NSBundle.bundleWithPath(path) ?: baseBundle
    }
    private fun getResource(key: String, def: String): String =
        bundle.localizedStringForKey(
            key, value=def, table="Metrolib")
    private fun getResource(res: StringResource): String = getResource("strings.${res.id}", res.english)
    private fun getPlural(res: PluralsResource, count: Int): String {            
        val def = if (count == 1) res.englishOne else res.englishMany
        val quantityString = Plurals.getQuantityString(language, count)
        val ret = getResource("plurals.$quantityString.${res.id}", def)
        Log.d("GetPlural", "${res.id}, $count -> $language, $quantityString -> $ret")
        return ret
    }
    override fun localizeFormatted(res: StringResource, vararg v: Any?): FormattedString = FormattedString(getResource(res)).format(*v)
    override fun localizeString(res: StringResource, vararg v: Any?): String = format(getResource(res), v)
    @Suppress("unused") // Used from Swift
    fun englishString(res: StringResource, vararg v: Any?): String = format(res.english, v)
    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?) = format(getPlural(res, count), v)
    override fun localizeTts(res: StringResource, vararg v: Any?): FormattedString = FormattedString(stripTts(getResource(res))).format(*v)
}
