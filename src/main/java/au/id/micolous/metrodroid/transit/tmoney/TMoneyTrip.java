/*
 * TmoneyTrip.java
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

package au.id.micolous.metrodroid.transit.tmoney;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class TMoneyTrip extends Trip {
    public static final Creator<TMoneyTrip> CREATOR = new Creator<TMoneyTrip>() {

        public TMoneyTrip createFromParcel(Parcel in) {
            return new TMoneyTrip(in);
        }

        public TMoneyTrip[] newArray(int size) {
            return new TMoneyTrip[size];
        }
    };

    private final long mTime;
    private final int mCost;
    private final int mType;
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Seoul");
    private static final long INVALID_DATETIME = 0xffffffffffffffL;

    public TMoneyTrip(Parcel parcel) {
        mType = parcel.readInt();
        mTime = parcel.readLong();
        mCost = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mType);
        parcel.writeLong(mTime);
        parcel.writeInt(mCost);
    }

    public TMoneyTrip(int type, int cost, long time) {
        mType = type;
        mCost = cost;
        mTime = time;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.KRW(mCost);
    }

    @Override
    public Mode getMode() {
        switch (mType) {
            case 2:
                return Mode.TICKET_MACHINE;
            default:
                return Mode.OTHER;
        }
    }

    private static Calendar parseHexDateTime(long val) {
        if (val == INVALID_DATETIME)
            return null;
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.set(Utils.convertBCDtoInteger((int) (val >> 40)),
                Utils.convertBCDtoInteger((int) ((val >> 32) & 0xffL))-1,
                Utils.convertBCDtoInteger((int) ((val >> 24) & 0xffL)),
                Utils.convertBCDtoInteger((int) ((val >> 16) & 0xffL)),
                Utils.convertBCDtoInteger((int) ((val >> 8) & 0xffL)),
                Utils.convertBCDtoInteger((int) ((val) & 0xffL)));
        return  g;
    }


    @Override
    public Calendar getStartTimestamp() {
        return parseHexDateTime (mTime);
    }

    public static TMoneyTrip parseTrip(byte[] data) {
        int type, cost;
        long time;
        // 1 byte type
        type = data[0];
        // 1 byte unknown
        // 4 bytes balance after transaction
        // 4 bytes counter
        // 4 bytes cost
        cost = Utils.byteArrayToInt(data, 10,4);
        if (type == 2)
            cost = -cost;
        // 2 bytes unknown
        // 1 byte type??
        // 7 bytes unknown
        // 7 bytes time
        time = Utils.byteArrayToLong(data, 26, 7);
        // 7 bytes zero
        // 4 bytes unknown
        // 2 bytes zero
        if (cost == 0 && time == INVALID_DATETIME)
            return null;
        return new TMoneyTrip(type, cost, time);
    }
}
