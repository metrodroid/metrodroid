/*
 * MyWayTransitData.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.smartrider;

import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Reader for SmartRider (Western Australia) and MyWay (Australian Capital Territory / Canberra)
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */

public class SmartRiderTransitData extends TransitData {
    public static final String NAME = "MyWay";
    public static final Creator<SmartRiderTransitData> CREATOR = new Creator<SmartRiderTransitData>() {
        @Override
        public SmartRiderTransitData createFromParcel(Parcel in) {
            return new SmartRiderTransitData(in);
        }

        @Override
        public SmartRiderTransitData[] newArray(int size) {
            return new SmartRiderTransitData[size];
        }
    };
    private static final String TAG = "SmartRiderTransitData";
    private static final GregorianCalendar SMARTRIDER_EPOCH = new GregorianCalendar(2000, Calendar.JANUARY, 1);
    private String mSerialNumber;
    private int mBalance;
    private SmartRiderTrip[] mTrips;
    private CardType mCardType;

    private static final String MYWAY_KEY_SALT = "myway";
    // md5sum of Salt + Common Key 2 + Salt, used on sector 7 key A and B.
    private static final String MYWAY_KEY_DIGEST = "29a61b3a4d5c818415350804c82cd834";

    private static final String SMARTRIDER_KEY_SALT = "smartrider";
    // md5sum of Salt + Common Key 2 + Salt, used on Sector 7 key A.
    // md5sum of Salt + Common Key 3 + Salt, used on Sector 7 key B.


    public enum CardType {
        UNKNOWN("Unknown SmartRider"),
        SMARTRIDER("SmartRider"),
        MYWAY("MyWay");

        String mFriendlyName;

        CardType(String friendlyString) {
            mFriendlyName = friendlyString;
        }

        public String getFriendlyName() {
            return mFriendlyName;
        }
    }

    private static CardType detectKeyType(ClassicCard card) {
        MessageDigest md5;
        String digest;

        byte[] key = card.getSector(7).getKey();
        if (key == null || key.length != 6) {
            // We don't have key data, bail out.
            return CardType.UNKNOWN;
        }

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Couldn't find implementation of MD5", e);
            return CardType.UNKNOWN;
        }

        md5.update(MYWAY_KEY_SALT.getBytes());
        md5.update(key);
        md5.update(MYWAY_KEY_SALT.getBytes());

        digest = Utils.getHexString(md5.digest());
        Log.d(TAG, "Myway key digest: " + digest);

        if (MYWAY_KEY_DIGEST.equals(digest)) {
            return CardType.MYWAY;
        }

        // TODO: Detect SmartRider

        return CardType.UNKNOWN;
    }

    public SmartRiderTransitData(Parcel p) {
        mCardType = CardType.valueOf(p.readString());
        mSerialNumber = p.readString();
        mBalance = p.readInt();
        mTrips = new SmartRiderTrip[p.readInt()];
        p.readTypedArray(mTrips, SmartRiderTrip.CREATOR);
    }

    public static boolean check(ClassicCard card) {
        return detectKeyType(card) != CardType.UNKNOWN;
    }

    public SmartRiderTransitData(ClassicCard card) {
        mCardType = detectKeyType(card);
        mSerialNumber = getSerialData(card);

        // Read trips.
        ArrayList<SmartRiderTagRecord> tagRecords = new ArrayList<>();
        ArrayList<SmartRiderTrip> trips = new ArrayList<>();

        for (int s = 10; s <= 13; s++) {
            for (int b = 0; b <= 2; b++) {
                SmartRiderTagRecord r = new SmartRiderTagRecord(card.getSector(s).getBlock(b).getData());

                if (r.getTimestamp() != 0) {
                    tagRecords.add(r);
                }
            }
        }

        // Build the Tag events into trips.
        if (tagRecords.size() >= 1) {
            Collections.sort(tagRecords);
            // Lets figure out the trips.
            int i = 0;

            while (tagRecords.size() > i) {
                SmartRiderTagRecord tapOn = tagRecords.get(i);

                //Log.d(TAG, "TapOn @" + Utils.isoDateTimeFormat(tapOn.getTimestamp()));
                // Start by creating an empty trip

                SmartRiderTrip trip = new SmartRiderTrip(mCardType);

                // Put in the metadatas
                trip.mStartTime = addSmartRiderEpoch(tapOn.getTimestamp());
                trip.mRouteNumber = tapOn.getRoute();
                trip.mCost = tapOn.getCost();

                // Peek at the next record and see if it is part of
                // this journey
                if (tagRecords.size() > i + 1 && shouldMergeJourneys(tapOn, tagRecords.get(i + 1))) {
                    // There is a tap off.  Lets put that data in
                    SmartRiderTagRecord tapOff = tagRecords.get(i + 1);
                    //Log.d(TAG, "TapOff @" + Utils.isoDateTimeFormat(tapOff.getTimestamp()));

                    trip.mEndTime = addSmartRiderEpoch(tapOff.getTimestamp());
                    trip.mCost += tapOff.getCost();

                    // Increment to skip the next record
                    i++;
                } else {
                    // There is no tap off. Journey is probably in progress, or the agency doesn't
                    // do tap offs.
                }

                trips.add(trip);

                // Increment to go to the next record
                i++;
            }

            // Now sort the trips array
            Collections.sort(trips, new Trip.Comparator());

        }

        mTrips = trips.toArray(new SmartRiderTrip[trips.size()]);

        // TODO: Figure out balance priorities properly.

        // Try to pick the balance data that is correct.
        // Take the last transaction, and see what its cost was.
        SmartRiderTagRecord lastTagEvent = tagRecords.get(tagRecords.size() - 1);
        int lastTripCost = lastTagEvent.getCost();

        byte[] balanceRecord;
        byte[] recordA = card.getSector(2).getBlock(2).getData();
        byte[] recordB = card.getSector(3).getBlock(2).getData();

        byte[] lastTripA = Utils.reverseBuffer(recordA, 5, 2);
        byte[] lastTripB = Utils.reverseBuffer(recordB, 5, 2);

        int costA = Utils.byteArrayToInt(lastTripA);
        int costB = Utils.byteArrayToInt(lastTripB);

        // TODO: improve this logic
        if (costB == lastTripCost) {
            Log.d(TAG, "Selecting balance in sector 3");
            balanceRecord = recordB;
        } else if (costA == lastTripCost) {
            Log.d(TAG, "Selecting balance in sector 2");
            balanceRecord = recordA;
        } else {
            Log.w(TAG, String.format("Neither costA (%d) nor costB (%d) match last trip (%d), picking A as fallback.", costA, costB, lastTripCost));
            balanceRecord = recordA;
        }

        byte[] balance = Utils.reverseBuffer(balanceRecord, 7, 2);
        mBalance = Utils.byteArrayToInt(balance);
    }

    private static String getSerialData(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(1).getData();
        String serial = Utils.getHexString(serialData, 6, 5);
        if (serial.startsWith("0")) {
            serial = serial.substring(1);
        }
        return serial;
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return new TransitIdentity(detectKeyType(card).getFriendlyName(), getSerialData(card));
    }

    private static long addSmartRiderEpoch(long epochTime) {
        return (SMARTRIDER_EPOCH.getTimeInMillis() / 1000) + epochTime;
    }

    private static boolean shouldMergeJourneys(SmartRiderTagRecord first, SmartRiderTagRecord second) {
        // Are the two trips on different routes?
        if (!first.getRoute().equals(second.getRoute())) {
            return false;
        }

        // Is the first trip a tag off, or is the second trip a tag on?
        if (!first.isTagOn() || second.isTagOn()) {
            return false;
        }

        // Are the two trips within 90 minutes of each other (sanity check)
        if (second.getTimestamp() - first.getTimestamp() > 5400) {
            return false;
        }

        return true;
    }


    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) mBalance / 100.);
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return mCardType.getFriendlyName();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCardType.toString());
        dest.writeString(mSerialNumber);
        dest.writeInt(mBalance);
        dest.writeInt(mTrips.length);
        dest.writeTypedArray(mTrips, flags);
    }
}
