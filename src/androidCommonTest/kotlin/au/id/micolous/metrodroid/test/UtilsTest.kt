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
import android.os.Build
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
    }

    @Test
    @AndroidMinSdk(Build.VERSION_CODES.JELLY_BEAN_MR1) // deviceInfoStringEnglish needs 17
    fun testDeviceInfoEnglish() {
        setLocale("ru-RU")
        assertContains(other = "Version", charSequence = Utils.deviceInfoStringEnglish)
    }

    // This test doesn't work consistently; in old versions of Android, clipboard write access may
    // fail; in new versions we can't actually check that the data was copied.
    //
    // @Test
    // @Suppress("DEPRECATION")
    // fun testClipboard() {
    //     assert(Utils.copyTextToClipboard(context, "t1", "AAAA"))
    //
    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    //         // Can't read the clipboard from tests without input focus.
    //         return
    //     }
    //
    //     val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    //     assertEquals("AAAA", cm.text)
    //
    //     assert(Utils.copyTextToClipboard(context, "t2", "BBBB"))
    //     assertEquals("BBBB", cm.text)
    // }
}
