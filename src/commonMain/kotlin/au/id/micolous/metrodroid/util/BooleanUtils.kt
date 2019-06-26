/*
 * BooleanUtils.kt
 *
 * Copyright (C) 2019 Michael Farrell <micolous+git@gmail.com>
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

/**
 * [ifTrue] returns [lazyValue] if [condition] is true, or null otherwise.
 *
 * [lazyValue] itself may be nullable.
 *
 * This is similar to [takeIf], but will not evaluate the closure in [lazyValue] if [condition]
 * is false.
 */
inline fun <T>Boolean.ifTrue(lazyValue: () -> T?) : T? {
    return when {
        this -> lazyValue()
        else -> null
    }
}

/**
 * [ifFalse] returns [lazyValue] if [condition] is false, or null otherwise.
 *
 * [lazyValue] itself may be nullable.
 *
 * This is similar to [takeUnless], but will not evaluate the closure in [lazyValue] if [condition]
 * is true.
 */
inline fun <T>Boolean.ifFalse(lazyValue: () -> T?) : T? {
    return when {
        this -> null
        else -> lazyValue()
    }
}

