/*
 * Preferences.kt
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

import au.id.micolous.metrodroid.multi.VisibleForTesting
import au.id.micolous.metrodroid.transit.TransitData
import platform.Foundation.*

actual object Preferences {
    private fun readBool(name: String) : Boolean = NSUserDefaults.standardUserDefaults.boolForKey(name)
    private fun readString(name: String) : String? = NSUserDefaults.standardUserDefaults.stringForKey(name)
    actual val obfuscateBalance: Boolean get() = readBool("pref_obfuscate_balance")
    actual val obfuscateTripFares: Boolean get() = readBool("pref_obfuscate_trip_fares")
    actual val hideCardNumbers: Boolean get() = readBool("pref_hide_card_numbers")
    actual val obfuscateTripDates: Boolean get() = readBool("pref_obfuscate_trip_dates")
    actual val convertTimezone: Boolean get() = readBool("pref_convert_timezones")
    actual val mfcFallbackReader: String get() = "" // useful only for reading really old dumps
    actual val mfcAuthRetry: Int get() = 5 // no MFC reader
    actual val retrieveLeapKeys: Boolean get() = false // no Leap key retriever
    actual val obfuscateTripTimes: Boolean get() = readBool("pref_obfuscate_trip_times")
    actual val debugSpans: Boolean get() = readBool("pref_debug_spans")
    actual val localisePlaces: Boolean get() = readBool("pref_localise_places")

    actual val showRawStationIds: Boolean
        get() = readBool("pref_show_raw_ids")
    actual val language: String
        get() = NSLocale.preferredLanguages[0] as String
    actual val showBothLocalAndEnglish: Boolean
        get() = readBool("pref_show_local_and_english")
    actual val rawLevel: TransitData.RawLevel
        get() = readString("pref_raw_level")?.let {
            TransitData.RawLevel.fromString(it) } ?: TransitData.RawLevel.NONE
    val speakBalance
        get() = readBool("pref_key_speak_balance")
}
