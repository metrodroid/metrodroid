/*
 * OrcaTransaction.java
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
 * Copyright 2014 Kramer Campbell
 * Copyright 2015 Sean CyberKitsune McClenaghan
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package au.id.micolous.metrodroid.transit.orca;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.time.Timestamp;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class OrcaTransaction extends Transaction {
    public static final Creator<OrcaTransaction> CREATOR = new Creator<OrcaTransaction>() {
        public OrcaTransaction createFromParcel(Parcel parcel) {
            return new OrcaTransaction(parcel);
        }

        public OrcaTransaction[] newArray(int size) {
            return new OrcaTransaction[size];
        }
    };
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Los_Angeles");
    private static final String ORCA_STR = "orca";

    private final long mTimestamp;
    private final int mCoachNum;
    private final int mFare;
    private final int mNewBalance;
    private final int mAgency;
    private final int mTransType;
    private final boolean mIsTopup;

    private static final int TRANS_TYPE_PURSE_USE = 0x0c;
    private static final int TRANS_TYPE_CANCEL_TRIP = 0x01;
    private static final int TRANS_TYPE_TAP_IN = 0x03;
    private static final int TRANS_TYPE_TAP_OUT = 0x07;
    private static final int TRANS_TYPE_PASS_USE = 0x60;

    public OrcaTransaction(ImmutableByteArray useData, boolean isTopup) {
        mIsTopup = isTopup;
        mAgency = useData.getBitsFromBuffer(24, 4);
        mTimestamp = useData.getBitsFromBuffer(28, 32);
        mCoachNum = useData.getBitsFromBuffer(76, 16);
        mFare = useData.getBitsFromBuffer(120, 15);
        mTransType = useData.getBitsFromBuffer(136, 8);
        mNewBalance = useData.getBitsFromBuffer(272, 16);
    }

    OrcaTransaction(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mCoachNum = parcel.readInt();
        mFare = parcel.readInt();
        mNewBalance = parcel.readInt();
        mAgency = parcel.readInt();
        mTransType = parcel.readInt();
        mIsTopup = parcel.readInt() != 0;
    }

    @Override
    public Timestamp getTimestamp() {
        if (mTimestamp == 0)
            return null;
        Calendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(mTimestamp * 1000);
        return TimestampFormatterKt.calendar2ts(g);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return StationTableReader.getOperatorName(ORCA_STR, mAgency, isShort);
    }

    @Override
    public boolean isTapOff() {
        return !mIsTopup && mTransType == TRANS_TYPE_TAP_OUT;
    }

    @Override
    public boolean isCancel() {
        return !mIsTopup && mTransType == TRANS_TYPE_CANCEL_TRIP;
    }

    @NonNull
    @Override
    public List<String> getRouteNames() {
        if (mIsTopup) {
            return Collections.singletonList(Localizer.INSTANCE.localizeString(R.string.orca_topup));
        } else if (isLink()) {
            return Collections.singletonList("Link Light Rail");
        } else if (isSounder()) {
            return Collections.singletonList("Sounder Train");
        } else {
            // FIXME: Need to find bus route #s
            if (mAgency == OrcaTransitData.AGENCY_ST) {
                return Collections.singletonList("Express Bus");
            } else if (mAgency == OrcaTransitData.AGENCY_KCM) {
                return Collections.singletonList("Bus");
            }
            return Collections.emptyList();
        }
    }

    @Override
    @Nullable
    public TransitCurrency getFare() {
        return TransitCurrency.USD(mIsTopup ? -mFare : mFare);
    }

    private static Station getStation(int agency, int stationId) {
        return StationTableReader.getStationNoFallback(ORCA_STR, ((agency << 16)|stationId));
    }

    @Override
    public Station getStation() {
        if (mIsTopup)
            return null;
        Station s = getStation(mAgency, mCoachNum);
        if (s != null)
            return s;
        if (isLink() || isSounder() || mAgency == OrcaTransitData.AGENCY_WSF) {
            return Station.unknown(mCoachNum);
        } else {
            return null;
        }
    }

    @Override
    public String getVehicleID() {
        if (mIsTopup)
            return String.valueOf(mCoachNum);
        if (isLink() || isSounder() || mAgency == OrcaTransitData.AGENCY_WSF) {
            return null;
        }

        return String.valueOf(mCoachNum);
    }

    @Override
    public Trip.Mode getMode() {
        if (mIsTopup)
            return Trip.Mode.TICKET_MACHINE;
        if (isLink()) {
            return Trip.Mode.METRO;
        }
        if (isSounder()) {
            return Trip.Mode.TRAIN;
        }
        if (mAgency == OrcaTransitData.AGENCY_WSF) {
            return Trip.Mode.FERRY;
        }

        return Trip.Mode.BUS;
    }

    @Override
    protected boolean isSameTrip(@NonNull Transaction other) {
        return other instanceof OrcaTransaction && mAgency == ((OrcaTransaction) other).mAgency;
    }

    @Override
    protected boolean isTapOn() {
        return !mIsTopup && mTransType == TRANS_TYPE_TAP_IN;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeInt(mCoachNum);
        parcel.writeInt(mFare);
        parcel.writeInt(mNewBalance);
        parcel.writeInt(mAgency);
        parcel.writeInt(mTransType);
        parcel.writeInt(mIsTopup ? 1 : 0);
    }

    public int describeContents() {
        return 0;
    }

    private boolean isLink() {
        return (mAgency == OrcaTransitData.AGENCY_ST && mCoachNum > 10000);
    }

    private boolean isSounder() {
        return (mAgency == OrcaTransitData.AGENCY_ST && mCoachNum < 20);
    }
}
