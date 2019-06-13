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
import android.os.StrictMode
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

import au.id.micolous.farebot.R

class MetrodroidApplication : Application() {
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

    companion object {
        lateinit var instance: MetrodroidApplication
            private set
    }
}
