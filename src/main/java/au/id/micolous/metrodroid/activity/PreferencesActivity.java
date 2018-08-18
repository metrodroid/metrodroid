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

package au.id.micolous.metrodroid.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.ui.AppCompatPreferenceActivity;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public class PreferencesActivity extends AppCompatPreferenceActivity implements Preference.OnPreferenceChangeListener {

    private CheckBoxPreference mPreferenceLaunchFromBackground;
    private ListPreference mPreferenceTheme;

    public void onCreate(Bundle savedInstanceState) {
        setTheme(MetrodroidApplication.chooseTheme());
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mPreferenceLaunchFromBackground
                = (CheckBoxPreference) getPreferenceManager().findPreference("pref_launch_from_background");
        mPreferenceLaunchFromBackground.setChecked(isLaunchFromBgEnabled());
        mPreferenceLaunchFromBackground.setOnPreferenceChangeListener(this);
        mPreferenceTheme = (ListPreference) getPreferenceManager().findPreference(MetrodroidApplication.PREF_THEME);
        mPreferenceTheme.setOnPreferenceChangeListener(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            for (String prefKey : new String[]{
                    MetrodroidApplication.PREF_LOCALISE_PLACES,
                    MetrodroidApplication.PREF_LOCALISE_PLACES_HELP}) {
                Preference pref = getPreferenceManager().findPreference(prefKey);
                if (pref == null) continue;
                pref.setEnabled(false);
                pref.setSummary(R.string.requires_android_21);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPreferenceLaunchFromBackground) {
            setLaunchFromBgEnabled((Boolean) newValue);
            return true;
        }
        if (preference == mPreferenceTheme) {
            recreate();
            return true;
        }
        return false;
    }

    private boolean isLaunchFromBgEnabled() {
        ComponentName componentName = new ComponentName(this, BackgroundTagActivity.class);
        PackageManager packageManager = getPackageManager();
        int componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName);
        return componentEnabledSetting == COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void setLaunchFromBgEnabled(boolean enabled) {
        ComponentName componentName = new ComponentName(this, BackgroundTagActivity.class);
        PackageManager packageManager = getPackageManager();
        int newState = enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED;
        packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP);
    }
}
