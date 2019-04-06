/*
 * NextfareTrip.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.nextfare;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;

/**
 * Represents trips on Nextfare
 */
public class NextfareTrip extends TransactionTrip
        // implements Comparable<NextfareTrip>
{
    protected NextfareTrip(@NonNull Transaction transaction) {
        super(transaction);

        if (!(transaction instanceof NextfareTransactionRecord)) {
            throw new RuntimeException("oops");
        }
    }

    protected NextfareTrip(Parcel in) {
        super(in);
    }

    @Nullable
    protected NextfareTransactionRecord getStartRecord() {
        if (mStart instanceof NextfareTransactionRecord) {
            return (NextfareTransactionRecord)mStart;
        }

        return null;
    }

    @Nullable
    protected NextfareTransactionRecord getEndRecord() {
        if (mEnd instanceof NextfareTransactionRecord) {
            return (NextfareTransactionRecord)mEnd;
        }

        return null;
    }

    @NonNull
    protected NextfareTransactionRecord getAnyRecord() {
        NextfareTransactionRecord r = getStartRecord();
        if (r != null) {
            return r;
        }

        // For nextfare, this is always true, because the default behaviour makes it a start
        // record!
        r = getEndRecord();
        assert r != null;
        return r;
    }

    @Nullable
    protected String getMdstName() {
        return null;
    }

    @Override
    public Mode getMode() {
        return StationTableReader.getOperatorDefaultMode(
                getMdstName(), getAnyRecord().getModeID());
    }

    @Nullable
    @Override
    public Station getStartStation() {
        final NextfareTransactionRecord r = getStartRecord();
        if (r == null) return null;
        return getStation(r.getStationID());
    }

    @Nullable
    @Override
    public Station getEndStation() {
        final NextfareTransactionRecord r = getEndRecord();
        if (r == null) return null;
        return getStation(r.getStationID());
    }

    protected Station getStation(int stationId) {
        return StationTableReader.getStation(getMdstName(), stationId);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        final int modeInt = getAnyRecord().getModeID();

        if (/* isTopup && */ modeInt == 0)
            return null;
        return StationTableReader.getOperatorName(getMdstName(), modeInt, isShort);
    }

    @Override
    public boolean isTransfer() {
        final NextfareTransactionRecord start = getStartRecord();
        if (start != null && start.isContinuation()) {
            return true;
        }

        final NextfareTransactionRecord end = getEndRecord();
        return end != null && end.isContinuation();
    }

    // Helpers

    private static int getStationIDForRecord(@Nullable NextfareTransactionRecord rec) {
        return rec == null ? 0 : rec.getStationID();
    }

    protected int getStartStationID() {
        return getStationIDForRecord(getStartRecord());
    }

    protected int getEndStationID() {
        return getStationIDForRecord(getEndRecord());
    }

    /*
    public static final Creator<NextfareTrip> CREATOR = new Creator<NextfareTrip>() {

        public NextfareTrip createFromParcel(Parcel in) {
            return new NextfareTrip(in);
        }

        public NextfareTrip[] newArray(int size) {
            return new NextfareTrip[size];
        }
    };
    protected int mJourneyId;
    protected boolean isTopup;
    protected int mModeInt;
    protected Calendar mStartTime;
    protected Calendar mEndTime;
    protected int mStartStation;
    protected int mEndStation;
    protected boolean mContinuation;
    protected int mCost;
    private final String mCurrency;
    @NonNls
    private String mSTR;

    protected NextfareTrip(Parcel parcel) {
        mJourneyId = parcel.readInt();
        mStartTime = Utils.unparcelCalendar(parcel);
        mEndTime = Utils.unparcelCalendar(parcel);
        isTopup = parcel.readInt() != 0;
        mStartStation = parcel.readInt();
        mEndStation = parcel.readInt();
        mModeInt = parcel.readInt();
        mCurrency = parcel.readString();
        mSTR = parcel.readString();
        if (mSTR.isEmpty())
            mSTR = null;
    }

    public NextfareTrip(@NonNull String currency, String str) {
        mStartStation = -1;
        mEndStation = -1;
        mCurrency = currency;
        mSTR = str;
    }

    public NextfareTrip(NextfareTopupRecord rec, @NonNull String currency, String str) {
        mStartTime = rec.getTimestamp();
        mEndTime = null;
        isTopup = true;
        mStartStation = -1;
        mEndStation = -1;
        mModeInt = 0;
        mCost = rec.getCredit() * -1;
        mCurrency = currency;
        mSTR = str;
    }

    @Override
    public Calendar getStartTimestamp() {
        return mStartTime;
    }

    @Override
    public Calendar getEndTimestamp() {
        return mEndTime;
    }

    @Override
    @Nullable
    public Station getStartStation() {
        if (mStartStation < 0) {
            return null;
        }
        return getStation(mStartStation);
    }

    @Override
    @Nullable
    public Station getEndStation() {
        if (mEndTime != null && mEndStation > -1) {
            return getStation(mEndStation);
        } else {
            return null;
        }
    }

    protected Station getStation(int stationId) {
        return StationTableReader.getStation(mSTR, stationId);
    }


    @Nullable
    @Override
    public TransitCurrency getFare() {
        return new TransitCurrency(mCost, mCurrency);
    }

    protected Mode lookupMode() {
        return StationTableReader.getOperatorDefaultMode(mSTR, mModeInt);
    }

    @Override
    public Mode getMode() {
        if (isTopup)
            return Mode.TICKET_MACHINE;
        return lookupMode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mJourneyId);
        Utils.parcelCalendar(parcel, mStartTime);
        Utils.parcelCalendar(parcel, mEndTime);
        parcel.writeInt(isTopup ? 1 : 0);
        parcel.writeInt(mStartStation);
        parcel.writeInt(mEndStation);
        parcel.writeInt(mModeInt);
        parcel.writeString(mCurrency);
        parcel.writeString(mSTR == null ? "" : mSTR);
    }

    @Override
    public int compareTo(@NonNull NextfareTrip other) {
        return this.mStartTime.compareTo(other.mStartTime);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        if (isTopup && mModeInt == 0)
            return null;
        return StationTableReader.getOperatorName(mSTR, mModeInt, isShort);
    }

    @Override
    public boolean isTransfer() {
        return mContinuation;
    }
    */
}
