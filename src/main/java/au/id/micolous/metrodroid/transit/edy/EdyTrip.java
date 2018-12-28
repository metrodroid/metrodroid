/*
 * EdyTransitData.java
 *
 * Copyright 2013 Chris Norden
 * Copyright 2013-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
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
package au.id.micolous.metrodroid.transit.edy;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class EdyTrip extends Trip {
    public static final Creator<EdyTrip> CREATOR = new Creator<EdyTrip>() {
        public EdyTrip createFromParcel(Parcel parcel) {
            return new EdyTrip(parcel);
        }

        public EdyTrip[] newArray(int size) {
            return new EdyTrip[size];
        }
    };
    private final int mProcessType;
    private final int mSequenceNumber;
    private final Calendar mTimestamp;
    private final int mTransactionAmount;
    private final int mBalance;

    public EdyTrip(FelicaBlock block) {
        byte[] data = block.getData();

        // Data Offsets with values
        // ------------------------
        // 0x00    type (0x20 = payment, 0x02 = charge, 0x04 = gift)
        // 0x01    sequence number (3 bytes, big-endian)
        // 0x04    date/time (upper 15 bits - added as day offset, lower 17 bits - added as second offset to Jan 1, 2000 00:00:00)
        // 0x08    transaction amount (big-endian)
        // 0x0c    balance (big-endian)

        mProcessType = data[0];
        mSequenceNumber = Utils.byteArrayToInt(data, 1, 3);
        mTimestamp = extractDate(data);
        mTransactionAmount = Utils.byteArrayToInt(data, 8, 4);
        mBalance = Utils.byteArrayToInt(data, 12, 4);
    }

    private EdyTrip(Parcel parcel) {
        mProcessType = parcel.readInt();
        mSequenceNumber = parcel.readInt();
        long t = parcel.readLong();
        if (t != 0) {
            mTimestamp = new GregorianCalendar(EdyTransitData.TIME_ZONE);
            mTimestamp.setTimeInMillis(t);
        } else {
            mTimestamp = null;
        }
        mTransactionAmount = parcel.readInt();
        mBalance = parcel.readInt();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mProcessType);
        parcel.writeInt(mSequenceNumber);
        parcel.writeLong(mTimestamp == null ? 0 : mTimestamp.getTimeInMillis());
        parcel.writeInt(mTransactionAmount);
        parcel.writeInt(mBalance);
    }

    public Mode getMode() {
        // TODO: Revisit this, and check that these Modes are sensible.
        if (mProcessType == EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            return Mode.POS;
        } else if (mProcessType == EdyTransitData.FELICA_MODE_EDY_CHARGE) {
            return Mode.TICKET_MACHINE;
        } else if (mProcessType == EdyTransitData.FELICA_MODE_EDY_GIFT) {
            return Mode.VENDING_MACHINE;
        } else {
            return Mode.OTHER;
        }
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTimestamp;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (mProcessType != EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            // Credits are "negative"
            return TransitCurrency.JPY(-mTransactionAmount);
        }

        return TransitCurrency.JPY(mTransactionAmount);
    }

    public int describeContents() {
        return 0;
    }

    private static Calendar extractDate(byte[] data) {
        int fulloffset = Utils.byteArrayToInt(data, 4, 4);
        if (fulloffset == 0)
            return null;

        int dateoffset = fulloffset >>> 17;
        int timeoffset = fulloffset & 0x1ffff;

        Calendar c = new GregorianCalendar(EdyTransitData.TIME_ZONE);
        c.set(2000, 0, 1, 0, 0, 0);
        c.add(Calendar.DATE, dateoffset);
        c.add(Calendar.SECOND, timeoffset);

        return c;
    }
}
