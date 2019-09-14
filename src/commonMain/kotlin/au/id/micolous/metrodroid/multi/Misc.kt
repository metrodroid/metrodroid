/*
 * Misc.kt
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

package au.id.micolous.metrodroid.multi

expect annotation class VisibleForTesting()
expect annotation class Parcelize()
expect interface Parcelable
@Target(AnnotationTarget.FUNCTION)
expect annotation class NativeThrows (vararg val exceptionClasses: kotlin.reflect.KClass<out kotlin.Throwable>)
// Swift doesn't propagate RuntimeException, hence we need this ugly wrapper
fun <T> logAndSwiftWrap(tag: String, msg: String, f: () -> T): T {
    try {
        return f()
    } catch (ex: Exception) {
        Log.e(tag, msg, ex)
        throw Exception(ex)
    }
}