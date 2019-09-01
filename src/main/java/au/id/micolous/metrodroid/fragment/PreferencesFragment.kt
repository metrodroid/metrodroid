/*
 * PreferencesActivity.kt
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

package au.id.micolous.metrodroid.fragment

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import au.id.micolous.farebot.BuildConfig

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.activity.BackgroundTagActivity
import au.id.micolous.metrodroid.activity.MainActivity
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.ui.AlertDialogPreference
import au.id.micolous.metrodroid.ui.NumberPickerPreference
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.collatedBy
import java.util.*

class PreferencesFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private var mPreferenceLaunchFromBackground: CheckBoxPreference? = null
    private var mPreferenceTheme: ListPreference? = null
    private var mPreferenceLang: ListPreference? = null

    private var isLaunchFromBgEnabled: Boolean
        get() {
            val componentName = ComponentName(context!!, BackgroundTagActivity::class.java)
            val packageManager = context!!.packageManager
            val componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName)
            return componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        set(enabled) {
            val componentName = ComponentName(context!!, BackgroundTagActivity::class.java)
            val packageManager = context!!.packageManager
            val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
        }

    private fun nameLanguage(id: String): String {
        val locale = Utils.languageToLocale(id)
        if (id.contains('-'))
            return "${locale.displayLanguage} (${locale.displayCountry})"
        return locale.displayLanguage
    }
    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        setPreferencesFromResource(R.xml.prefs, s)

        mPreferenceLaunchFromBackground = findPreference("pref_launch_from_background") as? CheckBoxPreference
        mPreferenceLaunchFromBackground?.isChecked = isLaunchFromBgEnabled
        mPreferenceLaunchFromBackground?.onPreferenceChangeListener = this
        mPreferenceTheme = preferenceManager.findPreference(Preferences.PREF_THEME) as? ListPreference
        mPreferenceTheme?.onPreferenceChangeListener = this
        mPreferenceLang = preferenceManager.findPreference(Preferences.PREF_LANG_OVERRIDE) as? ListPreference
        mPreferenceTheme?.onPreferenceChangeListener = this
        val translations = BuildConfig.AVAILABLE_TRANSLATIONS.filter { it !in listOf("in", "iw") }.map { Pair(it, nameLanguage(it)) }.collatedBy { it.second }
        mPreferenceLang?.entryValues = arrayOf("") + translations.map { it.first }
        mPreferenceLang?.entries = arrayOf(Localizer.localizeString(R.string.lang_default)) + translations.map { it.second }
        mPreferenceLang?.setDefaultValue("")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            for (prefKey in Preferences.PREFS_ANDROID_17) {
                val pref = findPreference(prefKey) ?: continue
                pref.isEnabled = false
                pref.setSummary(R.string.requires_android_17)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            for (prefKey in Preferences.PREFS_ANDROID_21) {
                val pref = findPreference(prefKey) ?: continue
                pref.isEnabled = false
                pref.setSummary(R.string.requires_android_21)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(context, MainActivity::class.java)
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
            activity?.recreate()
            return true
        }
        return false
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val dialogFragment: DialogFragment
        when (preference) {
            is AlertDialogPreference -> dialogFragment = AlertDialogPreferenceFragment()
            is NumberPickerPreference -> dialogFragment = NumberPickerPreferenceFragment()
            else -> return super.onDisplayPreferenceDialog(preference)
        }
        val b = Bundle(1)
        b.putString("key", preference.key)
        dialogFragment.arguments = b
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(fragmentManager,
                "androidx.preference.PreferenceFragment.DIALOG")
    }
}
