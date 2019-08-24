/*
 * Log.kt
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

package au.id.micolous.metrodroid.multi

actual object Log {
    private fun out(severity: String, tag: String,
                    msg: String, exception: Throwable) {
        println("$tag: $severity: $msg: $exception")
    }
    private fun out(severity: String, tag: String, msg: String) {
        println("$tag: $severity: $msg")
    }
    actual fun d(tag: String, msg: String) {
        out("DEBUG", tag, msg)
    }
    actual fun e(tag: String, msg: String) {
        out("ERROR", tag, msg)
    }
    actual fun e(tag: String, msg: String, exception: Throwable) {
        out("ERROR", tag, msg, exception)
    }
    actual fun w(tag: String, msg: String) {
        out("WARN", tag, msg)
    }
    actual fun w(tag: String, msg: String, exception: Throwable) {
        out("WARN", tag, msg, exception)
    }
    actual fun d(tag: String, msg: String, exception: Throwable) {
        out("DEBUG", tag, msg, exception)
    }
    actual fun i(tag: String, msg: String) {
        out("INFO", tag, msg)
    }
    actual fun i(tag: String, msg: String, exception: Throwable) {
        out("INFO", tag, msg, exception)
    }
}
