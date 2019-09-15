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

fun InputStream.fullRead(maxSize: Int? = null) : ByteArray {
    val bo = ByteArrayOutputStream() // TODO: preallocate the correct size once it's possible in kotlin-common
    val buf = ByteArray(64 * 1024) { 0 }
    var totalSize = 0
    while(true) {
        val actualLen = this.read(buf, 0, buf.size)
        if (actualLen <= 0)
            break
        bo.write(buf, 0, actualLen)
        totalSize += actualLen
        if (maxSize != null && totalSize > maxSize)
            break
    }
    return bo.toByteArray()
}
fun InputStream.readToString(maxSize: Int? = null) : String = kotlinx.io.core.String(
            bytes = this.fullRead(maxSize=maxSize),
            charset = Charsets.UTF_8)

fun InputStream.forEachLine(maxSize: Int? = null, function: (String) -> Unit) {
    this.readToString(maxSize=maxSize).split('\n', '\r').filter { it.isNotEmpty() }.forEach(function)
}