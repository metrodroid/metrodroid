/*
 * ClipperTransitData.java
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

package au.id.micolous.metrodroid.transit.clipper;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.apache.commons.lang3.ArrayUtils;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class ClipperTransitData extends TransitData {
    public static final Creator<ClipperTransitData> CREATOR = new Creator<ClipperTransitData>() {
        public ClipperTransitData createFromParcel(Parcel parcel) {
            return new ClipperTransitData(parcel);
        }

        public ClipperTransitData[] newArray(int size) {
            return new ClipperTransitData[size];
        }
    };
    private static final int RECORD_LENGTH = 32;
    static final TimeZone CLIPPER_TZ = TimeZone.getTimeZone("America/Los_Angeles");
    private static final GregorianCalendar CLIPPER_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(Utils.UTC);
        epoch.set(Calendar.YEAR, 1900);
        epoch.set(Calendar.MONTH, Calendar.JANUARY);
        epoch.set(Calendar.DAY_OF_MONTH, 1);
        epoch.set(Calendar.HOUR_OF_DAY, 0);
        epoch.set(Calendar.MINUTE, 0);
        epoch.set(Calendar.SECOND, 0);
        epoch.set(Calendar.MILLISECOND, 0);

        CLIPPER_EPOCH = epoch;
    }

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.clipper_card)
            .setName("Clipper")
            .setLocation(R.string.location_san_francisco)
            .setCardType(CardType.MifareDesfire)
            .setExtraNote(R.string.card_note_clipper)
            .build();

    @VisibleForTesting
    public static final int APP_ID = 0x9011f2;

    private final long mSerialNumber;
    private final int mBalance;
    private final int mExpiry;
    private final ClipperTrip[] mTrips;
    private final ClipperRefill[] mRefills;

    public ClipperTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
        mBalance = parcel.readInt();
        mExpiry = parcel.readInt();

        mTrips = new ClipperTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, ClipperTrip.CREATOR);

        mRefills = new ClipperRefill[parcel.readInt()];
        parcel.readTypedArray(mRefills, ClipperRefill.CREATOR);
    }

    public ClipperTransitData(Card card) {
        int expiry;
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data;

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x08).getData();
            mSerialNumber = Utils.byteArrayToLong(data, 1, 4);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper serial", ex);
        }

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x01).getData();
            expiry = Utils.byteArrayToInt(data, 8, 2);
        } catch (Exception ex) {
            expiry = 0;
        }

        mExpiry = expiry;
        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x02).getData();
            mBalance = (short) Utils.byteArrayToInt(data, 18, 2);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper balance", ex);
        }

        try {
            mTrips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper trips", ex);
        }

        try {
            mRefills = parseRefills(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper refills", ex);
        }
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID);
        }

        @Override
        public CardInfo getCardInfo() {
            return CARD_INFO;
        }

        @Override
        public TransitData parseTransitData(DesfireCard desfireCard) {
            return new ClipperTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(DesfireCard card) {
            try {
                byte[] data = card.getApplication(APP_ID).getFile(0x08).getData();
                return new TransitIdentity("Clipper", String.valueOf(Utils.byteArrayToLong(data, 1, 4)));
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing Clipper serial", ex);
            }
        }
    };

    @Override
    public String getCardName() {
        return "Clipper";
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(TransitCurrency.USD(mBalance),
                null, clipperTimestampToCalendar(mExpiry * 86400L));
    }

    @Override
    public String getSerialNumber() {
        return Long.toString(mSerialNumber);
    }

    @Override
    public List<Trip> getTrips() {
        // This is done in a roundabout way, as the base type used is the first parameter. Adding it
        // to an empty Trip[] first, coerces types correctly from the start.
        List<Trip> t = new ArrayList<>();
        t.addAll(Arrays.asList(mTrips));
        t.addAll(Arrays.asList(mRefills));
        return t;
    }

    private ClipperTrip[] parseTrips(DesfireCard card) {
        DesfireFile file = card.getApplication(APP_ID).getFile(0x0e);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte[] data = file.getData();
        List<ClipperTrip> result = new ArrayList<>();
        for (int pos = data.length - RECORD_LENGTH; pos >= 0; pos -= RECORD_LENGTH) {
            if (Utils.byteArrayToInt(data, pos + 0x2, 2) == 0)
                continue;

            final ClipperTrip trip = new ClipperTrip(Utils.byteArraySlice(data, pos, RECORD_LENGTH));

            // Some transaction types are temporary -- remove previous trip with the same timestamp.
            ClipperTrip existingTrip = Utils.findInList(result,
                    otherTrip -> trip.getStartTimestamp().equals(otherTrip.getStartTimestamp()));

            if (existingTrip != null) {
                if (existingTrip.getEndTimestamp() != null) {
                    // Old trip has exit timestamp, and is therefore better.
                    continue;
                } else {
                    result.remove(existingTrip);
                }
            }
            result.add(trip);
        }
        ClipperTrip[] useLog = new ClipperTrip[result.size()];
        result.toArray(useLog);

        Arrays.sort(useLog, new Trip.Comparator());

        return useLog;
    }

    private ClipperRefill[] parseRefills(DesfireCard card) {
        DesfireFile file = card.getApplication(APP_ID).getFile(0x04);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte[] data = file.getData();
        int pos = data.length - RECORD_LENGTH;
        List<ClipperRefill> result = new ArrayList<>();
        while (pos >= 0) {
            byte[] slice = Utils.byteArraySlice(data, pos, RECORD_LENGTH);
            ClipperRefill refill = createRefill(slice);
            if (refill != null)
                result.add(refill);
            pos -= RECORD_LENGTH;
        }
        ClipperRefill[] useLog = new ClipperRefill[result.size()];
        useLog = result.toArray(useLog);
        Arrays.sort(useLog);
        return useLog;
    }

    private ClipperRefill createRefill(byte[] useData) {
        long timestamp;
        String machineid;
        int amount, agency;

        agency = Utils.byteArrayToInt(useData, 0x2, 2);
        timestamp = Utils.byteArrayToLong(useData, 0x4, 4);
        machineid = Utils.getHexString(useData, 0x8, 4);
        amount = Utils.byteArrayToInt(useData, 0xe, 2);

        if (timestamp == 0)
            return null;

        return new ClipperRefill(clipperTimestampToCalendar(timestamp), amount, agency, machineid);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber);
        parcel.writeInt(mBalance);
        parcel.writeInt(mExpiry);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);

        parcel.writeInt(mRefills.length);
        parcel.writeTypedArray(mRefills, flags);
    }

    static Calendar clipperTimestampToCalendar(long timestamp) {
        if (timestamp == 0)
            return null;
        Calendar c = new GregorianCalendar(CLIPPER_TZ);
        //Log.d("clipperts", Long.toString(timestamp) + " " + Long.toHexString(timestamp));
        c.setTimeInMillis(CLIPPER_EPOCH.getTimeInMillis() + (timestamp * 1000));
        return c;
    }

    @Nullable
    public static String getNotice() {
        return StationTableReader.getNotice(ClipperData.CLIPPER_STR);
    }
}
