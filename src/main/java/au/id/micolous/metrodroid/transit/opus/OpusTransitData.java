/*
 * OpusTransitData.java
 *
 * Copyright 2018 Etienne Dubeau
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

package au.id.micolous.metrodroid.transit.opus;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public class OpusTransitData extends TransitData {
    private static final String OPUS_TICKET_ENV = "047892000904";
    public static final String NAME = "Opus";
    private final String mSerial;
    private final int mExpiry;
    private final List<OpusSubscription> mSubscriptions;
    private final List<OpusTrip> mTrips;

    private static final TimeZone TZ = TimeZone.getTimeZone("America/Montreal");
    private static final long OPUS_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1997, Calendar.JANUARY, 1, 0, 0, 0);

        OPUS_EPOCH = epoch.getTimeInMillis();
    }


    public static final Creator<OpusTransitData> CREATOR = new Creator<OpusTransitData>() {
        public OpusTransitData createFromParcel(Parcel parcel) {
            return new OpusTransitData(parcel);
        }

        public OpusTransitData[] newArray(int size) {
            return new OpusTransitData[size];
        }
    };


    private OpusTransitData(CalypsoApplication card) {
        mSerial = getSerial(card);
        mExpiry = Utils.getBitsFromBuffer(card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                        .getRecord(1).getData(),
                45, 14);
        mTrips = new ArrayList<>();
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_LOG).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            mTrips.add(new OpusTrip(record.getData()));
        }
        mSubscriptions = new ArrayList<>();
        int i = 0;
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            ISO7816File matchingCtr = card.getFile(
                    ISO7816Selector.makeSelector(0x2000, 0x202A + record.getIndex() - 1));
            if (matchingCtr == null)
                continue;
            mSubscriptions.add(new OpusSubscription(record.getData(), matchingCtr.getRecord(1).getData(),
                    i++));
        }
    }

    public static Calendar parseTime(int d, int t) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(OPUS_EPOCH);
        g.add(Calendar.DAY_OF_YEAR, d);
        g.add(Calendar.MINUTE, t);
        return g;
    }

    @Override
    public List<ListItem> getInfo() {
        return Collections.singletonList(new ListItem(R.string.opus_card_expiry_date, Utils.longDateFormat(parseTime(mExpiry, 0))));
    }


    private static String getSerial(CalypsoApplication card) {
        return Long.toString(Utils.byteArrayToLong(card.getTagId()));
    }

    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        return new TransitIdentity(NAME, getSerial(card));
    }

    public static boolean check(CalypsoApplication card) {
        try {
            return OPUS_TICKET_ENV.equals(Utils.getHexString(card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData(), 0, 6));
        } catch (Exception e) {
            return false;
        }
    }

    public static OpusTransitData parseTransitData(CalypsoApplication card) {
        return new OpusTransitData(card);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new OpusTrip[0]);
    }

    @Override
    public Subscription[] getSubscriptions() {
        return mSubscriptions.toArray(new OpusSubscription[0]);
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return null;
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
        dest.writeInt(mExpiry);
        dest.writeParcelableArray(mTrips.toArray(new OpusTrip[0]), flags);
        dest.writeParcelableArray(mSubscriptions.toArray(new OpusSubscription[0]), flags);
    }

    private OpusTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mExpiry = parcel.readInt();
        mTrips = Arrays.asList((OpusTrip[]) parcel.readParcelableArray(OpusTrip.class.getClassLoader()));
        mSubscriptions = Arrays.asList((OpusSubscription[]) parcel.readParcelableArray(OpusSubscription.class.getClassLoader()  ));
    }
}
