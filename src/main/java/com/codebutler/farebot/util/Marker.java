/*
 * Marker.java
 *
 * Copyright (C) 2012 Eric Butler <eric@codebutler.com>
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
package com.codebutler.farebot.util;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.codebutler.farebot.transit.Station;

/**
 * Markers for Leaflet to consume.
 */
public class Marker {
    Station mStation;
    String mIcon;

    public Marker(Station station, String icon) {
        this.mStation = station;
        this.mIcon = icon;
    }

    /**
     * Gets the WGS84 Latitude (Y) of the point represented by this marker, in decimal degrees.
     *
     * @return String representing the latitude of the point.
     */
    @JavascriptInterface
    public String getLat() {
        return this.mStation.getLatitude();
    }

    /**
     * Gets the WGS84 Longitude (X) of the point represented by this marker, in decimal degrees.
     *
     * @return String representing the longitude of the point.
     */
    @JavascriptInterface
    public String getLong() {
        return this.mStation.getLongitude();
    }

    /**
     * Gets the HTML used to represent the contents of the pop-up info bubble, containing the
     * station name and the name of the company who runs the station.
     *
     * @return HTML
     */
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

    /**
     * Icon name to use for this marker.
     *
     * @return Icon name
     */
    @JavascriptInterface
    public String getIcon() {
        return mIcon;
    }

}
