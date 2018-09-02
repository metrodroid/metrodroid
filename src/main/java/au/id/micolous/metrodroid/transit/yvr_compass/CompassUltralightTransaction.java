/*
 * CompassUltralightTrip.java
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

package au.id.micolous.metrodroid.transit.yvr_compass;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

public class CompassUltralightTransaction implements Parcelable {
    public static final Parcelable.Creator<CompassUltralightTransaction> CREATOR = new Parcelable.Creator<CompassUltralightTransaction>() {
        public CompassUltralightTransaction createFromParcel(Parcel parcel) {
            return new CompassUltralightTransaction(parcel);
        }

        public CompassUltralightTransaction[] newArray(int size) {
            return new CompassUltralightTransaction[size];
        }
    };
    private static final String COMPASS_STR = "compass";

    private final int mTime;
    private final int mDate;
    private final int mRoute;
    private final int mLocation;
    private final int mBaseDate;
    private final int mMachineCode;
    private final int mRecordType;
    private final int mSeqNo;
    private final int mBalance;
    private final int mExpiry;

    public CompassUltralightTransaction(UltralightCard card, int startPage, int baseDate) {
        byte []page0 = card.getPage(startPage).getData();
        byte []page1 = card.getPage(startPage+1).getData();
        byte []page2 = card.getPage(startPage+2).getData();
        byte []page3 = card.getPage(startPage+3).getData();
        int timeField = Utils.byteArrayToIntReversed(page0, 0, 2);
        mRecordType = timeField & 0x1f;
        mTime = timeField >> 5;
        mDate = page0[2] & 0xff;
        mRoute = page2[3];
        mLocation = Utils.byteArrayToIntReversed(page2, 1, 2);
        mMachineCode = Utils.byteArrayToInt(page3, 0, 2);
        mBaseDate = baseDate;
        int seqnofield = Utils.byteArrayToIntReversed(page1, 0, 3);
        mSeqNo = seqnofield & 0x7f;
        mExpiry = page2[0];
        mBalance = (seqnofield >> 5) & 0x7ff;
    }

    public boolean isSeqNoGreater(CompassUltralightTransaction other) {
        // handle wraparound correctly
        return ((mSeqNo - other.mSeqNo) & 0x7f) < 0x3f;
    }

    CompassUltralightTransaction(Parcel parcel) {
        mTime = parcel.readInt();
        mDate = parcel.readInt();
        mRoute = parcel.readInt();
        mLocation = parcel.readInt();
        mMachineCode = parcel.readInt();
        mBaseDate = parcel.readInt();
        mRecordType = parcel.readInt();
        mSeqNo = parcel.readInt();
        mExpiry = parcel.readInt();
        mBalance = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTime);
        dest.writeInt(mDate);
        dest.writeInt(mRoute);
        dest.writeInt(mLocation);
        dest.writeInt(mMachineCode);
        dest.writeInt(mBaseDate);
        dest.writeInt(mRecordType);
        dest.writeInt(mSeqNo);
        dest.writeInt(mExpiry);
        dest.writeInt(mBalance);
    }

    public String getRouteName() {
        return Integer.toHexString(mRoute);
    }


    public Station getStation() {
        return StationTableReader.getStation(COMPASS_STR, mLocation);
    }

    public Calendar getTimestamp() {
        return CompassUltralightTransitData.parseDateTime(mBaseDate, -mDate, mTime);
    }

    public boolean isSameTripTapOut(CompassUltralightTransaction other) {
        if (mSeqNo != ((other.mSeqNo + 1) & 0x7f))
            return false;
        if (isBus() || other.isBus())
            return false;
        return mRoute == other.mRoute && isTapOut() && other.isTapIn();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private boolean isBus() {
        return mRoute == 5 || mRoute == 7;
    }

    public Trip.Mode getMode() {
        if (isBus())
            return Trip.Mode.BUS;
        if (mRoute == 3 || mRoute == 9 || mRoute == 0xa)
            return Trip.Mode.TRAIN;
        if (mRoute == 0)
            return Trip.Mode.TICKET_MACHINE;
        return Trip.Mode.OTHER;
    }


    public boolean isTapOut() {
        return mRecordType == 6 && !isBus();
    }

    private boolean isTapIn() {
        return mRecordType == 2
                || mRecordType == 4
                || (mRecordType == 6 && isBus())
                || mRecordType == 0x12
                || mRecordType == 0x16;
    }

    public int getExpiry() {
        return mExpiry;
    }

    public int getBalance() {
        return mBalance;
    }
}
