/*
 * TripMapActivity.kt
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous@gmail.com>
 * Copyright 2018-2019 Google Inc.
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

package au.id.micolous.metrodroid.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import au.id.micolous.farebot.BuildConfig
import au.id.micolous.metrodroid.activity.TripMapActivity
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.Marker
import au.id.micolous.metrodroid.util.Preferences

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
// We only do it only on API >= 17 and we almost don't
// handle any user data in javascript. We use external reviewed code (Leaflet)
@Suppress("AddJavascriptInterface", "SetJavaScriptEnabled")
class TripMapFragment: Fragment() {
    private var mWebView: WebView? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mWebView?.destroy()
        val webView = WebView(requireContext())
        mWebView = webView
        webView.webChromeClient = WebChromeClient()
        return webView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val webView = view as WebView
        val trip = arguments?.getParcelable<Trip>(TripMapActivity.TRIP_EXTRA)
        if (trip == null) {
            Log.e(TAG, "Oops, couldn't display map, as we got a null trip!")
            return
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.allowUniversalAccessFromFileURLs = true

        settings.userAgentString = "${settings.userAgentString} metrodroid/${BuildConfig.VERSION_NAME}"

        val tileURL = Preferences.mapTileUrl
        val subdomains = Preferences.mapTileSubdomains

        Log.d(TAG, "TilesURL: $tileURL")

        Log.d(TAG, "Subdomains: $subdomains")


        val points = mutableListOf<Marker>()

        trip.startStation?.let {
            points.add(Marker(it, "start-marker"))
        }

        trip.endStation?.let {
            points.add(Marker(it, "end-marker"))
        }

        val shim = TripMapShim(points, tileURL, subdomains)

        webView.addJavascriptInterface(shim, "TripMapShim")

        webView.loadUrl("file:///android_asset/map.html")

        Log.d(TAG, "onViewCreated completed successfully")
    }

    override fun onPause() {
        super.onPause()
        mWebView?.onPause()
    }

    override fun onResume() {
        mWebView?.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        mWebView?.destroy()
        mWebView = null
        super.onDestroy()
    }

    class TripMapShim internal constructor(private val mMarkers: List<Marker>,
                                           @get:JavascriptInterface
                                           val tileUrl: String,
                                           @get:JavascriptInterface
                                           val subdomains: String) {

        // Lets build an interface where you can't pass arrays!
        val markerCount: Int
            @JavascriptInterface
            get() = this.mMarkers.size

        @JavascriptInterface
        fun getMarker(index: Int): Marker {
            return this.mMarkers[index]
        }
    }

    companion object {
        private const val TAG = "TripMapFragment"
    }
}
