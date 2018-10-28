/*
 * SmartRiderTransitData.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.smartrider;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Represents a single "tag on" / "tag off" event.
 */

public class SmartRiderTagRecord extends Transaction {
    private static final String TAG = SmartRiderTagRecord.class.getSimpleName();
    private long mTimestamp;
    private boolean mTagOn;
    private String mRoute;
    private int mCost;
    private SmartRiderTransitData.CardType mCardType;

    public SmartRiderTagRecord(SmartRiderTransitData.CardType cardType, byte[] record) {
        mTimestamp = Utils.byteArrayToLongReversed(record, 3, 4);

        mTagOn = (record[7] & 0x10) == 0x10;

        byte[] route = Arrays.copyOfRange(record, 8, 4 + 8);
        route = ArrayUtils.removeAllOccurences(route, (byte) 0x00);
        mRoute = new String(route);

        mCost = Utils.byteArrayToIntReversed(record, 13, 2);

        mCardType = cardType;

        Log.d(TAG, String.format(Locale.ENGLISH, "ts: %s, isTagOn: %s, route: %s, cost: %s",
                mTimestamp, Boolean.toString(mTagOn), mRoute, mCost));
    }

    private SmartRiderTagRecord(Parcel in) {
        mTimestamp = in.readLong();
        mTagOn = in.readByte() != 0;
        mRoute = in.readString();
        mCost = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTimestamp);
        dest.writeByte((byte) (mTagOn ? 1 : 0));
        dest.writeString(mRoute);
        dest.writeInt(mCost);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SmartRiderTagRecord> CREATOR = new Creator<SmartRiderTagRecord>() {
        @Override
        public SmartRiderTagRecord createFromParcel(Parcel in) {
            return new SmartRiderTagRecord(in);
        }

        @Override
        public SmartRiderTagRecord[] newArray(int size) {
            return new SmartRiderTagRecord[size];
        }
    };

    public boolean isValid() {
        return mTimestamp != 0;
    }

    private static final long SMARTRIDER_EPOCH;
    private static final long MYWAY_EPOCH;

    private static final TimeZone SMARTRIDER_TZ = TimeZone.getTimeZone("Australia/Perth");
    private static final TimeZone MYWAY_TZ = TimeZone.getTimeZone("Australia/Sydney"); // Canberra

    static {
        GregorianCalendar srEpoch = new GregorianCalendar(SMARTRIDER_TZ);
        srEpoch.set(Calendar.YEAR, 2000);
        srEpoch.set(Calendar.MONTH, Calendar.JANUARY);
        srEpoch.set(Calendar.DAY_OF_MONTH, 1);
        srEpoch.set(Calendar.HOUR_OF_DAY, 0);
        srEpoch.set(Calendar.MINUTE, 0);
        srEpoch.set(Calendar.SECOND, 0);
        srEpoch.set(Calendar.MILLISECOND, 0);

        SMARTRIDER_EPOCH = srEpoch.getTimeInMillis();

        GregorianCalendar mwEpoch = new GregorianCalendar(MYWAY_TZ);
        mwEpoch.set(Calendar.YEAR, 2000);
        mwEpoch.set(Calendar.MONTH, Calendar.JANUARY);
        mwEpoch.set(Calendar.DAY_OF_MONTH, 1);
        mwEpoch.set(Calendar.HOUR_OF_DAY, 0);
        mwEpoch.set(Calendar.MINUTE, 0);
        mwEpoch.set(Calendar.SECOND, 0);
        mwEpoch.set(Calendar.MILLISECOND, 0);

        MYWAY_EPOCH = mwEpoch.getTimeInMillis();
    }

    private Calendar addSmartRiderEpoch(long epochTime) {
        GregorianCalendar c;
        epochTime *= 1000;
        switch (mCardType) {
            case MYWAY:
                c = new GregorianCalendar(MYWAY_TZ);
                c.setTimeInMillis(MYWAY_EPOCH + epochTime);
                break;

            case SMARTRIDER:
            default:
                c = new GregorianCalendar(SMARTRIDER_TZ);
                c.setTimeInMillis(SMARTRIDER_EPOCH + epochTime);
                break;
        }
        return c;
    }

    @Override
    public Calendar getTimestamp() {
        return addSmartRiderEpoch(mTimestamp);
    }

    @Override
    public boolean isTapOn() {
        return mTagOn;
    }

    @Override
    public boolean isTapOff() {
        return !mTagOn;
    }

    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.AUD(mCost);
    }

    public int getCost() {
        return mCost;
    }

    @NonNull
    @Override
    public List<String> getRouteNames() {
        return Collections.singletonList(mRoute);
    }


    @Override
    protected boolean shouldBeMerged(Transaction other) {
        // Are the two trips within 90 minutes of each other (sanity check)
        return other instanceof SmartRiderTagRecord
                && ((SmartRiderTagRecord) other).mTimestamp - mTimestamp <= 5400
                && super.shouldBeMerged(other);

    }

    @Override
    public String getAgencyName(boolean isShort) {
        switch (mCardType) {
            case MYWAY:
                return "ACTION";

            case SMARTRIDER:
                return "TransPerth";

            default:
                return "";
        }
    }

    @Override
    public Trip.Mode getMode() {
        switch (mCardType) {
            case MYWAY:
                return Trip.Mode.BUS;

            case SMARTRIDER:
                if ("RAIL".equalsIgnoreCase(mRoute)) {
                    return Trip.Mode.TRAIN;
                } else if ("300".equals(mRoute)) {
                    // TODO: verify this
                    // There is also a bus with route number 300, but it is a free service.
                    return Trip.Mode.FERRY;
                } else {
                    return Trip.Mode.BUS;
                }

            default:
                return Trip.Mode.OTHER;
        }
    }

    @Override
    protected boolean isSameTrip(Transaction other) {
        // SmartRider only ever records route names.
        return getRouteNames().get(0).equals(other.getRouteNames().get(0));
    }

    @Override
    public Station getStation() {
        return null;
    }
}
