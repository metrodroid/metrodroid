/*
 * MetrodroidActivity.kt
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

package au.id.micolous.metrodroid.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils
import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

abstract class MetrodroidActivity : AppCompatActivity() {
    private var mAppliedTheme: Int = 0
    private var mAppliedLang: String = ""

    protected open val themeVariant: Int?
        get() = null

    override fun attachBaseContext(base: Context) {
        val locale = Utils.effectiveLocale()
        mAppliedLang = locale
        super.attachBaseContext(Utils.languageContext(base, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val variant = themeVariant
        val baseTheme = chooseTheme()
        val theme: Int
        mAppliedTheme = baseTheme
        if (variant != null) {
            val a = obtainStyledAttributes(
                    baseTheme,
                    intArrayOf(variant))

            theme = a.getResourceId(0, baseTheme)
            a.recycle()
        } else
            theme = baseTheme
        setTheme(theme)
        if (mAppliedLang != "")
            Utils.resetActivityTitle(this)
        installInsetsCallback()
        super.onCreate(savedInstanceState)
    }

    protected fun setDisplayHomeAsUpEnabled(b: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(b)
    }

    protected fun setHomeButtonEnabled(b: Boolean) {
        supportActionBar?.setHomeButtonEnabled(b)
    }

    override fun onResume() {
        super.onResume()

        if (chooseTheme() != mAppliedTheme || Utils.effectiveLocale() != mAppliedLang)
            recreate()
    }

    private fun insetsCallback(v: View)
    {
        val insets = ViewCompat.getRootWindowInsets(v)?.getInsets(
            WindowInsetsCompat.Type.systemBars()
        ) ?: return
        v.setPadding(v.paddingLeft,
            insets.top,
            v.paddingRight,
            insets.bottom
        )
    }

    private fun installInsetsCallback() {
        val main = findViewById<View>(android.R.id.content) ?: return
        insetsCallback(main)
        ViewCompat.setOnApplyWindowInsetsListener(main) {
            v, insets -> insetsCallback(v)
            insets
        }
    }

    companion object {
        fun chooseTheme(): Int = when (Preferences.themePreference) {
            "light" -> R.style.Metrodroid_Light
            "farebot" -> R.style.FareBot_Theme_Common
            else -> R.style.Metrodroid_Dark
        }
    }
}
