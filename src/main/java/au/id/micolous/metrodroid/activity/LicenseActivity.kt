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

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.io.InputStream

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData
import au.id.micolous.metrodroid.transit.tfi_leap.LeapTransitData
import au.id.micolous.metrodroid.util.Utils

class LicenseActivity : MetrodroidActivity() {

    private var lblLicenseText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        lblLicenseText = findViewById(R.id.lblLicenseText)
        lblLicenseText!!.beginBatchEdit()
        readLicenseTextFromAsset("Metrodroid-NOTICE.txt")
        readLicenseTextFromAsset("third_party/leaflet/LICENSE-prefix")
        readLicenseTextFromAsset("third_party/leaflet/LICENSE")
        readLicenseTextFromAsset("third_party/NOTICE.AOSP.txt")
        readLicenseTextFromAsset("third_party/NOTICE.noto-emoji.txt")
        readLicenseTextFromAsset("third_party/NOTICE.protobuf.txt")

        // TODO: Get a list of files programatically
        addNotice(SeqGoTransitData.notice)
        addNotice(LaxTapTransitData.notice)
        addNotice(ClipperTransitData.notice)
        addNotice(EZLinkTransitData.notice)
        addNotice(LeapTransitData.notice)

        lblLicenseText!!.endBatchEdit()
        lblLicenseText = null
    }

    private fun addNotice(notice: String?) {
        if (notice == null) return
        lblLicenseText!!.append(notice)
        lblLicenseText!!.append("\n\n")
    }

    private fun readLicenseTextFromAsset(path: String) {
        var s: InputStream? = null
        try {
            s = assets.open(path, AssetManager.ACCESS_RANDOM)
            val i = IOUtils.lineIterator(s!!, Utils.UTF8)

            while (i.hasNext()) {
                lblLicenseText!!.append(i.next())
                lblLicenseText!!.append("\n")
            }
        } catch (e: IOException) {

            Log.w(TAG, "Error reading license: $path", e)
            lblLicenseText!!.append("\n\n** Error reading license notice from $path\n\n")
        } finally {
            IOUtils.closeQuietly(s)
        }

        lblLicenseText!!.append("\n\n")
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
