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

import androidx.annotation.VisibleForTesting
import au.id.micolous.metrodroid.MetrodroidApplication
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.TransitData
import java.util.*

actual object Preferences {
    const val PREF_LAST_READ_ID = "last_read_id"
    const val PREF_LAST_READ_AT = "last_read_at"
    private const val PREF_MFC_AUTHRETRY = "pref_mfc_authretry"
    private const val PREF_MFC_FALLBACK = "pref_mfc_fallback"
    private const val PREF_RETRIEVE_LEAP_KEYS = "pref_retrieve_leap_keys"

    private const val PREF_HIDE_CARD_NUMBERS = "pref_hide_card_numbers"
    private const val PREF_OBFUSCATE_TRIP_DATES = "pref_obfuscate_trip_dates"
    private const val PREF_OBFUSCATE_TRIP_TIMES = "pref_obfuscate_trip_times"
    private const val PREF_OBFUSCATE_TRIP_FARES = "pref_obfuscate_trip_fares"
    private const val PREF_OBFUSCATE_BALANCE = "pref_obfuscate_balance"
    private const val PREF_SHOW_HIDDEN_CARDS = "pref_show_hidden_cards"
    private const val PREF_HIDE_UNSUPPORTED_RIBBON = "pref_hide_unsupported_ribbon"
    private const val PREF_DEBUG_SPANS = "pref_debug_spans"

    private const val PREF_LOCALISE_PLACES = "pref_localise_places"
    private const val PREF_LOCALISE_PLACES_HELP = "pref_localise_places_help"
    private const val PREF_CONVERT_TIMEZONES = "pref_convert_timezones"
    private const val PREF_RAW_LEVEL = "pref_raw_level"
    const val PREF_THEME = "pref_theme"
    @VisibleForTesting
    const val PREF_SHOW_LOCAL_AND_ENGLISH = "pref_show_local_and_english"
    @VisibleForTesting
    const val PREF_SHOW_RAW_IDS = "pref_show_raw_ids"

    private const val PREF_MAP_TILE_URL = "pref_map_tile_url"
    private const val PREF_MAP_TILE_SUBDOMAINS = "pref_map_tile_subdomains"
    private const val PREF_MAP_TILELAYER_DOCS = "pref_map_tilelayer_docs"

    val PREFS_ANDROID_17 = arrayOf(PREF_MAP_TILE_SUBDOMAINS, PREF_MAP_TILE_URL, PREF_MAP_TILELAYER_DOCS)

    val PREFS_ANDROID_21 = arrayOf(PREF_LOCALISE_PLACES, PREF_LOCALISE_PLACES_HELP)

    @VisibleForTesting
    fun getSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.instance)
    }

    private fun getBooleanPref(preference: String, default_setting: Boolean): Boolean {
        return getSharedPreferences().getBoolean(preference, default_setting)
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
    actual val hideCardNumbers: Boolean
        get() = getBooleanPref(PREF_HIDE_CARD_NUMBERS, false)
    actual val obfuscateBalance: Boolean
        get() = getBooleanPref(PREF_OBFUSCATE_BALANCE, false)
    actual val obfuscateTripFares: Boolean
        get() = getBooleanPref(PREF_OBFUSCATE_TRIP_FARES, false)
    actual val showRawStationIds: Boolean
        get() = getBooleanPref(PREF_SHOW_RAW_IDS, false)
    actual val obfuscateTripDates: Boolean
        get() = getBooleanPref(PREF_OBFUSCATE_TRIP_DATES, false)
    actual val convertTimezone: Boolean
        get() = getBooleanPref(PREF_CONVERT_TIMEZONES, false)

    actual val obfuscateTripTimes
        get() = getBooleanPref(PREF_OBFUSCATE_TRIP_TIMES, false)

    val showHiddenCards
        get() = getBooleanPref(PREF_SHOW_HIDDEN_CARDS, false)
    val hideUnsupportedRibbon
        get() = getBooleanPref(PREF_HIDE_UNSUPPORTED_RIBBON, false)
    actual val localisePlaces
      get() = getBooleanPref(PREF_LOCALISE_PLACES, false)

    val speakBalance
      get() = getBooleanPref("pref_key_speak_balance", false)

    actual val showBothLocalAndEnglish
        get() = getBooleanPref(PREF_SHOW_LOCAL_AND_ENGLISH, false)

    actual val retrieveLeapKeys
        get() = getBooleanPref(PREF_RETRIEVE_LEAP_KEYS, false)

    actual val debugSpans
        get() = getBooleanPref(PREF_DEBUG_SPANS, false)

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

    actual val mfcFallbackReader get() = getStringPreference(PREF_MFC_FALLBACK, "null").toLowerCase(Locale.US)

    actual val language: String get() = Locale.getDefault().language

    actual val rawLevel: TransitData.RawLevel get() = TransitData.RawLevel.fromString(getStringPreference(PREF_RAW_LEVEL,
            TransitData.RawLevel.NONE.toString())) ?: TransitData.RawLevel.NONE
}
