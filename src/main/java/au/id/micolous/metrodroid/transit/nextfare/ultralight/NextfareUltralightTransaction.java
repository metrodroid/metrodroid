/*
 * NextfareUltralightTrip.java
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

package au.id.micolous.metrodroid.transit.nextfare.ultralight;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public abstract class NextfareUltralightTransaction extends Transaction {
    private final int mTime;
    private final int mDate;
    protected final int mRoute;
    protected final int mLocation;
    private final int mBaseDate;
    private final int mMachineCode;
    private final int mRecordType;
    private final int mSeqNo;
    private final int mBalance;
    private final int mExpiry;

    public NextfareUltralightTransaction(UltralightCard card, int startPage, int baseDate) {
        ImmutableByteArray page0 = card.getPage(startPage).getData();
        ImmutableByteArray page1 = card.getPage(startPage+1).getData();
        ImmutableByteArray page2 = card.getPage(startPage+2).getData();
        ImmutableByteArray page3 = card.getPage(startPage+3).getData();
        int timeField = page0.byteArrayToIntReversed(0, 2);
        mRecordType = timeField & 0x1f;
        mTime = timeField >> 5;
        mDate = page0.get(2) & 0xff;
        mRoute = page2.get(3);
        mLocation = page2.byteArrayToIntReversed(1, 2);
        mMachineCode = page3.byteArrayToInt(0, 2);
        mBaseDate = baseDate;
        int seqnofield = page1.byteArrayToIntReversed(0, 3);
        mSeqNo = seqnofield & 0x7f;
        mExpiry = page2.get(0);
        mBalance = (seqnofield >> 5) & 0x7ff;
    }

    public boolean isSeqNoGreater(NextfareUltralightTransaction other) {
        // handle wraparound correctly
        return ((mSeqNo - other.mSeqNo) & 0x7f) < 0x3f;
    }

    protected NextfareUltralightTransaction(Parcel parcel) {
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

    @NonNull
    @Override
    public List<String> getRouteNames() {
        return Collections.singletonList(Integer.toHexString(mRoute));
    }

    public Station getStation() {
        if (mLocation == 0) {
            return null;
        }
        return Station.unknown(mLocation);
    }

    public Calendar getTimestamp() {
        return NextfareUltralightTransitData.parseDateTime(getTimezone(), mBaseDate, mDate, mTime);
    }

    protected abstract TimeZone getTimezone();

    public boolean shouldBeMerged(Transaction other) {
        return other instanceof NextfareUltralightTransaction
                && ((NextfareUltralightTransaction) other).mSeqNo == ((mSeqNo + 1) & 0x7f)
                && super.shouldBeMerged(other);
    }

    @Override
    protected boolean isSameTrip(@NonNull Transaction other) {
        return (other instanceof NextfareUltralightTransaction)
                && !isBus() && !((NextfareUltralightTransaction) other).isBus()
                && mRoute == ((NextfareUltralightTransaction) other).mRoute;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    abstract protected boolean isBus();

    @Override
    protected boolean isTapOff() {
        return mRecordType == 6 && !isBus();
    }

    @Override
    protected boolean isTapOn() {
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

    @Override
    public String getAgencyName(boolean isShort) {
        return null;
    }

    @Override
    public TransitCurrency getFare() {
        return null;
    }
}
