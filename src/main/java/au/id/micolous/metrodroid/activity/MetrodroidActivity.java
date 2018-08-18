/*
 * MetrodroidActivity.java
 *
 * Copyright 2018 Google
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

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import au.id.micolous.metrodroid.MetrodroidApplication;

public abstract class MetrodroidActivity extends AppCompatActivity {
    int mAppliedTheme;

    protected Integer getThemeVariant() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Integer variant = getThemeVariant();
        int baseTheme = MetrodroidApplication.chooseTheme();
        int theme;
        mAppliedTheme = baseTheme;
        if (variant != null) {
            TypedArray a = obtainStyledAttributes(
                    baseTheme,
                    new int[] { variant });

            theme = a.getResourceId(0, baseTheme);
            a.recycle();
        } else
            theme = baseTheme;
        setTheme(theme);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (MetrodroidApplication.chooseTheme() != mAppliedTheme)
            recreate();
    }

    @Deprecated
    @Nullable
    @Override
    public ActionBar getActionBar() {
        return null;
    }
}
