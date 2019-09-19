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

package au.id.micolous.metrodroid.activity

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import au.id.micolous.metrodroid.fragment.PreferencesFragment

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Log

class PreferencesActivity : FragmentWrapperActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat?, pref: PreferenceScreen?): Boolean {
        Log.d("PreferencesActivity", "pref=$pref, key=${pref?.key}")
        val ft = supportFragmentManager.beginTransaction()
        val frag = PreferencesFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref?.key)
        frag.arguments = args
        ft.replace(R.id.content, frag, pref?.key)
        ft.addToBackStack(pref?.key)
        ft.commit()
        return true
    }

    override fun createFragment() = PreferencesFragment()
}
