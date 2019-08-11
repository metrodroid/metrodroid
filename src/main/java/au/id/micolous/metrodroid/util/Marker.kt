/*
 * Marker.kt
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util

import android.text.TextUtils
import android.webkit.JavascriptInterface

import au.id.micolous.metrodroid.transit.Station

/**
 * Markers for Leaflet to consume.
 */
class Marker(private val mStation: Station,
             @get:JavascriptInterface
             /**
              * Icon name to use for this marker.
              *
              * @return Icon name
              */
             val icon: String) {

    /**
     * Gets the WGS84 Latitude (Y) of the point represented by this marker, in decimal degrees.
     *
     * @return String representing the latitude of the point.
     */
    val lat: String
        @JavascriptInterface
        get() = this.mStation.latitude.toString()

    /**
     * Gets the WGS84 Longitude (X) of the point represented by this marker, in decimal degrees.
     *
     * @return String representing the longitude of the point.
     */
    val long: String
        @JavascriptInterface
        get() = this.mStation.longitude.toString()

    /**
     * Gets the HTML used to represent the contents of the pop-up info bubble, containing the
     * station name and the name of the company who runs the station.
     *
     * @return HTML
     */
    val html: String
        @JavascriptInterface
        get() {
            val station = TextUtils.htmlEncode(this.mStation.stationName?.unformatted ?: "")
            val company = TextUtils.htmlEncode(this.mStation.companyName?.unformatted ?: "")

            return "<b>$station</b><br>$company"
        }
}
