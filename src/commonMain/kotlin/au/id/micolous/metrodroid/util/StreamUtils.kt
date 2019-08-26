/*
 * StreamUtils.kt
 *
 * Copyright (C) 2019 Google
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

import kotlinx.io.ByteArrayOutputStream
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets

fun InputStream.fullRead() : ByteArray {
    val bo = ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024) { 0 }
    while(true) {
        val actualLen = this.read(buf, 0, buf.size)
        if (actualLen <= 0)
            break
        if (actualLen == buf.size)
            bo.write(buf)
        else
            bo.write(buf.sliceArray(0 until actualLen))
    }
    return bo.toByteArray()
}
fun InputStream.readToString() : String = kotlinx.io.core.String(
            bytes = this.fullRead(),
            charset = Charsets.UTF_8)
