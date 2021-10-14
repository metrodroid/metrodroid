/*
 * BaseInstrumentedTestPlatform.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.test

import android.content.Context
import android.content.res.AssetManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.JavaStreamInput
import au.id.micolous.metrodroid.util.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import java.io.DataInputStream
import java.io.InputStream

actual fun loadAssetStream(path: String): InputStream? =
    try {
        DataInputStream(
            InstrumentationRegistry.getInstrumentation().context.assets.open(path, AssetManager.ACCESS_RANDOM))
    } catch (e: Exception) {
        null
    }

@RunWith(AndroidJUnit4::class)
actual abstract class BaseInstrumentedTestPlatform {
    val context : Context
            get() = InstrumentationRegistry.getInstrumentation().context

    /**
     * Sets the Android and Java locales to a different language
     * and country. Does not clean up after execution, and should
     * only be used in tests.
     *
     * @param languageTag ITEF BCP-47 language tag string
     */
    actual fun setLocale(languageTag: String) {
        LocaleTools.setLocale(languageTag, context.resources)
    }

    /**
     * Sets the system language used by [MetrodroidApplication] resources.
     *
     * This is needed for things that call
     * [au.id.micolous.metrodroid.multi.Localizer.localizeString].
     *
     * @param languageTag ITEF BCP-47 language tag string
     */
    fun setAndroidLanguage(languageTag: String?) {
        val l = languageTag?.let { LocaleTools.compatLocaleForLanguageTag(it) }
        LocaleTools.setResourcesLocale(l, MetrodroidApplication.instance.resources)
    }


    actual fun loadAssetSafe(path: String) : Input? = loadAssetStream(path)?.let {
        JavaStreamInput(it)
    }

    actual fun listAsset(path: String) : List <String>? = context.assets.list(path)?.toList()

    val isUnitTest get() = false
}
