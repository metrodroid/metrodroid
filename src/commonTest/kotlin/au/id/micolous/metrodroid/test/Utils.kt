/*
 * Utils.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.ui.ListItem
import kotlin.math.abs
import kotlin.test.*

internal fun assertNear(expected: Double, actual: Double, tol: Double) {
    if (abs(actual - expected) < tol)
        return
    // Fall to normal assertEquals so it shows everything
    assertEquals(expected, actual)
}

internal fun assertContainsListItem(
        expected: ListItem, actual: Iterable<ListItem>, message: String? = null) {
    assertNotNull(actual.firstOrNull{
        it == expected
    }, message)
}

internal fun assertNotContainsListItem(
        expected: ListItem, actual: Iterable<ListItem>, message: String? = null) {
    assertNull(actual.firstOrNull{
        it == expected
    }, message)
}
