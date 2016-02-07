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
    String mIcon;

    public Marker(Station station, String icon) {
        this.mStation = station;
        this.mIcon = icon;
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

    @JavascriptInterface
    public String getIcon() {
        return mIcon;
    }

}
