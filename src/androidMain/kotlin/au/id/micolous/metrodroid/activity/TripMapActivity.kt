/*
 * TripMapActivity.kt
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * Authors: Eric Butler, Michael Farrell, Vladimir Serbinenko
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

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import au.id.micolous.metrodroid.fragment.TripMapFragment
import au.id.micolous.metrodroid.transit.Trip

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class TripMapActivity : FragmentWrapperActivity() {

    override fun createFragment(): Fragment = TripMapFragment().also {
        val args = Bundle()
        args.putParcelable(TRIP_EXTRA, intent.getParcelableExtra<Trip>(TRIP_EXTRA))
        it.arguments = args
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        val trip = intent.getParcelableExtra<Trip>(TRIP_EXTRA)

        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        if (actionBar != null && trip != null) {
            actionBar.title = Trip.formatStationNames(trip)?.spanned
            val agencyName = trip.getAgencyName(false)
            val routeName = Trip.getRouteDisplayName(trip)
            actionBar.subtitle = when {
                routeName == null -> agencyName?.spanned
                agencyName == null -> routeName.spanned
                else -> (agencyName + " " + routeName).spanned
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

    companion object {
        const val TRIP_EXTRA = "trip"
    }
}
