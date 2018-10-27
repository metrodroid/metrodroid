/*
 * KievTrip.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.kiev;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class KievTrip extends Trip {
    private final Calendar mTimestamp;
    private final String mTransactionType;
    private final int mCounter1;
    private final int mCounter2;
    private final int mValidator;
    private final static TimeZone TZ = TimeZone.getTimeZone("Europe/Kiev");

    KievTrip(byte []data) {
        mTimestamp = parseTimestamp(data);
        // This is a shameless plug. We have no idea which field
        // means what. But metro transport is always 84/04/40/53
        mTransactionType = Utils.getHexString(data, 0, 1)
                + "/" + Utils.getHexString(data, 6, 1)
                + "/" + Utils.getHexString(data, 8, 1)
                + "/" + Integer.toHexString(Utils.getBitsFromBuffer(data, 88, 10));
        mValidator = Utils.getBitsFromBuffer(data, 56, 8);
        mCounter1 = Utils.getBitsFromBuffer(data, 72, 16);
        mCounter2 = Utils.getBitsFromBuffer(data, 98, 16);
    }

    private static Calendar parseTimestamp(byte[] data) {
        Calendar c = new GregorianCalendar(TZ);
        c.set(Utils.getBitsFromBuffer(data, 17, 5) + 2000,
                Utils.getBitsFromBuffer(data, 13, 4) - 1,
                Utils.getBitsFromBuffer(data, 8, 5),
                Utils.getBitsFromBuffer(data, 33, 5),
                Utils.getBitsFromBuffer(data, 27, 6),
                Utils.getBitsFromBuffer(data, 22, 5));
        return c;
    }

    @Nullable
    @Override
    public Station getStartStation() {
        return Station.unknown(mValidator);
    }

    private KievTrip(Parcel in) {
        mTimestamp = Utils.unparcelCalendar(in);
        mCounter1 = in.readInt();
        mCounter2 = in.readInt();
        mValidator = in.readInt();
        mTransactionType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Utils.parcelCalendar(dest, mTimestamp);
        dest.writeInt(mCounter1);
        dest.writeInt(mCounter2);
        dest.writeInt(mValidator);
        dest.writeString(mTransactionType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<KievTrip> CREATOR = new Creator<KievTrip>() {
        @Override
        public KievTrip createFromParcel(Parcel in) {
            return new KievTrip(in);
        }

        @Override
        public KievTrip[] newArray(int size) {
            return new KievTrip[size];
        }
    };

    @Override
    public Calendar getStartTimestamp() {
        return mTimestamp;
    }

    @Nullable
    @Override
    public String getAgencyName(boolean isShort) {
        if (mTransactionType.equals("84/04/40/53"))
            return Utils.localizeString(R.string.mode_metro);
        return mTransactionType;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return null;
    }

    @Override
    public Mode getMode() {
        if (mTransactionType.equals("84/04/40/53"))
            return Mode.METRO;
        return Mode.OTHER;
    }
}
