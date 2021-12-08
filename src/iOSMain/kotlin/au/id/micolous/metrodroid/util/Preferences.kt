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
import platform.CoreTelephony.CTCarrier
import platform.CoreTelephony.CTTelephonyNetworkInfo
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
import kotlin.reflect.KProperty

actual object Preferences {
    private fun readString(name: String) : String? = NSUserDefaults.standardUserDefaults.stringForKey(name)

    class BoolDelegate(private val name: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
                NSUserDefaults.standardUserDefaults.boolForKey(name)
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            NSUserDefaults.standardUserDefaults.setBool(value, forKey = name)
        }
    }
    actual var obfuscateBalance by BoolDelegate("pref_obfuscate_balance")
    actual var obfuscateTripFares by BoolDelegate("pref_obfuscate_trip_fares")
    actual var hideCardNumbers by BoolDelegate("pref_hide_card_numbers")
    actual var obfuscateTripDates by BoolDelegate("pref_obfuscate_trip_dates")
    actual val convertTimezone by BoolDelegate("pref_convert_timezones")
    actual val mfcFallbackReader: String get() = "" // useful only for reading really old dumps
    actual val mfcAuthRetry: Int get() = 5 // no MFC reader
    actual var retrieveLeapKeys by BoolDelegate("pref_retrieve_leap_keys")
    actual var obfuscateTripTimes by BoolDelegate("pref_obfuscate_trip_times")
    actual val debugSpans by BoolDelegate("pref_debug_spans")
    actual val localisePlaces by BoolDelegate("pref_localise_places")

    actual val language: String
        get() = languageOverrideForTest.value ?: NSLocale.preferredLanguages[0] as String
    actual val regions: Set<String>?
        get() {
            val over = regionOverrideForTest.value
            if (over != null)
                return setOf(over)
            val tm = CTTelephonyNetworkInfo()
            return (tm.serviceSubscriberCellularProviders
                ?.values
                ?.filterIsInstance<CTCarrier>()?.mapNotNull { it.isoCountryCode?.uppercase() }
                ?.toSet().orEmpty()
                    + setOfNotNull(tm.subscriberCellularProvider?.isoCountryCode?.uppercase())
                    + setOfNotNull(currentLocale.countryCode)).ifEmpty { null }
        }
    val languageOverrideForTest = AtomicReference<String?>(null)
    val currentLocale: NSLocale get() = localeOverrideForTest.value ?: NSLocale.currentLocale
    val localeOverrideForTest = AtomicReference<NSLocale?>(null)
    val regionOverrideForTest = AtomicReference<String?>(null)

    actual var showRawStationIds by BoolDelegate("pref_show_raw_ids")
    actual var showBothLocalAndEnglish by BoolDelegate("pref_show_local_and_english")
    actual val rawLevel: TransitData.RawLevel
        get() = readString("pref_raw_level")?.let {
            TransitData.RawLevel.fromString(it) } ?: TransitData.RawLevel.NONE
    @Suppress("unused") // Used from Swift
    val speakBalance by BoolDelegate("pref_key_speak_balance")
    actual val metrodroidVersion: String
        get() = (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "unknown"

    private val useIsoDateTimeStampsBacker = AtomicInt(0)
    actual var useIsoDateTimeStamps: Boolean
        get() = useIsoDateTimeStampsBacker.value != 0
        set(value) {
            useIsoDateTimeStampsBacker.value = if (value) 1 else 0
        }
}
