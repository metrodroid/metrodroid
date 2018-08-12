/*
 * Station.java
 *
 * Copyright (C) 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.transit;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

public class Station implements Parcelable {
    public static final Creator<Station> CREATOR = new Creator<Station>() {
        public Station createFromParcel(Parcel parcel) {
            return new Station(parcel);
        }

        public Station[] newArray(int size) {
            return new Station[size];
        }
    };
    protected final String mCompanyName, mLineName, mStationName, mShortStationName, mLatitude, mLongitude, mLanguage;

    public Station(String stationName, String latitude, String longitude) {
        this(stationName, null, latitude, longitude);
    }

    public Station(String stationName, String shortStationName, String latitude, String longitude) {
        this(null, null, stationName, shortStationName, latitude, longitude);
    }

    public Station(String companyName, String lineName, String stationName, String shortStationName, String latitude, String longitude) {
        this(companyName, lineName, stationName, shortStationName, latitude, longitude, null);
    }

    public Station(String companyName, String lineName, String stationName, String shortStationName, String latitude, String longitude, String language) {
        mCompanyName = companyName;
        mLineName = lineName;
        mStationName = stationName;
        mShortStationName = shortStationName;
        mLatitude = latitude;
        mLongitude = longitude;
        mLanguage = language;
    }

    protected Station(Parcel parcel) {
        mCompanyName = parcel.readString();
        mLineName = parcel.readString();
        mStationName = parcel.readString();
        mShortStationName = parcel.readString();
        mLatitude = parcel.readString();
        mLongitude = parcel.readString();
        mLanguage = parcel.readString();
    }

    public String getStationName() {
        return mStationName;
    }

    public String getShortStationName() {
        return (mShortStationName != null) ? mShortStationName : mStationName;
    }

    public String getCompanyName() {
        return mCompanyName;
    }

    public String getLineName() {
        return mLineName;
    }

    public String getLatitude() {
        return mLatitude;
    }

    public String getLongitude() {
        return mLongitude;
    }

    /**
     * Language that the station line name and station name are written in. If null, then use
     * the system locale instead.
     *
     * https://developer.android.com/reference/java/util/Locale.html#forLanguageTag(java.lang.String)
     */
    public String getLanguage() {
        return mLanguage;
    }

    public boolean hasLocation() {
        return getLatitude() != null && !getLatitude().isEmpty()
                && getLongitude() != null && !getLongitude().isEmpty();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mCompanyName);
        parcel.writeString(mLineName);
        parcel.writeString(mStationName);
        parcel.writeString(mShortStationName);
        parcel.writeString(mLatitude);
        parcel.writeString(mLongitude);
        parcel.writeString(mLanguage);
    }

    public static Station unknown(String id) {
        return new Station(Utils.localizeString(R.string.unknown_format, id), null, null);
    }

    public static Station unknown(Integer id) {
        return new Station(Utils.localizeString(R.string.unknown_format,
                "0x" + Integer.toHexString(id)), null, null);
    }

    public static Station nameOnly(String name) {
        return new Station(name, null, null);
    }
}
