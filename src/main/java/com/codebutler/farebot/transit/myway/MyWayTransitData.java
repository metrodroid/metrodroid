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
package com.codebutler.farebot.transit.myway;

import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.card.UnauthorizedException;
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
 * Experimental reader for MyWay (Australian Capital Territory / Canberra)
 * https://github.com/micolous/metrodroid/wiki/MyWay
 * <p>
 * Currently only available in "Fallback" mode.
 */

public class MyWayTransitData extends TransitData {
    public static final String NAME = "MyWay";
    public static final Creator<MyWayTransitData> CREATOR = new Creator<MyWayTransitData>() {
        @Override
        public MyWayTransitData createFromParcel(Parcel in) {
            return new MyWayTransitData(in);
        }

        @Override
        public MyWayTransitData[] newArray(int size) {
            return new MyWayTransitData[size];
        }
    };
    private static final String TAG = "MyWayTransitData";
    private static final GregorianCalendar MYWAY_EPOCH = new GregorianCalendar(2000, Calendar.JANUARY, 1);
    private String mSerialNumber;
    private int mBalance;
    private MyWayTrip[] mTrips;

    private static final String MYWAY_KEY_SALT = "myway"; //
    private static final String MYWAY_KEY_DIGEST = "29a61b3a4d5c818415350804c82cd834";

    public MyWayTransitData(Parcel p) {
        mSerialNumber = p.readString();
        mBalance = p.readInt();
        mTrips = new MyWayTrip[p.readInt()];
        p.readTypedArray(mTrips, MyWayTrip.CREATOR);
    }

    public static boolean check(ClassicCard card) {
        // We don't know how reliable card value data is just yet. But there are some standard
        // keys on the card that we can detect.
        byte[] key7 = card.getSector(7).getKey();
        if (key7 == null) {
            // We don't have key data, bail out.
            return false;
        }

        // We have some key data, so lets see if this is Standard Key B.
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Couldn't find implementation of MD5", e);
            return false;
        }

        md5.update(MYWAY_KEY_SALT.getBytes());
        md5.update(key7);
        md5.update(MYWAY_KEY_SALT.getBytes());

        String digest = Utils.getHexString(md5.digest());

        Log.d(TAG, "Myway key digest: " + digest);
        return digest.equals(MYWAY_KEY_DIGEST);

    }

    public MyWayTransitData(ClassicCard card) {
        mSerialNumber = getSerialData(card);
        // Read trips.
        ArrayList<MyWayTagRecord> tagRecords = new ArrayList<>();
        ArrayList<MyWayTrip> trips = new ArrayList<>();

        for (int s = 10; s <= 12; s++) {
            for (int b = 0; b <= 2; b++) {
                MyWayTagRecord r = new MyWayTagRecord(card.getSector(s).getBlock(b).getData());

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
                MyWayTagRecord tapOn = tagRecords.get(i);

                //Log.d(TAG, "TapOn @" + Utils.isoDateTimeFormat(tapOn.getTimestamp()));
                // Start by creating an empty trip

                MyWayTrip trip = new MyWayTrip();


                // Put in the metadatas
                trip.mStartTime = addMyWayEpoch(tapOn.getTimestamp());
                trip.mRouteNumber = tapOn.getRoute();
                trip.mCost = tapOn.getCost();

                // Peek at the next record and see if it is part of
                // this journey
                if (tagRecords.size() > i + 1 && shouldMergeJourneys(tapOn, tagRecords.get(i + 1))) {
                    // There is a tap off.  Lets put that data in
                    MyWayTagRecord tapOff = tagRecords.get(i + 1);
                    //Log.d(TAG, "TapOff @" + Utils.isoDateTimeFormat(tapOff.getTimestamp()));

                    trip.mEndTime = addMyWayEpoch(tapOff.getTimestamp());
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

        mTrips = trips.toArray(new MyWayTrip[trips.size()]);

        // TODO: Figure out balance priorities properly.

        // Try to pick the balance data that is correct.
        // Take the last transaction, and see what its cost was.
        MyWayTagRecord lastTagEvent = tagRecords.get(tagRecords.size() - 1);
        int lastTripCost = lastTagEvent.getCost();

        byte[] balanceRecord;
        byte[] recordA = card.getSector(2).getBlock(2).getData();
        byte[] recordB = card.getSector(3).getBlock(2).getData();

        byte[] lastTripA = Utils.reverseBuffer(recordA, 5, 2);
        byte[] lastTripB = Utils.reverseBuffer(recordB, 5, 2);

        int costA = Utils.byteArrayToInt(lastTripA);
        int costB = Utils.byteArrayToInt(lastTripB);

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
        return new TransitIdentity(NAME, getSerialData(card));
    }

    private static long addMyWayEpoch(long epochTime) {
        return (MYWAY_EPOCH.getTimeInMillis() / 1000) + epochTime;
    }

    private static boolean shouldMergeJourneys(MyWayTagRecord first, MyWayTagRecord second) {
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
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerialNumber);
        dest.writeInt(mBalance);
        dest.writeInt(mTrips.length);
        dest.writeTypedArray(mTrips, flags);
    }
}
