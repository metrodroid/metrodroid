package com.codebutler.farebot.util;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.codebutler.farebot.transit.Station;

/**
 * Markers for Leaflet to consume.
 */
public class Marker {
    private final static String TAG = "Marker";
    Station mStation;
    int mIconResource;

    public Marker(Station station, int iconResource) {
        this.mStation = station;
        this.mIconResource = iconResource;
    }

    @JavascriptInterface
    public String getLat() {
        return this.mStation.getLatitude();
    }

    @JavascriptInterface
    public String getLong() {
        return this.mStation.getLongitude();
    }

    @JavascriptInterface
    public String getHTML() {
        String station = this.mStation.getStationName();
        String company = this.mStation.getCompanyName();

        if (station == null) {
            station = "";
        }

        if (company == null) {
            company = "";
        }

        station = TextUtils.htmlEncode(station);
        company = TextUtils.htmlEncode(company);

        return "<b>" + station + "</b><br>" + company;
    }

}
