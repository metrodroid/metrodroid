/*
 * TripMapActivity.java
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

import android.app.ActionBar
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewFragment

import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.TripFormatter
import au.id.micolous.metrodroid.util.Marker
import au.id.micolous.metrodroid.util.Preferences

import au.id.micolous.farebot.BuildConfig
import au.id.micolous.farebot.R

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class TripMapActivity : MetrodroidActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trip = intent.getParcelableExtra<Trip>(TRIP_EXTRA)
        if (trip == null) {
            // Probably passing around an unparcelable trip
            Log.d(TAG, "Oops, couldn't display map, as we got a null trip!")
            finish()
            return
        }

        setContentView(R.layout.activity_trip_map)

        val tileURL = Preferences.mapTileUrl
        val subdomains = Preferences.mapTileSubdomains

        Log.d(TAG, "TilesURL: $tileURL")

        Log.d(TAG, "Subdomains: $subdomains")

        val webView = (fragmentManager.findFragmentById(R.id.map) as WebViewFragment).webView
        webView.webChromeClient = WebChromeClient()

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowUniversalAccessFromFileURLs = true

        settings.userAgentString = "${settings.userAgentString} metrodroid/${BuildConfig.VERSION_NAME}"

        val actionBar = actionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.title = TripFormatter.formatStationNames(trip)
            val agencyName = trip.getAgencyName(false)
            val routeName = Trip.getRouteDisplayName(trip)
            actionBar.subtitle = if (routeName == null)
                agencyName
            else
                "$agencyName $routeName"
        }

        //int startMarkerId = R.drawable.marker_start;
        //int endMarkerId = R.drawable.marker_end;

        /* FIXME: Need icons...

        if (trip.getMode() == Trip.Mode.BUS) {
            startMarkerId = R.drawable.marker_bus_start;
            endMarkerId   = R.drawable.marker_bus_end;

        } else if (trip.getMode() == Trip.Mode.TRAIN) {
            startMarkerId = R.drawable.marker_train_start;
            endMarkerId   = R.drawable.marker_train_end;

        } else if (trip.getMode() == Trip.Mode.TRAM) {
            startMarkerId = R.drawable.marker_tram_start;
            endMarkerId   = R.drawable.marker_tram_end;

        } else if (trip.getMode() == Trip.Mode.METRO) {
            startMarkerId = R.drawable.marker_metro_start;
            endMarkerId   = R.drawable.marker_metro_end;

        } else if (trip.getMode() == Trip.Mode.FERRY) {
            startMarkerId = R.drawable.marker_ferry_start;
            endMarkerId   = R.drawable.marker_ferry_end;
        }
        */


        val points = ArrayList<Marker>()
        //LatLngBounds.Builder builder = LatLngBounds.builder();

        trip.startStation?.let {
            points.add(Marker(it, "start-marker"))
        }

        trip.endStation?.let {
            points.add(Marker(it, "end-marker"))
        }

        val shim = TripMapShim(points.toTypedArray(), tileURL, subdomains)

        webView.addJavascriptInterface(shim, "TripMapShim")

        webView.loadUrl("file:///android_asset/map.html")

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

    /*
        private LatLng addStationMarker(Station station, int iconId) {
            LatLng pos = new LatLng(Double.valueOf(station.getLatitude()), Double.valueOf(station.getLongitude()));
            mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(station.getStationName())
                .snippet(station.getCompanyName())
                .icon(BitmapDescriptorFactory.fromResource(iconId))
            );
            return pos;
        }
    */
    class TripMapShim internal constructor(private val mMarkers: Array<Marker>,
                                           @get:JavascriptInterface
                                           val tileUrl: String,
                                           @get:JavascriptInterface
                                           val subdomains: String) {

        // Lets build an interface where you can't pass arrays!
        val markerCount: Int
            @JavascriptInterface
            get() = this.mMarkers.size

        @JavascriptInterface
        fun getMarker(index: Int): Any {
            return this.mMarkers[index]
        }

        companion object {
            private const val TAG = "TripMapShim"
        }
    }

    companion object {
        const val TRIP_EXTRA = "trip"
        private val TAG = TripMapActivity::class.java.simpleName
    }
}
