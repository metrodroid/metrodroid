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

import kotlinx.io.InputStream
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.comparisons.minOf

// Using mmap allows us to avoid having mutexes or to handle seek
// pointers
class ConcurrentFileReader private constructor(
    private val mFd: Int,
    private val mMapped: CPointer<ByteVar>,
    val fileLength: Long) {

    // Class based on kotlinx code
    class FileInputStream constructor(
        private val reader: ConcurrentFileReader): InputStream() {

            private var pos: Long = 0
            private var mark: Long = 0
            private val count: Long get() = reader.fileLength

            override fun available(): Int = (count - pos).toInt()

            override fun read(): Int = if (pos < count) reader.read(pos++, 1)[0].toInt() and 0xFF else -1

            override fun read(b: ByteArray, offset: Int, len: Int): Int {
                // avoid int overflow
                if (offset < 0 || offset > b.size || len < 0
                || len > b.size - offset) {
                    throw IndexOutOfBoundsException()
                }
                // Are there any bytes available?
                if (this.pos >= this.count) {
                    return -1
                }
                if (len == 0) {
                    return 0
                }

                val copylen = minOf(this.count - pos, len.toLong())
                reader.read(pos, copylen.toInt()).copyInto(b, offset)
                pos += copylen
                return copylen.toInt()
            }

            override fun skip(n: Long): Long {
                if (n <= 0) {
                    return 0
                }
                val temp = pos
                pos = if (this.count - pos < n) this.count else (pos + n)
                return (pos - temp)
            }
        }

    fun makeInputStream(): InputStream = FileInputStream(this)

    fun read(off: Long, len: Int): ByteArray {
        val actualLen = minOf(len, (fileLength - off).toInt())
        if (actualLen <= 0)
            return byteArrayOf()
        return (mMapped + off)!!.readBytes(actualLen)
    }

    companion object {
        private const val TAG = "ConcurrentFileReader"

        fun getLength(fd: Int): Long {
            val ret = lseek (fd, 0, SEEK_END)
            lseek (fd, 0, SEEK_SET)
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
