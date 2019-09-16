/*
 * LicenseActivity.kt
 *
 * Copyright 2015-2018 Michael Farrell
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

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.transit.CardInfoRegistry
import kotlin.io.use

class LicenseActivity : MetrodroidActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        val lblLicenseText = findViewById<TextView>(R.id.lblLicenseText)
        lblLicenseText.beginBatchEdit()
        readLicenseTextFromAsset(lblLicenseText, "Metrodroid-NOTICE.txt")
        readLicenseTextFromAsset(lblLicenseText, "Logos-NOTICE.txt")
        readLicenseTextFromAsset(lblLicenseText, "third_party/leaflet/LICENSE-prefix")
        readLicenseTextFromAsset(lblLicenseText, "third_party/leaflet/LICENSE")
        readLicenseTextFromAsset(lblLicenseText, "third_party/NOTICE.AOSP.txt")
        readLicenseTextFromAsset(lblLicenseText, "third_party/NOTICE.noto-emoji.txt")
        readLicenseTextFromAsset(lblLicenseText, "third_party/NOTICE.protobuf.txt")

        for (factory in CardInfoRegistry.allFactories) {
                lblLicenseText.append(factory.notice ?: continue)
                lblLicenseText.append("\n\n")
        }

        lblLicenseText.endBatchEdit()
    }

    private fun readLicenseTextFromAsset(lblLicenseText: TextView, path: String) {
        try {
            assets.open(path, AssetManager.ACCESS_RANDOM).use { s->
                s.reader().forEachLine {
                    lblLicenseText.append(it)
                    lblLicenseText.append("\n")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading license: $path", e)
            lblLicenseText.append("\n\n** Error reading license notice from $path\n\n")
        }

        lblLicenseText.append("\n\n")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return false
    }

    companion object {
        private val TAG = LicenseActivity::class.java.simpleName
    }
}
