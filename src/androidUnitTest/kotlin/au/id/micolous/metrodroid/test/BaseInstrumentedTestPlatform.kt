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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream

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

    actual fun setLocale(languageTag: String) {
        LocaleTools.setLocale(languageTag, context.resources)
    }

    actual fun loadAssetSafe(path: String) : Input? = loadAssetStream(path)?.let {
        JavaStreamInput(it)
    }

    actual fun listAsset(path: String) : List <String>? = null
}
