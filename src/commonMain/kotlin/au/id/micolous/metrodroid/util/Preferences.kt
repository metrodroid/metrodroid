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

import au.id.micolous.metrodroid.transit.TransitData

expect object Preferences {
    val rawLevel: TransitData.RawLevel
    val obfuscateBalance: Boolean
    val obfuscateTripFares: Boolean
    val hideCardNumbers: Boolean
    var showRawStationIds:Boolean
    val obfuscateTripDates: Boolean
    val convertTimezone: Boolean
    val mfcFallbackReader: String
    val mfcAuthRetry: Int
    val retrieveLeapKeys: Boolean
    var showBothLocalAndEnglish: Boolean
    val language: String
    val region: String?
    val obfuscateTripTimes: Boolean
    val debugSpans: Boolean
    val localisePlaces: Boolean
    val metrodroidVersion: String
}
