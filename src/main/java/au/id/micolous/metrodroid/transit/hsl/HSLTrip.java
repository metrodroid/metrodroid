/*
 * HSLTrip.java
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
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

import au.id.micolous.metrodroid.card.desfire.files.DesfireRecord;
import au.id.micolous.metrodroid.transit.CompatTrip;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class HSLTrip extends CompatTrip {
    public static final Creator<HSLTrip> CREATOR = new Creator<HSLTrip>() {
        public HSLTrip createFromParcel(Parcel parcel) {
            return new HSLTrip(parcel);
        }

        public HSLTrip[] newArray(int size) {
            return new HSLTrip[size];
        }
    };
    final long mNewBalance;
    String mLine;
    long mVehicleNumber;
    long mTimestamp;
    int mFare;
    long mArvo;
    long mExpireTimestamp;
    long mPax;

    public HSLTrip(DesfireRecord record) {
        byte[] useData = record.getData();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        mArvo = HSLTransitData.bitsToLong(0, 1, usefulData);

        mTimestamp = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(1, 14, usefulData), HSLTransitData.bitsToLong(15, 11, usefulData));
        mExpireTimestamp = HSLTransitData.cardDateToTimestamp(HSLTransitData.bitsToLong(26, 14, usefulData), HSLTransitData.bitsToLong(40, 11, usefulData));

        mFare = (int) HSLTransitData.bitsToLong(51, 14, usefulData);

        mPax = HSLTransitData.bitsToLong(65, 5, usefulData);
        mLine = null;
        mVehicleNumber = -1;

        mNewBalance = HSLTransitData.bitsToLong(70, 20, usefulData);

    }

    HSLTrip(Parcel parcel) {
        // mArvo, mTimestamp, mExpireTimestamp, mFare, mPax, mNewBalance
        mArvo = parcel.readLong();
        mTimestamp = parcel.readLong();
        mExpireTimestamp = parcel.readLong();
        mFare = parcel.readInt();
        mPax = parcel.readLong();
        mNewBalance = parcel.readLong();
        mLine = null;
        mVehicleNumber = -1;
    }

    public HSLTrip() {
        mArvo = mTimestamp = mExpireTimestamp = mPax = mNewBalance = mVehicleNumber = -1;
        mFare = -1;
        mLine = null;
    }

    public double getExpireTimestamp() {
        return this.mExpireTimestamp;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public String getAgencyName() {
        MetrodroidApplication app = MetrodroidApplication.getInstance();
        String pax = app.getString(R.string.hsl_person_format, mPax);
        if (mArvo == 1) {
            String mins = app.getString(R.string.hsl_mins_format, ((this.mExpireTimestamp - this.mTimestamp) / 60));
            String type = app.getString(R.string.hsl_balance_ticket);
            return String.format("%s, %s, %s", type, pax, mins);
        } else {
            String type = app.getString(R.string.hsl_pass_ticket);
            return String.format("%s, %s", type, pax);
        }
    }

    @Override
    public String getRouteName() {
        if (mLine != null) {
            // FIXME: i18n
            return String.format("Line %s, Vehicle %s", mLine.substring(1), mVehicleNumber);
        }
        return null;
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Nullable
    @Override
    public Integer getFare() {
        return mFare;
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

    @Override
    public boolean hasTime() {
        return false;
    }

    public long getCoachNumber() {
        if (mVehicleNumber > -1)
            return mVehicleNumber;
        return mPax;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        // mArvo, mTimestamp, mExpireTimestamp, mFare, mPax, mNewBalance
        parcel.writeLong(mArvo);
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mExpireTimestamp);
        parcel.writeInt(mFare);
        parcel.writeLong(mPax);
        parcel.writeLong(mNewBalance);
    }

    public int describeContents() {
        return 0;
    }
}
