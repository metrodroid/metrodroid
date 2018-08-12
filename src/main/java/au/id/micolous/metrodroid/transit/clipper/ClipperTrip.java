/*
 * ClipperTrip.java
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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
package au.id.micolous.metrodroid.transit.clipper;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

import static au.id.micolous.metrodroid.transit.clipper.ClipperTransitData.CLIPPER_TZ;

public class ClipperTrip extends Trip {
    public static final Creator<ClipperTrip> CREATOR = new Creator<ClipperTrip>() {
        public ClipperTrip createFromParcel(Parcel parcel) {
            return new ClipperTrip(parcel);
        }

        public ClipperTrip[] newArray(int size) {
            return new ClipperTrip[size];
        }
    };
    protected final Calendar mTimestamp;
    private final Calendar mExitTimestamp;
    protected final int mFare;
    private final int mAgency;
    private final int mFrom;
    private final int mTo;
    private final int mRoute;

    public ClipperTrip(Calendar timestamp, Calendar exitTimestamp, int fare, int agency, int from, int to, int route) {
        // NOTE: All timestamps must be in CLIPPER_TZ.
        mTimestamp = timestamp;
        mExitTimestamp = exitTimestamp;
        mFare = fare;
        mAgency = agency;
        mFrom = from;
        mTo = to;
        mRoute = route;
    }

    ClipperTrip(Parcel parcel) {
        mTimestamp = Utils.longToCalendar(parcel.readLong(), CLIPPER_TZ);
        mExitTimestamp = Utils.longToCalendar(parcel.readLong(), CLIPPER_TZ);
        mFare = parcel.readInt();
        mAgency = parcel.readInt();
        mFrom = parcel.readInt();
        mTo = parcel.readInt();
        mRoute = parcel.readInt();
    }

    @Override
    public String getAgencyName() {
        return ClipperData.getAgencyName(mAgency);
    }

    @Override
    public String getShortAgencyName() {
        return ClipperData.getShortAgencyName(mAgency);
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTimestamp;
    }

    @Override
    public Calendar getEndTimestamp() {
        return mExitTimestamp;
    }

    @Override
    public String getRouteName() {
        if (mAgency == ClipperData.AGENCY_GG_FERRY) {
            return ClipperData.GG_FERRY_ROUTES.get(mRoute);
        } else {
            // FIXME: Need to find bus route #s
            // return "(Route 0x" + Long.toString(mRoute, 16) + ")";
            return null;
        }
    }

    @Override
    @Nullable
    public TransitCurrency getFare() {
        return TransitCurrency.USD(mFare);
    }

    @Override
    public Station getStartStation() {
        return ClipperData.getStation(mAgency, mFrom);
    }

    @Override
    public Station getEndStation() {
        return ClipperData.getStation(mAgency, mTo);
    }

    @Override
    public Mode getMode() {
        return ClipperData.getMode(mAgency);
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(Utils.calendarToLong(mTimestamp));
        parcel.writeLong(Utils.calendarToLong(mExitTimestamp));
        parcel.writeInt(mFare);
        parcel.writeInt(mAgency);
        parcel.writeInt(mFrom);
        parcel.writeInt(mTo);
        parcel.writeInt(mRoute);
    }

    public int describeContents() {
        return 0;
    }
}
