/*
 * SmartRiderTransitData.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Reader for SmartRider (Western Australia) and MyWay (Australian Capital Territory / Canberra)
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */

public class SmartRiderTransitData extends TransitData {
    private static final String SMARTRIDER_NAME = "SmartRider";
    private static final String MYWAY_NAME = "MyWay";
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

    public static final CardInfo SMARTRIDER_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.smartrider_card)
            .setName(SmartRiderTransitData.SMARTRIDER_NAME)
            .setLocation(R.string.location_wa_australia)
            .setCardType(au.id.micolous.metrodroid.card.CardType.MifareClassic)
            .setKeysRequired()
            .setPreview() // We don't know about ferries.
            .build();

    public static final CardInfo MYWAY_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.myway_card)
            .setName(SmartRiderTransitData.MYWAY_NAME)
            .setLocation(R.string.location_act_australia)
            .setCardType(au.id.micolous.metrodroid.card.CardType.MifareClassic)
            .setKeysRequired()
            .build();

    private String mSerialNumber;
    private int mBalance;
    private List<TransactionTrip> mTrips;
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
        try {
            ClassicSectorKey key = card.getSector(7).getKey();
            if (key == null) {
                // We don't have key data, bail out.
                return CardType.UNKNOWN;
            }

            Log.d(TAG, "Checking for MyWay key...");
            if (Utils.checkKeyHash(key, MYWAY_KEY_SALT, MYWAY_KEY_DIGEST) >= 0) {
                return CardType.MYWAY;
            }

            Log.d(TAG, "Checking for SmartRider key...");
            if (Utils.checkKeyHash(key, SMARTRIDER_KEY_SALT,
                    SMARTRIDER_KEY2_DIGEST, SMARTRIDER_KEY3_DIGEST) >= 0) {
                return CardType.SMARTRIDER;
            }
        } catch (IndexOutOfBoundsException ignored) {
            // If that sector number is too high, then it's not for us.
        }

        return CardType.UNKNOWN;
    }

    public SmartRiderTransitData(Parcel p) {
        mCardType = CardType.valueOf(p.readString());
        mSerialNumber = p.readString();
        mBalance = p.readInt();
        mTrips = p.readArrayList(TransactionTrip.class.getClassLoader());
    }

    public static boolean check(ClassicCard card) {
        return detectKeyType(card) != CardType.UNKNOWN;
    }

    public SmartRiderTransitData(ClassicCard card) {
        mCardType = detectKeyType(card);
        mSerialNumber = getSerialData(card);

        // Read trips.
        ArrayList<SmartRiderTagRecord> tagRecords = new ArrayList<>();

        for (int s = 10; s <= 13; s++) {
            for (int b = 0; b <= 2; b++) {
                SmartRiderTagRecord r = new SmartRiderTagRecord(mCardType, card.getSector(s).getBlock(b).getData());

                if (r.isValid()) {
                    tagRecords.add(r);
                }
            }
        }

        mTrips = new ArrayList<>();

        // Build the Tag events into trips.
        if (tagRecords.size() >= 1)
            mTrips.addAll(TransactionTrip.merge(tagRecords, SmartRiderTrip::new));

        // TODO: Figure out balance priorities properly.

        // This presently only picks whatever balance is lowest. Recharge events aren't understood,
        // and parking fees (SmartRider only) are also not understood.  So just pick whatever is
        // the lowest balance, and we're probably right, unless the user has just topped up their
        // card.
        byte[] recordA = card.getSector(2).getBlock(2).getData();
        byte[] recordB = card.getSector(3).getBlock(2).getData();

        int balanceA = Utils.byteArrayToIntReversed(recordA, 7, 2);
        int balanceB = Utils.byteArrayToIntReversed(recordB, 7, 2);

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

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return TransitCurrency.AUD(mBalance);
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new Trip[0]);
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
        dest.writeList(mTrips);
    }
}
