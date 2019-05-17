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

package au.id.micolous.metrodroid.activity;

import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewFragment;

import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.TripFormatter;
import au.id.micolous.metrodroid.util.Marker;
import au.id.micolous.metrodroid.util.Preferences;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.BuildConfig;
import au.id.micolous.farebot.R;

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TripMapActivity extends MetrodroidActivity {
    public static final String TRIP_EXTRA = "trip";
    private static final String TAG = TripMapActivity.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Trip trip = getIntent().getParcelableExtra(TRIP_EXTRA);
        if (trip == null) {
            // Probably passing around an unparcelable trip
            Log.d(TAG, "Oops, couldn't display map, as we got a null trip!");
            finish();
            return;
        }

        setContentView(R.layout.activity_trip_map);

        String tileURL = Preferences.INSTANCE.getMapTileUrl();
        String subdomains = Preferences.INSTANCE.getMapTileSubdomains();

        //noinspection StringConcatenation
        Log.d(TAG, "TilesURL: " + tileURL);
        //noinspection StringConcatenation
        Log.d(TAG, "Subdomains: " + subdomains);

        WebView webView = ((WebViewFragment) getFragmentManager().findFragmentById(R.id.map)).getWebView();
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        //noinspection StringConcatenation
        settings.setUserAgentString(settings.getUserAgentString() + " metrodroid/" + BuildConfig.VERSION_NAME);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(TripFormatter.INSTANCE.formatStationNames(trip));
            final String agencyName = trip.getAgencyName(false);
            final String routeName = Trip.Companion.getRouteDisplayName(trip);
            actionBar.setSubtitle(routeName == null ? agencyName
                    : String.format("%s %s", agencyName, routeName));
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


        final List<Marker> points = new ArrayList<>();
        //LatLngBounds.Builder builder = LatLngBounds.builder();

        if (trip.getStartStation() != null) {
            points.add(new Marker(trip.getStartStation(), "start-marker"));
        }

        if (trip.getEndStation() != null) {
            points.add(new Marker(trip.getEndStation(), "end-marker"));
        }

        TripMapShim shim = new TripMapShim(points.toArray(new Marker[0]), tileURL, subdomains);

        webView.addJavascriptInterface(shim, "TripMapShim");

        webView.loadUrl("file:///android_asset/map.html");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
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
    public static class TripMapShim {
        private static final String TAG = "TripMapShim";
        private final Marker[] mMarkers;
        private final String mTileUrl;
        private final String mSubdomains;

        TripMapShim(Marker[] markers, String tileUrl, String subdomains) {
            this.mMarkers = markers;
            this.mTileUrl = tileUrl;
            this.mSubdomains = subdomains;
        }

        // Lets build an interface where you can't pass arrays!
        @JavascriptInterface
        public int getMarkerCount() {
            return this.mMarkers.length;
        }

        @JavascriptInterface
        public Object getMarker(int index) {
            return this.mMarkers[index];
        }

        @JavascriptInterface
        public String getTileUrl() {
            return this.mTileUrl;
        }

        @JavascriptInterface
        public String getSubdomains() {
            return this.mSubdomains;
        }

    }

}
