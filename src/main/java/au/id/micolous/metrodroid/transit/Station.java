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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.proto.Stations;
import au.id.micolous.metrodroid.util.StationTableReader;
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
    private final String mCompanyName, mStationName, mShortStationName, mLatitude, mLongitude, mLanguage;
    @Nullable
    private final List<String> mLineNames;
    private final boolean mIsUnknown;
    private final String mHumanReadableId;
    private final List<String> mAttributes;

    private Station(String humanReadableId, String stationName, boolean isUnknown) {
        this(humanReadableId, null, null, stationName,
                null, null, null, null, isUnknown);
    }

    private Station(String humanReadableId, String companyName, @Nullable List<String> lineName,
                    String stationName, String shortStationName, String latitude,
                    String longitude, String language, boolean isUnknown) {
        mHumanReadableId = humanReadableId;
        mCompanyName = companyName;
        mLineNames = lineName;
        mStationName = stationName;
        mShortStationName = shortStationName;
        mLatitude = latitude;
        mLongitude = longitude;
        mLanguage = language;
        mAttributes = new ArrayList<>();
        mIsUnknown = isUnknown;
    }

    private Station(Parcel parcel) {
        mCompanyName = parcel.readString();
        mLineNames = new ArrayList<>();
        parcel.readList(mLineNames, String.class.getClassLoader());
        mStationName = parcel.readString();
        mShortStationName = parcel.readString();
        mLatitude = parcel.readString();
        mLongitude = parcel.readString();
        mLanguage = parcel.readString();
        mIsUnknown = parcel.readInt() == 1;
        mHumanReadableId = parcel.readString();
        mAttributes = new ArrayList<>();
        parcel.readList(mAttributes, Station.class.getClassLoader());
    }

    public String getStationName() {
        String ret;
        if (mStationName == null)
            ret = Utils.localizeString(R.string.unknown_format, mHumanReadableId);
        else
            ret = mStationName;
        if (showRawId() && mStationName != null && !mStationName.equals(mHumanReadableId))
            ret = String.format(Locale.ENGLISH, "%s [%s]", ret, mHumanReadableId);
        if (!mAttributes.isEmpty())
            for (String attribute : mAttributes)
                ret = String.format(Locale.ENGLISH, "%s, %s", ret, attribute);
        return ret;
    }

    private boolean showRawId() {
        return MetrodroidApplication.showRawStationIds();
    }

    public String getShortStationName() {
        String ret;
        if (mStationName == null && mShortStationName == null)
            ret = Utils.localizeString(R.string.unknown_format, mHumanReadableId);
        else
            ret = (mShortStationName != null) ? mShortStationName : mStationName;
        if (showRawId() && mStationName != null && !mStationName.equals(mHumanReadableId))
            ret = String.format(Locale.ENGLISH, "%s [%s]", ret, mHumanReadableId);
        if (!mAttributes.isEmpty())
            for (String attribute : mAttributes)
                ret = String.format(Locale.ENGLISH, "%s, %s", ret, attribute);
        return ret;
    }

    public String getCompanyName() {
        return mCompanyName;
    }

    /**
     * Return a list of candidate line names for a station. In the case that a station ID is used
     * on more than one line, then there should be multiple lines returned.
     *
     * If there is no line information available, this returns an empty list.
     */
    @NonNull
    public List<String> getLineNames() {
        if (mLineNames != null) {
            return mLineNames;
        } else {
            return Collections.emptyList();
        }
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
        parcel.writeList(mLineNames);
        parcel.writeString(mStationName);
        parcel.writeString(mShortStationName);
        parcel.writeString(mLatitude);
        parcel.writeString(mLongitude);
        parcel.writeString(mLanguage);
        parcel.writeInt(mIsUnknown ? 1 : 0);
        parcel.writeString(mHumanReadableId);
        parcel.writeList(mAttributes);
    }

    public boolean isUnknown() {
        return mIsUnknown;
    }

    public static Station unknown(String id) {
        return new Station(id, null, true);
    }

    public static Station unknown(Integer id) {
        return unknown("0x" + Integer.toHexString(id));
    }

    public static Station nameOnly(String name) {
        return new Station(name, name, false);
    }

    public Station addAttribute(String s) {
        mAttributes.add(s);
        return this;
    }

    public static Station fromProto(String humanReadableID, Stations.Station ps,
                                    Stations.Operator po, Collection<Stations.Line> pl,
                                    String ttsHintLanguage, StationTableReader str) {
        boolean hasLocation = ps.getLatitude() != 0 && ps.getLongitude() != 0;

        List<String> stations = null;
        if (pl != null) {
            stations = new ArrayList<>();
            for (Stations.Line l : pl) {
                stations.add(str.selectBestName(l.getName(), true));
            }
        }

        return new Station(
                humanReadableID,
                po == null ? null : str.selectBestName(po.getName(), true),
                stations,
                str.selectBestName(ps.getName(), false),
                str.selectBestName(ps.getName(), true),
                hasLocation ? Float.toString(ps.getLatitude()) : null,
                hasLocation ? Float.toString(ps.getLongitude()) : null,
                ttsHintLanguage, false);
    }
}
