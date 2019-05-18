/*
 * PreferencesActivity.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.view.MenuItem

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.Preferences

class PreferencesActivity : PreferenceActivity(), Preference.OnPreferenceChangeListener {

    private var mPreferenceLaunchFromBackground: CheckBoxPreference? = null
    private var mPreferenceTheme: ListPreference? = null

    private var isLaunchFromBgEnabled: Boolean
        get() {
            val componentName = ComponentName(this, BackgroundTagActivity::class.java)
            val packageManager = packageManager
            val componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName)
            return componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        set(enabled) {
            val componentName = ComponentName(this, BackgroundTagActivity::class.java)
            val packageManager = packageManager
            val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MetrodroidActivity.chooseTheme())
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        mPreferenceLaunchFromBackground = preferenceManager.findPreference("pref_launch_from_background") as CheckBoxPreference
        mPreferenceLaunchFromBackground!!.isChecked = isLaunchFromBgEnabled
        mPreferenceLaunchFromBackground!!.onPreferenceChangeListener = this
        mPreferenceTheme = preferenceManager.findPreference(Preferences.PREF_THEME) as ListPreference?
        mPreferenceTheme?.onPreferenceChangeListener = this

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            for (prefKey in Preferences.PREFS_ANDROID_17) {
                val pref = preferenceManager.findPreference(prefKey) ?: continue
                pref.isEnabled = false
                pref.setSummary(R.string.requires_android_17)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            for (prefKey in Preferences.PREFS_ANDROID_21) {
                val pref = preferenceManager.findPreference(prefKey) ?: continue
                pref.isEnabled = false
                pref.setSummary(R.string.requires_android_21)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            return true
        }

        return false
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference === mPreferenceLaunchFromBackground) {
            isLaunchFromBgEnabled = newValue as Boolean
            return true
        }
        if (preference === mPreferenceTheme) {
            recreate()
            return true
        }
        return false
    }
}
