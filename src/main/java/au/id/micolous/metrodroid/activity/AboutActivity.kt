/*
 * AboutActivity.kt
 *
 * Copyright 2015-2016 Michael Farrell
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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.pm.PackageInfoCompat

import au.id.micolous.metrodroid.util.Utils

import au.id.micolous.farebot.R

/**
 * @author Michael Farrell
 */
class AboutActivity : MetrodroidActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setDisplayHomeAsUpEnabled(true)

        this.findViewById<TextView>(R.id.lblDebugText).text = Utils.deviceInfoString
    }

    @Suppress("UNUSED_PARAMETER")
    fun onWebsiteClick(view: View) {
        val b = Uri.parse("https://micolous.github.io/metrodroid/").buildUpon()
        val version: Long = try {
            PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        } catch (ex: PackageManager.NameNotFoundException) {
            // Shouldn't hit this...
            -1
        }

        // Pass the version number to the website.
        // This allows the website to have a hook showing if the user's version is out of date
        // and flag specifically which cards *won't* be supported (or have problems).
        b.appendQueryParameter("ver", version.toString())
        startActivity(Intent(Intent.ACTION_VIEW, b.build()))
    }

    @Suppress("UNUSED_PARAMETER")
    fun onLicenseClick(view: View) {
        startActivity(Intent(this, LicenseActivity::class.java))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return false
    }
}
