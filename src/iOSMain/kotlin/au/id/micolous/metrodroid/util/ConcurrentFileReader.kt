/*
 * ConcurrentFileReader.kt
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

import au.id.micolous.metrodroid.multi.Log
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.min


// Using mmap allows us to avoid having mutexes or to handle seek
// pointers
class ConcurrentFileReader private constructor(
        private val mFd: Int,
        private val mMapped: CPointer<ByteVar>,
        val fileLength: Long) {

    class FileInput constructor(
            private val reader: ConcurrentFileReader) : Input {
        private var offset: Long = 0
        private val count: Long get() = reader.fileLength

        val available get() = (count - offset)
        private val available2G get() = if (available < 0x7fffffff) available.toInt() else 0x7fffffff

        private fun realRead(sz: Int): ByteArray {
            val off = offset
            offset += sz
            return reader.read(off, sz)
        }

        override fun readBytes(sz: Int): ByteArray = realRead(
                min(sz, available2G))

        override fun readToString(): String = realRead(available2G).decodeToString()
    }

    fun makeInput(): Input = FileInput(this)

    fun read(off: Long, len: Int): ByteArray {
        val actualLen = minOf(len, (fileLength - off).toInt())
        if (actualLen <= 0)
            return byteArrayOf()
        return (mMapped + off)!!.readBytes(actualLen)
    }

    companion object {
        private const val TAG = "ConcurrentFileReader"

        fun getLength(fd: Int): Long {
            val ret = lseek(fd, 0, SEEK_END)
            lseek(fd, 0, SEEK_SET)
            return ret
        }

        fun openFile(path: String): ConcurrentFileReader? {
            Log.d(TAG, "Opening $path")
            val fd = open(path, O_RDONLY)
            if (fd < 0) {
                Log.w(TAG, "Error: $errno")
                return null
            }
            Log.d(TAG, "Success")
            val len = getLength(fd)
            val mapped = mmap(null, len.toULong(), PROT_READ, MAP_SHARED, fd, 0) ?: return null
            Log.d(TAG, "Mapped")
            return ConcurrentFileReader(fd, mapped.reinterpret<ByteVar>(), len)
        }
    }
}
