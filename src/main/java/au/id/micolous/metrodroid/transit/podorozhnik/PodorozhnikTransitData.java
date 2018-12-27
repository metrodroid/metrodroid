/*
 * PodorozhnikTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.podorozhnik;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Podorozhnik cards.
 */

public class PodorozhnikTransitData extends TransitData {
    // We don't want to actually include these keys in the program, so include a hashed version of
    // this key.
    private static final String KEY_SALT = "podorozhnik";
    // md5sum of Salt + Common Key + Salt, used on sector 4.
    private static final String KEY_DIGEST_A = "f3267ff451b1fc3076ba12dcee2bf803";
    private static final String KEY_DIGEST_B = "3823b5f0b45f3519d0ce4a8b5b9f1437";

    public static final Parcelable.Creator<PodorozhnikTransitData> CREATOR = new Parcelable.Creator<PodorozhnikTransitData>() {
        public PodorozhnikTransitData createFromParcel(Parcel parcel) {
            return new PodorozhnikTransitData(parcel);
        }

        public PodorozhnikTransitData[] newArray(int size) {
            return new PodorozhnikTransitData[size];
        }
    };

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            // seqgo_card_alpha has identical geometry
            .setImageId(R.drawable.podorozhnik_card, R.drawable.seqgo_card_alpha)
            .setName(Utils.localizeString(R.string.card_name_podorozhnik))
            .setLocation(R.string.location_saint_petersburg)
            .setCardType(CardType.MifareClassic)
            .setExtraNote(R.string.card_note_russia)
            .setKeysRequired()
            .setPreview()
            .build();


    private static final long PODOROZHNIK_EPOCH;
    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Moscow");

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(2010, Calendar.JANUARY, 1, 0, 0, 0);

        PODOROZHNIK_EPOCH = epoch.getTimeInMillis();
    }

    private static final String TAG = "PodorozhnikTransitData";

    private int mBalance;
    private int mLastTopup;
    private int mLastTopupTime;
    private int mLastFare;
    private final List<Integer> mExtraTripTimes;
    private int mLastValidator;
    private int mLastTripTime;
    private int mGroundCounter;
    private int mSubwayCounter;
    private int mLastTransport;
    private final String mSerial;
    private int mLastTopupMachine;
    private int mLastTopupAgency;
    private boolean mCountersValid;

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_podorozhnik);
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeInt(mBalance);
	    dest.writeInt(mLastTopup);
	    dest.writeInt(mLastTopupTime);
	    dest.writeInt(mLastFare);
	    dest.writeList(mExtraTripTimes);
	    dest.writeInt(mLastTripTime);
	    dest.writeInt(mLastValidator);
	    dest.writeInt(mGroundCounter);
	    dest.writeInt(mSubwayCounter);
	    dest.writeString(mSerial);
        dest.writeInt(mLastTopupMachine);
        dest.writeInt(mLastTopupAgency);
        dest.writeInt(mCountersValid ? 1 : 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public PodorozhnikTransitData(Parcel p) {
        mBalance = p.readInt();
    	mLastTopup = p.readInt();
	    mLastTopupTime = p.readInt();
	    mLastFare = p.readInt();
        //noinspection unchecked
	    mExtraTripTimes = p.readArrayList(PodorozhnikTransitData.class.getClassLoader());
	    mLastTripTime = p.readInt();
	    mLastValidator = p.readInt();
	    mGroundCounter = p.readInt();
	    mSubwayCounter = p.readInt();
	    mSerial = p.readString();
        mLastTopupMachine = p.readInt();
        mLastTopupAgency = p.readInt();
        mCountersValid = p.readInt() != 0;
    }

    private static String getSerial(byte []uid) {
        String sn;
        sn = "9643 3078 " + Utils.formatNumber(Utils.byteArrayToLongReversed(uid, 0, 7),
                " ", 4, 4, 4, 4, 1);
        sn += Utils.calculateLuhn (sn.replaceAll(" ", ""));// last digit is luhn
        return sn;
    }

    private void decodeSector4(ClassicCard card) {
        ClassicSector sector4 = card.getSector(4);

	    if (sector4 instanceof UnauthorizedClassicSector)
	        return;

	    // Block 0 and block 1 are copies. Let's use block 0
	    byte[] block0 = sector4.getBlock(0).getData();
        byte[] block2 = sector4.getBlock(2).getData();
        mBalance = Utils.byteArrayToIntReversed(block0, 0, 4);
        mLastTopupTime = Utils.byteArrayToIntReversed(block2, 2, 3);
        mLastTopupAgency = block2[5];
        mLastTopupMachine = Utils.byteArrayToIntReversed(block2, 6, 2);
        mLastTopup = Utils.byteArrayToIntReversed(block2, 8, 3);
    }

    private void decodeSector5(ClassicCard card) {
        ClassicSector sector5 = card.getSector(5);

	    if (sector5 instanceof UnauthorizedClassicSector)
	        return;

	    byte[] block0 = sector5.getBlock(0).getData();
	    byte[] block1 = sector5.getBlock(1).getData();
        byte[] block2 = sector5.getBlock(2).getData();

        mLastTripTime = Utils.byteArrayToIntReversed(block0, 0, 3);
        mLastTransport = block0[3] & 0xff;
        mLastValidator = Utils.byteArrayToIntReversed(block0, 4, 2);
        mLastFare = Utils.byteArrayToIntReversed(block0, 6, 4);
        // Usually block1 and block2 are identical. However rarely only one of them
        // gets updated. Pick most recent one for counters but remember both trip
        // timestamps.
        if (Utils.byteArrayToIntReversed(block2, 2, 3) > Utils.byteArrayToIntReversed(block1, 2, 3)) {
            mSubwayCounter = block2[0] & 0xff;
            mGroundCounter = block2[1] & 0xff;
        } else {
            mSubwayCounter = block1[0] & 0xff;
            mGroundCounter = block1[1] & 0xff;
        }
        mCountersValid = true;
        if (mLastTripTime != Utils.byteArrayToIntReversed(block1, 2, 3)) {
            mExtraTripTimes.add(Utils.byteArrayToIntReversed(block1, 2, 3));
        }
        if (mLastTripTime != Utils.byteArrayToIntReversed(block2, 2, 3)
                && Utils.byteArrayToIntReversed(block2, 2, 3) != Utils.byteArrayToIntReversed(block1, 2, 3)) {
            mExtraTripTimes.add(Utils.byteArrayToIntReversed(block2, 2, 3));
        }
    }

    public PodorozhnikTransitData(ClassicCard card) {
        mSerial = getSerial(card.getTagId());
        mExtraTripTimes = new ArrayList<>();
        decodeSector4(card);
        decodeSector5(card);
    }

    public static Calendar convertDate(int mins) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(PODOROZHNIK_EPOCH);
        g.add(GregorianCalendar.MINUTE, mins);
        return g;
    }

    @Override
    public List<Trip> getTrips() {
        List<Trip> items = new ArrayList<>();
        if (mLastTopupTime != 0) {
            items.add(new PodorozhnikTopup(mLastTopupTime, mLastTopup,
                    mLastTopupAgency, mLastTopupMachine));
        }
        if (mLastTripTime != 0) {
            items.add (new PodorozhnikTrip(mLastTripTime, mLastFare, mLastTransport, mLastValidator));
            for (Integer timestamp : mExtraTripTimes) {
                items.add (new PodorozhnikDetachedTrip(timestamp));
            }
        }
        return items;
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();

	    if (mCountersValid) {
            items.add(new ListItem(R.string.ground_trips,
                    Integer.toString(mGroundCounter)));
            items.add(new ListItem(R.string.subway_trips,
                    Integer.toString(mSubwayCounter)));
        }

        return items;
    }

    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(TransitCurrency.RUB(mBalance),
                Utils.localizeString(R.string.card_name_podorozhnik), null);
    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {
        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            try {
                ClassicSectorKey key = sectors.get(4).getKey();

                Log.d(TAG, "Checking for Podorozhnik key...");
                return Utils.checkKeyHash(key, KEY_SALT, KEY_DIGEST_A, KEY_DIGEST_B) >= 0;
            } catch (IndexOutOfBoundsException ignored) {
                // If that sector number is too high, then it's not for us.
            }
            return false;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(Utils.localizeString(R.string.card_name_podorozhnik),
                    getSerial(card.getTagId()));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new PodorozhnikTransitData(classicCard);
        }

        @Override
        public int earlySectors() {
            return 5;
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };
}
