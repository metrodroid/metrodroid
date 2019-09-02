/*
 * CursorIterator.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import android.database.Cursor

import java.io.Closeable
import java.util.NoSuchElementException

/**
 * CursorIterator gives [Iterator] semantics to [Cursor].
 *
 * Note: This doesn't guard calling [Cursor.moveToNext] or other similar methods that move the
 * [Cursor] position.
 */
class CursorIterator(private val mCursor: Cursor) : Iterator<Cursor>, Closeable {
    override fun close() = mCursor.close()
    override fun hasNext() = !mCursor.isLast

    override fun next(): Cursor {
        return if (mCursor.moveToNext()) {
            mCursor
        } else {
            throw NoSuchElementException()
        }
    }
}
