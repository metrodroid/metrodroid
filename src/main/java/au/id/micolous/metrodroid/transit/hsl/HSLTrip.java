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

import org.jetbrains.annotations.NonNls;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.files.DesfireRecord;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

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
    @NonNls String mLine;
    int mVehicleNumber;
    Calendar mTimestamp;
    int mFare;
    int mArvo;
    Calendar mExpireTimestamp;
    int mPax;

    public HSLTrip(DesfireRecord record) {
        ImmutableByteArray useData = record.getData();

        mArvo = useData.getBitsFromBuffer(0, 1);

        mTimestamp = HSLTransitData.cardDateToCalendar(
                useData.getBitsFromBuffer(1, 14),
                useData.getBitsFromBuffer(15, 11));
        mExpireTimestamp = HSLTransitData.cardDateToCalendar(
                useData.getBitsFromBuffer(26, 14),
                useData.getBitsFromBuffer(40, 11));

        mFare = useData.getBitsFromBuffer(51, 14);

        mPax = useData.getBitsFromBuffer(65, 5);
        mLine = null;
        mVehicleNumber = -1;

        mNewBalance = useData.getBitsFromBuffer(70, 20);

    }

    private HSLTrip(Parcel parcel) {
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
        mTimestamp = null;
        mExpireTimestamp = null;
        mFare = -1;
        mArvo = -1;
        mPax = -1;
        mNewBalance = -1;
        mVehicleNumber = -1;
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
        if (mArvo == 1) {
            String mins = Utils.localizeString(R.string.hsl_mins_format,
                    Integer.toString((int)((this.mExpireTimestamp.getTimeInMillis() - this.mTimestamp.getTimeInMillis()) / 60000L)));
            String type = Utils.localizeString(R.string.hsl_balance_ticket);
            return String.format("%s, %s", type, mins);
        } else {
            return Utils.localizeString(R.string.hsl_pass_ticket);
        }
    }

    @Override
    public int getPassengerCount() {
        return mPax;
    }

    @Override
    public String getRouteName() {
        if (mLine != null) {
            return mLine.substring(1);
        }
        return null;
    }

    @Nullable
    @Override
    public String getHumanReadableRouteID() {
        return null;
    }

    @Override
    public String getVehicleID() {
        if (mVehicleNumber != 0 && mVehicleNumber != -1)
            return Integer.toString(mVehicleNumber);
        return null;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.EUR(mFare);
    }

    @Override
    public Mode getMode() {
        if (mLine == null)
            return Mode.BUS;

        if (mLine.equals("1300"))
            return Mode.METRO;
        if (mLine.equals("1019"))
            return Mode.FERRY;
        if (mLine.startsWith("100") || mLine.equals("1010"))
            return Mode.TRAM;
        if (mLine.startsWith("3"))
            return Mode.TRAIN;
        return Mode.BUS;
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
