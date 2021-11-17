/*
 * ByteArrayInput.kt
 *
 * Copyright (C) 2021 Google
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

package au.id.micolous.metrodroid.util

import kotlin.math.min

class ByteArrayInput (private val ba: ByteArray): Input {
    private var offset: Int = 0

    val available get() = ba.size - offset

    private fun realRead(sz: Int): ByteArray {
        val off = offset
        offset += sz
        return ba.sliceArray(off until (off+sz))
    }

    override fun readBytes(sz: Int): ByteArray = realRead(
        min(sz, available))

    override fun readToString(): String = realRead(available).decodeToString()
}
