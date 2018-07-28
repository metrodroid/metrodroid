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
import android.text.Spanned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.card.calypso.CalypsoCard;
import au.id.micolous.metrodroid.card.calypso.CalypsoRecord;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class RavKavTransitData extends TransitData {
    private static final String RAVKAV_AID = "3MTR.ICA";
    public static final String NAME = "RavKav";
    private final String mSerial;
    private final int mBalance;
    List<RavKavTrip> mTrips;

    public static final Creator<RavKavTransitData> CREATOR = new Creator<RavKavTransitData>() {
        public RavKavTransitData createFromParcel(Parcel parcel) {
            return new RavKavTransitData(parcel);
        }

        public RavKavTransitData[] newArray(int size) {
            return new RavKavTransitData[size];
        }
    };


    public RavKavTransitData(CalypsoCard card) {
        mSerial = getSerial(card);
        mBalance = Utils.byteArrayToInt(card.getFile(CalypsoCard.File.TICKETING_COUNTERS_1).getRecord(1).getData(),
                0, 3);
        mTrips = new ArrayList<>();
        RavKavTrip last = null;
        for (CalypsoRecord record : card.getFile(CalypsoCard.File.TICKETING_LOG).getRecords()) {
            RavKavTrip t = new RavKavTrip(record.getData());
            if (t == null)
                continue;
            if (last != null && last.shouldBeMerged(t)) {
                last.merge(t);
                continue;
            }
            last = t;
            mTrips.add(t);
        }
    }

    private static String getSerial(CalypsoCard card) {
        return Long.toString(Utils.byteArrayToLong(card.getTagId()));
    }

    public static TransitIdentity parseTransitIdentity(CalypsoCard card) {
        return new TransitIdentity(NAME, getSerial(card));
    }

    public static boolean check(CalypsoCard card) {
        return RAVKAV_AID.equals(card.getAID());
    }

    public static RavKavTransitData parseTransitData(CalypsoCard card) {
        return new RavKavTransitData(card);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new RavKavTrip[0]);
    }

    @Nullable
    @Override
    public Integer getBalance() {
        return mBalance;
    }

    @Override
    public Spanned formatCurrencyString(int currency, boolean isBalance) {
        return Utils.formatCurrencyString(currency, isBalance, "ILS");
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerial);
        dest.writeInt(mBalance);
        dest.writeParcelableArray(mTrips.toArray(new RavKavTrip[0]), flags);
    }

    public RavKavTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mBalance = parcel.readInt();
        mTrips = Arrays.asList((RavKavTrip[]) parcel.readParcelableArray(null));
    }
}
