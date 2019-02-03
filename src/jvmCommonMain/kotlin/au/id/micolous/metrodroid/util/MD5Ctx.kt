/*
 * MD5Ctx.kt
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

@file:JvmName("MD5CtxActualKt")

package au.id.micolous.metrodroid.util

import java.security.MessageDigest

/**
 * Simple wrapper around Java's [MessageDigest] class to implement MD5.
 */
actual class MD5Ctx actual constructor() {
    private val md5 = MessageDigest.getInstance("MD5")!!

    /**
     * Plain update, updates this object
     */
    actual fun update(buffer: ImmutableByteArray, offset: Int, length: Int) {
        md5.update((when {
            offset == 0 && length == buffer.size -> buffer
            else -> buffer.sliceOffLen(offset, length)
        }).dataCopy)
    }

    /**
     * Returns array of bytes (16 bytes) representing hash as of the
     * current state of this object. Note: getting a hash does not
     * invalidate the hash object, it only creates a copy of the real
     * state which is finalized.
     *
     * @return    Array of 16 bytes, the hash of all updated bytes
     */
    actual fun digest(): ImmutableByteArray = md5.digest().toImmutable()
}