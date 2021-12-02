/*
 * UtilsJvmTest.kt
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

import au.id.micolous.metrodroid.util.getErrorMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsJvmTest : BaseInstrumentedTest() {
    class TestException(s: String): Exception(s)
    class TestLocalizedException(
        override val message: String?,
        private val localizedMsg: String?): Exception(message) {
        override fun getLocalizedMessage(): String? = localizedMsg
        override fun toString(): String = "default msg"
    }

    @Test
    fun testErrorMessage() {
        assertEquals("unknown error", getErrorMessage(null))
        assertEquals("TestException: test error", getErrorMessage(TestException("test error")))
        assertEquals("TestException: test error",
            getErrorMessage(Exception(TestException("test error"))))
        assertEquals(
            "TestLocalizedException: localized error",
            getErrorMessage(
                TestLocalizedException(
                    localizedMsg = "localized error",
                    message = "test error"
                )
            )
        )
        assertEquals(
            "TestLocalizedException: test error",
            getErrorMessage(
                TestLocalizedException(
                    localizedMsg = "",
                    message = "test error"
                )
            )
        )

        assertEquals(
            "TestLocalizedException: test error",
            getErrorMessage(
                TestLocalizedException(
                    localizedMsg = null,
                    message = "test error"
                )
            )
        )

        assertEquals(
            "TestLocalizedException: default msg",
            getErrorMessage(
                TestLocalizedException(
                    localizedMsg = null,
                    message = ""
                )
            )
        )

        assertEquals(
            "TestLocalizedException: default msg",
            getErrorMessage(
                TestLocalizedException(
                    localizedMsg = null,
                    message = null
                )
            )
        )
    }
}
