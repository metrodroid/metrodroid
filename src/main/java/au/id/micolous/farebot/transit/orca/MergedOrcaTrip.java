/*
 * MergedOrcaTrip.java
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.farebot.transit.orca;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.transit.Station;
import au.id.micolous.farebot.transit.Trip;

public class MergedOrcaTrip extends Trip {
    public static final Creator<MergedOrcaTrip> CREATOR = new Creator<MergedOrcaTrip>() {
        public MergedOrcaTrip createFromParcel(Parcel parcel) {
            return new MergedOrcaTrip(
                    (OrcaTrip) parcel.readParcelable(OrcaTrip.class.getClassLoader()),
                    (OrcaTrip) parcel.readParcelable(OrcaTrip.class.getClassLoader())
            );
        }

        public MergedOrcaTrip[] newArray(int size) {
            return new MergedOrcaTrip[size];
        }
    };
    private final OrcaTrip mStartTrip;
    private final OrcaTrip mEndTrip;

    public MergedOrcaTrip(OrcaTrip startTrip, OrcaTrip endTrip) {
        mStartTrip = startTrip;
        mEndTrip = endTrip;
    }

    @Override
    public long getTimestamp() {
        return mStartTrip.getTimestamp();
    }

    @Override
    public long getExitTimestamp() {
        return mEndTrip.getTimestamp();
    }

    @Override
    public String getRouteName() {
        return mStartTrip.getRouteName();
    }

    @Override
    public String getAgencyName() {
        return mStartTrip.getAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return mStartTrip.getShortAgencyName();
    }

    @Override
    @Nullable
    public Integer getFare() {
        if (mEndTrip.mTransType == OrcaTransitData.TRANS_TYPE_CANCEL_TRIP) {
            // No fare applies to the trip, as the tap-on was reversed.
            return null;
        }

        return mStartTrip.getFare();
    }

    @Override
    public String getStartStationName() {
        return mStartTrip.getStartStationName();
    }

    @Override
    public Station getStartStation() {
        return mStartTrip.getStartStation();
    }

    @Override
    public String getEndStationName() {
        return mEndTrip.getStartStationName();
    }

    @Override
    public Station getEndStation() {
        return mEndTrip.getStartStation();
    }

    @Override
    public boolean hasFare() {
        return mStartTrip.hasFare();
    }

    @Override
    public Mode getMode() {
        return mStartTrip.getMode();
    }

    @Override
    public boolean hasTime() {
        return mStartTrip.hasTime();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        mStartTrip.writeToParcel(parcel, flags);
        mEndTrip.writeToParcel(parcel, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
