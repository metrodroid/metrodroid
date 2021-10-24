/*
 * Utils.kt
 *
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

package au.id.micolous.metrodroid.util

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toCValues
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

fun ByteArray.toNSData(): NSData = memScoped {
    toCValues()
            .ptr
            .let { NSData.dataWithBytes(it, size.toULong()) }
}

fun ImmutableByteArray.toNSData(): NSData = dataCopy.toNSData()

fun NSData.toByteArray(): ByteArray {
    val len = this.length.toInt() and 0x7fffffff
    if (len == 0)
       return ByteArray(0) { 0.toByte() }
    return this.bytes!!.readBytes(len)
}

fun NSData.toImmutable(): ImmutableByteArray = ImmutableByteArray.fromByteArray(this.toByteArray())

