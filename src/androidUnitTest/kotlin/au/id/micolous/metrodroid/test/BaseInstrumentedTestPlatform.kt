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
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.JavaStreamInput
import au.id.micolous.metrodroid.util.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream

actual fun <T> runAsync(block: suspend () -> T) {
    runBlocking { block() }
}

actual fun loadAssetStream(path: String): InputStream? {
    val uri = BaseInstrumentedTest::class.java.getResource("/$path")?.toURI() ?: return null
    val file = File(uri)
    return file.inputStream()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
actual abstract class BaseInstrumentedTestPlatform {
    val context : Context
        get() = MetrodroidApplication.instance

    val isUnitTest get() = true

    actual fun setLocale(languageTag: String) {
        LocaleTools.setLocale(languageTag, context.resources)
    }

    /**
     * Sets a boolean preference.
     * @param preference Key to the preference
     * @param value Desired state of the preference.
     */
    private fun setBooleanPref(preference: String, value: Boolean) {
        val prefs = Preferences.getSharedPreferences()
        prefs.edit()
                .putBoolean(preference, value)
                .apply()
    }

    actual fun showRawStationIds(state: Boolean) {
        setBooleanPref(Preferences.PREF_SHOW_RAW_IDS, state)
    }

    actual fun showLocalAndEnglish(state: Boolean) {
        setBooleanPref(Preferences.PREF_SHOW_LOCAL_AND_ENGLISH, state)
    }

    actual fun loadAssetSafe(path: String) : Input? = loadAssetStream(path)?.let {
        JavaStreamInput(it)
    }

    actual fun listAsset(path: String) : List <String>? = null
}
