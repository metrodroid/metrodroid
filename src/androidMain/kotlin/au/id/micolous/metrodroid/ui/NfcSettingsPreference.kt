/*
 * NfcSettingsPreference.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.preference.Preference
import au.id.micolous.metrodroid.util.Utils

// Used from XML
@Suppress("unused")
class NfcSettingsPreference : Preference {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onClick() {
        showNfcSettings(context)
    }

    companion object {
        private const val TAG = "NfcSettingsPreference"
        private const val ADVANCED_CONNECTED_DEVICE_SETTINGS =
                "com.android.settings.ADVANCED_CONNECTED_DEVICE_SETTINGS"

        fun showNfcSettings(context: Context) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                // Workaround for https://issuetracker.google.com/135970325
                // Only required for Android P
                if (Utils.tryStartActivity(context, ADVANCED_CONNECTED_DEVICE_SETTINGS))
                    return
            }

            // JB and later; we target JB+, so can skip the version check
            if (Utils.tryStartActivity(context, Settings.ACTION_NFC_SETTINGS))
                return

            // Fallback
            if (Utils.tryStartActivity(context, Settings.ACTION_WIRELESS_SETTINGS))
                return

            Log.w(TAG, "Failed to launch NFC settings")
        }
    }
}
