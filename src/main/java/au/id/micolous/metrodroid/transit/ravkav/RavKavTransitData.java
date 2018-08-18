/*
 * RavKavTransitData.java
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

package au.id.micolous.metrodroid.transit.ravkav;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class RavKavTransitData extends TransitData {
    private static final String RAVKAV_TICKET_ENV = "06ec06000006";
    private final String mSerial;
    private final int mBalance;
    private final List<RavKavTrip> mTrips;

    public static final Creator<RavKavTransitData> CREATOR = new Creator<RavKavTransitData>() {
        public RavKavTransitData createFromParcel(Parcel parcel) {
            return new RavKavTransitData(parcel);
        }

        public RavKavTransitData[] newArray(int size) {
            return new RavKavTransitData[size];
        }
    };


    private RavKavTransitData(CalypsoApplication card) {
        mSerial = getSerial(card);
        mBalance = Utils.byteArrayToInt(card.getFile(CalypsoApplication.File.TICKETING_COUNTERS_1).getRecord(1).getData(),
                0, 3);
        mTrips = new ArrayList<>();
        RavKavTrip last = null;
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_LOG).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            RavKavTrip t = new RavKavTrip(record.getData());
            if (last != null && last.shouldBeMerged(t)) {
                last.merge(t);
                continue;
            }
            last = t;
            mTrips.add(t);
        }
    }

    private static String getSerial(CalypsoApplication card) {
        return Long.toString(Utils.byteArrayToLong(card.getTagId()));
    }

    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_ravkav), getSerial(card));
    }

    public static boolean check(CalypsoApplication card) {
        try {
            return RAVKAV_TICKET_ENV.equals(Utils.getHexString(card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData(), 0, 6));
        } catch (Exception e) {
            return false;
        }
    }

    public static RavKavTransitData parseTransitData(CalypsoApplication card) {
        return new RavKavTransitData(card);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new RavKavTrip[0]);
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return new TransitCurrency(mBalance, "ILS");
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_ravkav);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerial);
        dest.writeInt(mBalance);
        dest.writeParcelableArray(mTrips.toArray(new RavKavTrip[0]), flags);
    }

    private RavKavTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mBalance = parcel.readInt();
        mTrips = Arrays.asList((RavKavTrip[]) parcel.readParcelableArray(RavKavTrip.class.getClassLoader()));
    }
}
