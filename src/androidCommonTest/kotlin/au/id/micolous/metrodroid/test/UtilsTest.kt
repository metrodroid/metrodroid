/*
 * UtilsTest.kt
 *
 * Copyright 2021 Google
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

import android.content.ClipboardManager
import android.content.Context
import au.id.micolous.metrodroid.util.Utils
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class UtilsTest : BaseInstrumentedTest() {
    @Test
    fun testDeviceInfo() {
        setLocale("en-US")
        assertContains(other = "Version", charSequence = Utils.deviceInfoString)
        assertContains(other = "Version", charSequence = Utils.deviceInfoStringEnglish)
        setLocale("ru-RU")
        assertContains(other = "Версия", charSequence = Utils.deviceInfoString)
        assertContains(other = "Version", charSequence = Utils.deviceInfoStringEnglish)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testClipboard() {
        Utils.copyTextToClipboard(context, "t1", "AAAA")
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("AAAA", cm.text)
        Utils.copyTextToClipboard(context, "t2", "BBBB")
        assertEquals("BBBB", cm.text)
    }
}
