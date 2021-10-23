/*
 * Preferences.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.content.Context
import androidx.annotation.VisibleForTesting
import au.id.micolous.metrodroid.MetrodroidApplication
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import androidx.preference.PreferenceManager
import au.id.micolous.farebot.BuildConfig
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.TransitData
import kotlin.reflect.KProperty
import java.util.Locale

actual object Preferences {
    const val PREF_LAST_READ_ID = "last_read_id"
    const val PREF_LAST_READ_AT = "last_read_at"
    private const val PREF_MFC_AUTHRETRY = "pref_mfc_authretry"
    private const val PREF_MFC_FALLBACK = "pref_mfc_fallback"
    private const val PREF_FELICA_ONLY_FIRST = "pref_felica_only_first"
    private const val PREF_RETRIEVE_LEAP_KEYS = "pref_retrieve_leap_keys"

    private const val PREF_HIDE_CARD_NUMBERS = "pref_hide_card_numbers"
    private const val PREF_OBFUSCATE_TRIP_DATES = "pref_obfuscate_trip_dates"
    private const val PREF_OBFUSCATE_TRIP_TIMES = "pref_obfuscate_trip_times"
    private const val PREF_OBFUSCATE_TRIP_FARES = "pref_obfuscate_trip_fares"
    private const val PREF_OBFUSCATE_BALANCE = "pref_obfuscate_balance"
    private const val PREF_HIDE_UNSUPPORTED_RIBBON = "pref_hide_unsupported_ribbon"
    private const val PREF_DEBUG_SPANS = "pref_debug_spans"

    private const val PREF_LOCALISE_PLACES = "pref_localise_places"
    private const val PREF_LOCALISE_PLACES_HELP = "pref_localise_places_help"
    private const val PREF_CONVERT_TIMEZONES = "pref_convert_timezones"
    private const val PREF_RAW_LEVEL = "pref_raw_level"
    const val PREF_THEME = "pref_theme"
    const val PREF_LANG_OVERRIDE = "pref_lang_override"
    private const val PREF_SHOW_LOCAL_AND_ENGLISH = "pref_show_local_and_english"
    private const val PREF_SHOW_RAW_IDS = "pref_show_raw_ids"

    private const val PREF_MAP_TILE_URL = "pref_map_tile_url"
    private const val PREF_MAP_TILE_SUBDOMAINS = "pref_map_tile_subdomains"
    private const val PREF_MAP_TILELAYER_DOCS = "pref_map_tilelayer_docs"

    val PREFS_ANDROID_17 = arrayOf(PREF_MAP_TILE_SUBDOMAINS, PREF_MAP_TILE_URL,
            PREF_MAP_TILELAYER_DOCS, PREF_LANG_OVERRIDE)

    val PREFS_ANDROID_21 = arrayOf(PREF_LOCALISE_PLACES, PREF_LOCALISE_PLACES_HELP)

    private fun getSharedPreferences(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.instance)

    class BoolDelegate(private val preference: String, private val defaultSetting: Boolean) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
            getSharedPreferences().getBoolean(preference, defaultSetting)

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            getSharedPreferences().edit()
                .putBoolean(preference, value)
                .apply()
        }
    }

    /**
     * Gets a string preference.
     *
     * @param preference Preference key to fetch
     * @param defaultValue Default value of the preference
     * @param useDefaultForEmpty If True, when the preference contains an empty string, return
     * defaultValue.
     */
    private fun getStringPreference(preference: String, defaultValue: String, useDefaultForEmpty: Boolean = true): String {
        val v = getSharedPreferences().getString(preference, defaultValue) ?: defaultValue
        return if (useDefaultForEmpty && v.isEmpty()) {
            defaultValue
        } else v
    }

    private fun getIntPreference(preference: String, defaultValue: Int): Int {
        return getSharedPreferences().getInt(preference, defaultValue)
    }

    /**
     * Returns true if the user has opted to hide card numbers in the UI.
     *
     * @return true if we should not show any card numbers
     */
    actual val hideCardNumbers by BoolDelegate(PREF_HIDE_CARD_NUMBERS, false)
    actual val obfuscateBalance by BoolDelegate(PREF_OBFUSCATE_BALANCE, false)
    actual val obfuscateTripFares by BoolDelegate(PREF_OBFUSCATE_TRIP_FARES, false)
    actual var showRawStationIds by BoolDelegate(PREF_SHOW_RAW_IDS, false)
    actual val obfuscateTripDates by BoolDelegate(PREF_OBFUSCATE_TRIP_DATES, false)
    actual val convertTimezone by BoolDelegate(PREF_CONVERT_TIMEZONES, false)

    actual val obfuscateTripTimes by BoolDelegate(PREF_OBFUSCATE_TRIP_TIMES, false)

    val hideUnsupportedRibbon by BoolDelegate(PREF_HIDE_UNSUPPORTED_RIBBON, false)
    actual val localisePlaces by BoolDelegate(PREF_LOCALISE_PLACES, false)

    val speakBalance by BoolDelegate("pref_key_speak_balance", false)

    actual var showBothLocalAndEnglish by BoolDelegate(PREF_SHOW_LOCAL_AND_ENGLISH, false)

    actual var retrieveLeapKeys by BoolDelegate(PREF_RETRIEVE_LEAP_KEYS, false)

    actual val debugSpans by BoolDelegate(PREF_DEBUG_SPANS, false)

    val mapTileUrl: String
        get () {
            val def = Localizer.localizeString(R.string.default_map_tile_url)
            return getStringPreference(PREF_MAP_TILE_URL, def)
        }

    val mapTileSubdomains: String
        get() {
            val def = Localizer.localizeString(R.string.default_map_tile_subdomains)
            return getStringPreference(PREF_MAP_TILE_SUBDOMAINS, def)
        }

    actual val mfcAuthRetry get() = getIntPreference(PREF_MFC_AUTHRETRY, 5)

    val themePreference get() = getStringPreference(PREF_THEME, "dark")

    actual val mfcFallbackReader get() = getStringPreference(PREF_MFC_FALLBACK, "null").lowercase(Locale.US)

    actual val language: String get() = Locale.getDefault().language

    actual val region: String? get() {
        val tm = MetrodroidApplication.instance.getSystemService(Context.TELEPHONY_SERVICE)
        if (tm is TelephonyManager && (
                tm.phoneType == TelephonyManager.PHONE_TYPE_GSM ||
                tm.phoneType == TelephonyManager.PHONE_TYPE_CDMA)) {
            val netCountry = tm.networkCountryIso
            if (netCountry != null && netCountry.length == 2)
                return netCountry.uppercase(Locale.US)

            val simCountry = tm.simCountryIso
            if (simCountry != null && simCountry.length == 2)
                return simCountry.uppercase(Locale.US)
        }

        // Fall back to using the Locale settings
        return Locale.getDefault().country.uppercase(Locale.US)
    }

    actual val rawLevel: TransitData.RawLevel get() = TransitData.RawLevel.fromString(getStringPreference(PREF_RAW_LEVEL,
            TransitData.RawLevel.NONE.toString())) ?: TransitData.RawLevel.NONE

    val overrideLang get() = getStringPreference(PREF_LANG_OVERRIDE, "")

    val felicaOnlyFirst by BoolDelegate(PREF_FELICA_ONLY_FIRST, false)

    actual val metrodroidVersion: String
        get() = BuildConfig.VERSION_NAME
}
