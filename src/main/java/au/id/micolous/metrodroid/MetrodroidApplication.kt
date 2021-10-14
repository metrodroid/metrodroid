/*
 * MetrodroidApplication.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils

class MetrodroidApplication : MultiDexApplication() {
    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
    }

    override fun attachBaseContext(base: Context) {
        // Do not use Preferences.langOverride as it relies on app context
        // and it has not been inited yet
        val prefs = PreferenceManager.getDefaultSharedPreferences(base)
        val v = prefs.getString(Preferences.PREF_LANG_OVERRIDE, "") ?: ""
        val locale = Utils.effectiveLocale(v)
        super.attachBaseContext(Utils.languageContext(base, locale))
    }

    companion object {
        lateinit var instance: MetrodroidApplication
            private set
    }
}
