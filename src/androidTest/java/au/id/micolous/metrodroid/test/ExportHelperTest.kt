/*
 * ExportHelperTest.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.util.ExportHelper
import au.id.micolous.metrodroid.util.Preferences
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertTrue

class ExportHelperTest : BaseInstrumentedTest() {

    private fun testExportZip(locale: String, androidLanguage: String?, check : (String)->Boolean) {
        setLocale(locale)
        setAndroidLanguage(androidLanguage)

        val os = ByteArrayOutputStream()
        ExportHelper.exportCardsZip(os, context)

        // Now load the ZIP up again
        val zis = ZipInputStream(ByteArrayInputStream(os.toByteArray()))
        val readmeFilename = Localizer.localizeString(R.string.readme_filename) + "." + Preferences.language + ".txt"

        var hasReadme = false
        XmlOrJsonCardFormat.ZipIterator(zis).forEach {
            if (readmeFilename == it.first.name) {
                hasReadme = true

                // Read the file and see if there's a version code
                val b = it.second.readBytes()
                val content = b.toString(Charsets.UTF_8)
                assertTrue(check(content), "Content check failed, got: \"$content\"")
            }
        }

        assertTrue(hasReadme, "Needs $readmeFilename")
    }

    @Test
    fun testExportZipEnglish() {
        testExportZip("en-US", "en") {
            it.contains("version", true)
        }
    }

    @Test
    fun testExportZipRussian() {
        testExportZip("ru-RU", "ru") {
            it.contains("Версия", true)
        }
    }
}
