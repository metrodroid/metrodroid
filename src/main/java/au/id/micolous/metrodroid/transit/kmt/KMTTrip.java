/*
 * KMTTrip.java
 *
 * Copyright 2018 Bondan Sumbodo <sybond@gmail.com>
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

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

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
    private static final String KMT_STR = "kmt";

    private static Calendar calcDate(byte[] data) {
        int fulloffset = Utils.byteArrayToInt(data, 0, 4);
        if (fulloffset == 0) {
            return null;
        }
        Calendar c = new GregorianCalendar(KMTTransitData.TIME_ZONE);
        c.setTimeInMillis(KMTTransitData.KMT_EPOCH);
        c.add(Calendar.SECOND, fulloffset);
        return c;
    }

    private static Station getStation(int Code) {
        return StationTableReader.getStation(KMT_STR, Code);
    }

    public KMTTrip(FelicaBlock block) {
        byte[] data = block.getData();
        mProcessType = data[12] & 0xff;
        mSequenceNumber = Utils.byteArrayToInt(data, 13, 3);
        mTimestamp = calcDate(data);
        mTransactionAmount = Utils.byteArrayToInt(data, 4, 4);
        mEndGateCode = data[9] & 0xff;
    }

    public Station getEndStation() {
        return getStation(mEndGateCode);
    }

    private KMTTrip(Parcel parcel) {
        mProcessType = parcel.readInt();
        mSequenceNumber = parcel.readInt();
        mTimestamp = Utils.unparcelCalendar(parcel);
        mTransactionAmount = parcel.readInt();
        mEndGateCode = parcel.readInt();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mProcessType);
        parcel.writeInt(mSequenceNumber);
        Utils.parcelCalendar(parcel, mTimestamp);
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

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (mProcessType != 1) {
            return TransitCurrency.IDR(-mTransactionAmount);
        }
        return TransitCurrency.IDR(mTransactionAmount);
    }

    public String getAgencyName() {
        String str;
        if (mProcessType == 1)
            str = Utils.localizeString(R.string.felica_process_charge);
        else
            str = Utils.localizeString(R.string.felica_terminal_pos);
        return str;
    }

    public boolean hasTime() {
        return mTimestamp != null;
    }

    public String getRouteName() {
        return Utils.localizeString(R.string.kmt_def_route);
    }

    public int describeContents() {
        return 0;
    }

}
