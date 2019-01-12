/*
 * BilheteUnicoSPTrip.java
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

package au.id.micolous.metrodroid.transit.bilhete_unico;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

class BilheteUnicoSPTrip extends Trip {
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Sao_Paulo");
    private static final long EPOCH;
    private static final int BUS = 0xb4;
    private static final int TRAM = 0x78;
    private final int mDay;
    private final int mTime;
    private final int mTransport;
    private final int mLocation;
    private final int mLine;
    private final int mFare;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(2000, Calendar.JANUARY, 1, 0, 0, 0);

        EPOCH = epoch.getTimeInMillis();
    }

    BilheteUnicoSPTrip(ClassicSector lastTripSector) {
        ImmutableByteArray block0 = lastTripSector.getBlock(0).getData();
        mTransport = block0.getBitsFromBuffer(0, 8);
        mLocation = block0.getBitsFromBuffer(8, 20);
        mLine = block0.getBitsFromBuffer(28, 20);

        ImmutableByteArray block1 = lastTripSector.getBlock(1).getData();

        mFare = block1.getBitsFromBuffer(36, 16);
        mDay = block1.getBitsFromBuffer(76, 14);
        mTime =  block1.getBitsFromBuffer(90, 11);
    }

    private BilheteUnicoSPTrip(Parcel in) {
        mDay = in.readInt();
        mTime = in.readInt();
        mTransport = in.readInt();
        mLocation = in.readInt();
        mLine = in.readInt();
        mFare = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDay);
        dest.writeInt(mTime);
        dest.writeInt(mTransport);
        dest.writeInt(mLocation);
        dest.writeInt(mLine);
        dest.writeInt(mFare);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BilheteUnicoSPTrip> CREATOR = new Creator<BilheteUnicoSPTrip>() {
        @Override
        public BilheteUnicoSPTrip createFromParcel(Parcel in) {
            return new BilheteUnicoSPTrip(in);
        }

        @Override
        public BilheteUnicoSPTrip[] newArray(int size) {
            return new BilheteUnicoSPTrip[size];
        }
    };

    public static Calendar parseTimestamp(int day, int min) {
        Calendar c = new GregorianCalendar(TZ);
        c.setTimeInMillis(EPOCH);
        c.add(Calendar.DAY_OF_YEAR, day);
        c.add(Calendar.MINUTE, min);
        return c;
    }

    @Override
    public Calendar getStartTimestamp() {
        return parseTimestamp(mDay, mTime);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.BRL(mFare);
    }

    @Override
    public Mode getMode() {
        switch (mTransport) {
            case BUS:
                return Mode.BUS;
            case TRAM:
                return Mode.TRAM;
        }
        return Mode.OTHER;
    }

    @Override
    public String getRouteName() {
        if (mTransport == BUS && mLine == 0x38222)
            return Integer.toHexString(mLocation);
        return Integer.toHexString(mLine);
    }

    @Nullable
    @Override
    public String getHumanReadableRouteID() {
        if (mTransport == BUS && mLine == 0x38222)
            return Integer.toHexString(mLocation);
        return Integer.toHexString(mLine);
    }

    @Nullable
    @Override
    public Station getStartStation() {
        if (mTransport == BUS && mLine == 0x38222)
            return null;
        return Station.unknown(mLocation);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return Integer.toHexString(mTransport);
    }
}
