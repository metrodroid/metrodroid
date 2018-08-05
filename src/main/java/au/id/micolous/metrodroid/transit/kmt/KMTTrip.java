/*
 * KMTTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.kmt;

import android.app.Application;
import android.os.Parcel;
import android.support.annotation.Nullable;
import net.kazzz.felica.lib.Util;
import java.util.Calendar;
import java.util.GregorianCalendar;
import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

public class KMTTrip extends Trip {
    public static final Creator<KMTTrip> CREATOR = new Creator<KMTTrip>() {
        public KMTTrip createFromParcel(Parcel parcel) {
            return new KMTTrip(parcel);
        }

        public KMTTrip[] newArray(int size) {
            return new KMTTrip[size];
        }
    };
    private final int mProcessType;
    private final int mSequenceNumber;
    private final Calendar mTimestamp;
    private final int mTransactionAmount;
    private final int mEndGateCode;

    public KMTTrip(FelicaBlock block) {
        byte[] data = block.getData();
        mProcessType = data[12];
        mSequenceNumber = Util.toInt(data[13], data[14], data[15]);
        mTimestamp = KMTUtil.extractDate(data);
        mTransactionAmount = Util.toInt(data[4], data[5], data[6], data[7]);
        mEndGateCode = data[9];
    }

    @Override
    public String getEndStationName() {
        // Need to work on decoding gate code
        // Collect the data !!
        return String.format("[%02X]",mEndGateCode);
    }

    public KMTTrip(Parcel parcel) {
        mProcessType = parcel.readInt();
        mSequenceNumber = parcel.readInt();
        long t = parcel.readLong();
        if (t != 0) {
            mTimestamp = new GregorianCalendar(KMTTransitData.TIME_ZONE);
            mTimestamp.setTimeInMillis(t);
        } else {
            mTimestamp = null;
        }
        mTransactionAmount = parcel.readInt();
        mEndGateCode = parcel.readInt();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mProcessType);
        parcel.writeInt(mSequenceNumber);
        parcel.writeLong(mTimestamp == null ? 0 : mTimestamp.getTimeInMillis());
        parcel.writeInt(mTransactionAmount);
        parcel.writeInt(mEndGateCode);
    }

    public Mode getMode() {
        switch (mProcessType) {
            case 0:
                return Mode.TICKET_MACHINE;
            case 1:
                return Mode.TRAIN;
            case 2:
                return Mode.POS;
            default:
                return Mode.OTHER;
        }
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTimestamp;
    }

    public boolean hasFare() {
        return true;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (mProcessType != 1) {
            return TransitCurrency.IDR(-mTransactionAmount);
        }
        return TransitCurrency.IDR(mTransactionAmount);
    }

    public String getAgencyName() {
        Application app = MetrodroidApplication.getInstance();
        String str;
        if (mProcessType == 1)
            str = app.getString(R.string.felica_process_charge);
        else
            str = app.getString(R.string.felica_terminal_pos);
        return str;
    }

    public boolean hasTime() {
        return mTimestamp != null;
    }

    public String getRouteName() {
        Application app = MetrodroidApplication.getInstance();
        return app.getString(R.string.kmt_def_route);
    }

    public int describeContents() {
        return 0;
    }

}
