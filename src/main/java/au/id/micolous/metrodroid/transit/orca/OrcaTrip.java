/*
 * OrcaTrip.java
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
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.files.DesfireRecord;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class OrcaTrip extends Transaction {
    public static final Creator<OrcaTrip> CREATOR = new Creator<OrcaTrip>() {
        public OrcaTrip createFromParcel(Parcel parcel) {
            return new OrcaTrip(parcel);
        }

        public OrcaTrip[] newArray(int size) {
            return new OrcaTrip[size];
        }
    };
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Los_Angeles");
    private static final String ORCA_STR = "orca";

    final long mTimestamp;
    final int mCoachNum;
    final int mFare;
    final int mNewBalance;
    final int mAgency;
    final int mTransType;
    final boolean mIsTopup;

    static final int TRANS_TYPE_PURSE_USE = 0x0c;
    static final int TRANS_TYPE_CANCEL_TRIP = 0x01;
    static final int TRANS_TYPE_TAP_IN = 0x03;
    static final int TRANS_TYPE_TAP_OUT = 0x07;
    static final int TRANS_TYPE_PASS_USE = 0x60;


    public OrcaTrip(DesfireRecord record, boolean isTopup) {
        byte[] useData = record.getData();

        mIsTopup = isTopup;
        mAgency = Utils.getBitsFromBuffer(useData, 24, 4);
        mTimestamp = Utils.getBitsFromBuffer(useData, 28, 32);
        mCoachNum = Utils.getBitsFromBuffer(useData, 76, 16);
        mFare = Utils.getBitsFromBuffer(useData, 120, 15);
        mTransType = Utils.getBitsFromBuffer(useData, 136, 8);
        mNewBalance = Utils.getBitsFromBuffer(useData, 272, 16);
    }

    OrcaTrip(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mCoachNum = parcel.readInt();
        mFare = parcel.readInt();
        mNewBalance = parcel.readInt();
        mAgency = parcel.readInt();
        mTransType = parcel.readInt();
        mIsTopup = parcel.readInt() != 0;
    }

    @Override
    public Calendar getTimestamp() {
        if (mTimestamp == 0)
            return null;
        Calendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(mTimestamp * 1000);
        return g;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return StationTableReader.getOperatorName(ORCA_STR, mAgency, isShort);
    }

    @Override
    protected boolean isTapOff() {
        return !mIsTopup && mTransType == TRANS_TYPE_TAP_OUT;
    }

    @Override
    protected boolean isCancel() {
        return !mIsTopup && mTransType == TRANS_TYPE_CANCEL_TRIP;
    }

    @Override
    public String getRouteName() {
        if (mIsTopup)
            return Utils.localizeString(R.string.orca_topup);
        if (isLink()) {
            return "Link Light Rail";
        } else if (isSounder()) {
            return "Sounder Train";
        } else {
            // FIXME: Need to find bus route #s
            if (mAgency == OrcaTransitData.AGENCY_ST) {
                return "Express Bus";
            } else if (mAgency == OrcaTransitData.AGENCY_KCM) {
                return "Bus";
            }
            return null;
        }
    }

    @Override
    @Nullable
    public TransitCurrency getFare() {
        return TransitCurrency.USD(mIsTopup ? -mFare : mFare);
    }

    @Override
    public Station getStation() {
        if (mIsTopup)
            return null;

        Station s = StationTableReader.getStation(
                ORCA_STR, ((mAgency<< 16) | mCoachNum), mCoachNum);

        if (!s.isUnknown())
            return s;
        if (isLink() || isSounder() || mAgency == OrcaTransitData.AGENCY_WSF) {
            return s;
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
    protected boolean isSameTrip(Transaction other) {
        return other instanceof OrcaTrip && mAgency == ((OrcaTrip) other).mAgency;
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
