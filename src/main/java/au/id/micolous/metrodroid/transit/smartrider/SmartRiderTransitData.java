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
package au.id.micolous.metrodroid.transit.smartrider;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.Spanned;
import android.util.Log;

import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Reader for SmartRider (Western Australia) and MyWay (Australian Capital Territory / Canberra)
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */

public class SmartRiderTransitData extends TransitData {
    public static final String SMARTRIDER_NAME = "SmartRider";
    public static final String MYWAY_NAME = "MyWay";
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
    private static final TimeZone SMARTRIDER_TZ = TimeZone.getTimeZone("Australia/Perth");
    private static final TimeZone MYWAY_TZ = TimeZone.getTimeZone("Australia/Sydney"); // Canberra

    private static final long SMARTRIDER_EPOCH;
    private static final long MYWAY_EPOCH;

    static {
        GregorianCalendar srEpoch = new GregorianCalendar(SMARTRIDER_TZ);
        srEpoch.set(Calendar.YEAR, 2000);
        srEpoch.set(Calendar.MONTH, Calendar.JANUARY);
        srEpoch.set(Calendar.DAY_OF_MONTH, 1);
        srEpoch.set(Calendar.HOUR_OF_DAY, 0);
        srEpoch.set(Calendar.MINUTE, 0);
        srEpoch.set(Calendar.SECOND, 0);
        srEpoch.set(Calendar.MILLISECOND, 0);

        SMARTRIDER_EPOCH = srEpoch.getTimeInMillis();

        GregorianCalendar mwEpoch = new GregorianCalendar(MYWAY_TZ);
        mwEpoch.set(Calendar.YEAR, 2000);
        mwEpoch.set(Calendar.MONTH, Calendar.JANUARY);
        mwEpoch.set(Calendar.DAY_OF_MONTH, 1);
        mwEpoch.set(Calendar.HOUR_OF_DAY, 0);
        mwEpoch.set(Calendar.MINUTE, 0);
        mwEpoch.set(Calendar.SECOND, 0);
        mwEpoch.set(Calendar.MILLISECOND, 0);

        MYWAY_EPOCH = mwEpoch.getTimeInMillis();
    }

    private String mSerialNumber;
    private int mBalance;
    private SmartRiderTrip[] mTrips;
    private CardType mCardType;

    // Unfortunately, there's no way to reliably identify these cards except for the "standard" keys
    // which are used for some empty sectors.  It is not enough to read the whole card (most data is
    // protected by a unique key).
    //
    // We don't want to actually include these keys in the program, so include a hashed version of
    // this key.
    private static final String MYWAY_KEY_SALT = "myway";
    // md5sum of Salt + Common Key 2 + Salt, used on sector 7 key A and B.
    private static final String MYWAY_KEY_DIGEST = "29a61b3a4d5c818415350804c82cd834";

    private static final String SMARTRIDER_KEY_SALT = "smartrider";
    // md5sum of Salt + Common Key 2 + Salt, used on Sector 7 key A.
    private static final String SMARTRIDER_KEY2_DIGEST = "e0913518a5008c03e1b3f2bb3a43ff78";
    // md5sum of Salt + Common Key 3 + Salt, used on Sector 7 key B.
    private static final String SMARTRIDER_KEY3_DIGEST = "bc510c0183d2c0316533436038679620";


    public enum CardType {
        UNKNOWN("Unknown SmartRider"),
        SMARTRIDER(SMARTRIDER_NAME),
        MYWAY(MYWAY_NAME);

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

        md5.update(SMARTRIDER_KEY_SALT.getBytes());
        md5.update(key);
        md5.update(SMARTRIDER_KEY_SALT.getBytes());

        digest = Utils.getHexString(md5.digest());
        Log.d(TAG, "Smartrider key digest: " + digest);

        if (SMARTRIDER_KEY2_DIGEST.equals(digest) || SMARTRIDER_KEY3_DIGEST.equals(digest)) {
            return CardType.SMARTRIDER;
        }

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
                Log.d(TAG, String.format(Locale.ENGLISH, "epoch: %s", Utils.isoDateTimeFormat(addSmartRiderEpoch(0))));
                Log.d(TAG, String.format(Locale.ENGLISH, "tripStart: %s, route: %s, cost: %s",
                        Utils.isoDateTimeFormat(trip.mStartTime), trip.mRouteNumber, trip.mCost));

                // Increment to go to the next record
                i++;
            }

            // Now sort the trips array
            Collections.sort(trips, new Trip.Comparator());

        }

        mTrips = trips.toArray(new SmartRiderTrip[trips.size()]);

        // TODO: Figure out balance priorities properly.

        // This presently only picks whatever balance is lowest. Recharge events aren't understood,
        // and parking fees (SmartRider only) are also not understood.  So just pick whatever is
        // the lowest balance, and we're probably right, unless the user has just topped up their
        // card.
        byte[] recordA = card.getSector(2).getBlock(2).getData();
        byte[] recordB = card.getSector(3).getBlock(2).getData();

        byte[] balanceDataA = Utils.reverseBuffer(recordA, 7, 2);
        byte[] balanceDataB = Utils.reverseBuffer(recordB, 7, 2);

        int balanceA = Utils.byteArrayToInt(balanceDataA);
        int balanceB = Utils.byteArrayToInt(balanceDataB);

        Log.d(TAG, String.format("balanceA = %d, balanceB = %d", balanceA, balanceB));
        mBalance = balanceA < balanceB ? balanceA : balanceB;
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

    private Calendar addSmartRiderEpoch(long epochTime) {
        GregorianCalendar c;
        epochTime *= 1000;
        switch (mCardType) {
            case MYWAY:
                c = new GregorianCalendar(MYWAY_TZ);
                c.setTimeInMillis(MYWAY_EPOCH + epochTime);
                break;

            case SMARTRIDER:
            default:
                c = new GregorianCalendar(SMARTRIDER_TZ);
                c.setTimeInMillis(SMARTRIDER_EPOCH + epochTime);
                break;
        }
        return c;
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

    @Nullable
    @Override
    public Integer getBalance() {
        return mBalance;
    }

    @Override
    public Spanned formatCurrencyString(int currency, boolean isBalance) {
        return Utils.formatCurrencyString(currency, isBalance, "AUD");
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
