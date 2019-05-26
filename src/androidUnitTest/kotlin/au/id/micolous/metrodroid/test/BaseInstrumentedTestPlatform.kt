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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.LocalizerInterface
import au.id.micolous.metrodroid.util.Preferences
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.InputStream
import java.io.File
import java.util.*
import kotlin.test.BeforeTest
import android.content.res.Resources

actual fun <T> runAsync(block: suspend () -> T) {
    runBlocking { block() }
}

actual abstract class BaseInstrumentedTestPlatform {
    @BeforeTest
    fun setUp() {
        Localizer.mock = object: LocalizerInterface {
            override fun localizeString(res: Int, vararg v: Any?): String = "{$res}"
            override fun localizePlural(res: Int, count: Int, vararg v: Any?): String = "{$res}"
        }
    }

    actual fun setLocale(languageTag: String) {}

    actual fun showRawStationIds(state: Boolean) {}

    actual fun showLocalAndEnglish(state: Boolean) {}

    actual fun loadAssetSafe(path: String) : InputStream? {
        val uri = BaseInstrumentedTest::class.java.getResource("/$path")?.toURI() ?: return null
        val file = File(uri)
        return file.inputStream()
    }

    actual fun listAsset(path: String) : List <String>? = null
}
