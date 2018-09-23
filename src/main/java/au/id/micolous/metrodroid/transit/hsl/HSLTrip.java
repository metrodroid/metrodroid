/*
 * HSLTrip.java
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.hsl;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.files.DesfireRecord;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class HSLTrip extends Trip {
    public static final Creator<HSLTrip> CREATOR = new Creator<HSLTrip>() {
        public HSLTrip createFromParcel(Parcel parcel) {
            return new HSLTrip(parcel);
        }

        public HSLTrip[] newArray(int size) {
            return new HSLTrip[size];
        }
    };
    private final int mNewBalance;
    String mLine;
    int mVehicleNumber;
    Calendar mTimestamp;
    int mFare;
    int mArvo;
    Calendar mExpireTimestamp;
    int mPax;

    public HSLTrip(DesfireRecord record) {
        byte[] useData = record.getData();

        mArvo = Utils.getBitsFromBuffer(useData, 0, 1);

        mTimestamp = HSLTransitData.cardDateToCalendar(
                Utils.getBitsFromBuffer(useData, 1, 14),
                Utils.getBitsFromBuffer(useData, 15, 11));
        mExpireTimestamp = HSLTransitData.cardDateToCalendar(
                Utils.getBitsFromBuffer(useData, 26, 14),
                Utils.getBitsFromBuffer(useData, 40, 11));

        mFare = Utils.getBitsFromBuffer(useData, 51, 14);

        mPax = Utils.getBitsFromBuffer(useData, 65, 5);
        mLine = null;
        mVehicleNumber = -1;

        mNewBalance = Utils.getBitsFromBuffer(useData, 70, 20);

    }

    HSLTrip(Parcel parcel) {
        // mArvo, mTimestamp, mExpireTimestamp, mFare, mPax, mNewBalance
        mArvo = parcel.readInt();
        mTimestamp = Utils.unparcelCalendar(parcel);
        mExpireTimestamp = Utils.unparcelCalendar(parcel);
        mFare = parcel.readInt();
        mPax = parcel.readInt();
        mNewBalance = parcel.readInt();
        mLine = null;
        mVehicleNumber = -1;
    }

    public HSLTrip() {
        mTimestamp = mExpireTimestamp = null;
        mFare = mArvo = mPax = mNewBalance = mVehicleNumber = -1;
        mLine = null;
    }

    public Calendar getExpireTimestamp() {
        return this.mExpireTimestamp;
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTimestamp;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        String pax = Utils.localizeString(R.string.hsl_person_format, Integer.toString(mPax));
        if (mArvo == 1) {
            String mins = Utils.localizeString(R.string.hsl_mins_format,
                    Integer.toString((int)((this.mExpireTimestamp.getTimeInMillis() - this.mTimestamp.getTimeInMillis()) / 60000L)));
            String type = Utils.localizeString(R.string.hsl_balance_ticket);
            return String.format("%s, %s, %s", type, pax, mins);
        } else {
            String type = Utils.localizeString(R.string.hsl_pass_ticket);
            return String.format("%s, %s", type, pax);
        }
    }

    @Override
    public String getRouteName() {
        if (mLine != null) {
            return Utils.localizeString(R.string.hsl_line_vehicle, mLine.substring(1), mVehicleNumber);
        }
        return null;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.EUR(mFare);
    }

    @Override
    public Mode getMode() {
        if (mLine != null) {
            if (mLine.equals("1300"))
                return Mode.METRO;
            if (mLine.equals("1019"))
                return Mode.FERRY;
            if (mLine.startsWith("100") || mLine.equals("1010"))
                return Mode.TRAM;
            if (mLine.startsWith("3"))
                return Mode.TRAIN;
            return Mode.BUS;
        } else {
            return Mode.BUS;
        }
    }

    public long getCoachNumber() {
        if (mVehicleNumber > -1)
            return mVehicleNumber;
        return mPax;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        // mArvo, mTimestamp, mExpireTimestamp, mFare, mPax, mNewBalance
        parcel.writeInt(mArvo);
        Utils.parcelCalendar(parcel, mTimestamp);
        Utils.parcelCalendar(parcel, mExpireTimestamp);
        parcel.writeInt(mFare);
        parcel.writeInt(mPax);
        parcel.writeInt(mNewBalance);
    }

    public int describeContents() {
        return 0;
    }
}
