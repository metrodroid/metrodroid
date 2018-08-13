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
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
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

    public static final String NAME = "Podorozhnik";

    // We don't want to actually include these keys in the program, so include a hashed version of
    // this key.
    private static final String KEY_SALT = "podorozhnik";
    // md5sum of Salt + Common Key + Salt, used on sector 4.
    private static final String KEY_DIGEST = "f3267ff451b1fc3076ba12dcee2bf803";

    public static final Parcelable.Creator<PodorozhnikTransitData> CREATOR = new Parcelable.Creator<PodorozhnikTransitData>() {
        public PodorozhnikTransitData createFromParcel(Parcel parcel) {
            return new PodorozhnikTransitData(parcel);
        }

        public PodorozhnikTransitData[] newArray(int size) {
            return new PodorozhnikTransitData[size];
        }
    };

    private static final long PODOROZHNIK_EPOCH;
    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Moscow");

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(2010, Calendar.JANUARY, 1, 0, 0, 0);

        PODOROZHNIK_EPOCH = epoch.getTimeInMillis();
    }

    private static final String TAG = "PodorozhnikTransitData";

    private int mBalance;
    private Integer mLastTopup;
    private Integer mLastTopupTime;
    private Integer mLastSpend;
    private Integer mLastSpendTime;
    private Integer mLastValidator;
    private Integer mLastTripTime;
    private Integer mGroundCounter;
    private Integer mSubwayCounter;
    private Integer mLastTransport;
    private String mSerial;

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
	    dest.writeInt(mLastSpend);
	    dest.writeInt(mLastSpendTime);
	    dest.writeInt(mLastTripTime);
	    dest.writeInt(mLastValidator);
	    dest.writeInt(mGroundCounter);
	    dest.writeInt(mSubwayCounter);
	    dest.writeString(mSerial);
    }

    @SuppressWarnings("UnusedDeclaration")
    public PodorozhnikTransitData(Parcel p) {
        mBalance = p.readInt();
    	mLastTopup = p.readInt();
	    mLastTopupTime = p.readInt();
	    mLastSpend = p.readInt();
	    mLastSpendTime = p.readInt();
	    mLastTripTime = p.readInt();
	    mLastValidator = p.readInt();
	    mGroundCounter = p.readInt();
	    mSubwayCounter = p.readInt();
	    mSerial = p.readString();
    }

    private static String getSerial(byte []uid) {
        String sn;StringBuilder pretty = new StringBuilder();
        sn = String.format(Locale.ENGLISH, "96433078%017d",
                Utils.byteArrayToLong(Utils.reverseBuffer(uid, 0, 7)));
        sn += Utils.calculateLuhn(sn);// last digit is luhn
        for (int i = 0; i < 6; i++)
            pretty.append(sn.substring(i * 4, i * 4 + 4)).append(" ");
        pretty.append(sn.substring(24, 26));
        return pretty.toString();

    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_podorozhnik),
                getSerial(card.getTagId()));
    }

    private void decodeSector4(ClassicCard card) {
        ClassicSector sector4 = card.getSector(4);

	    if (sector4 instanceof UnauthorizedClassicSector)
	        return;

	    // Block 0 and block 1 are copies. Let's use block 0
	    byte[] block0 = sector4.getBlock(0).getData();
	    byte[] block2 = sector4.getBlock(2).getData();
	    byte[] b;
	    b = Utils.reverseBuffer(block0, 0, 4);
        mBalance = Utils.byteArrayToInt(b, 0, 4);
	    b = Utils.reverseBuffer(block2, 8, 3);
	    mLastTopup = Utils.byteArrayToInt(b, 0, 3);
	    b = Utils.reverseBuffer(block2, 2, 3);
	    mLastTopupTime = Utils.byteArrayToInt(b, 0, 3);
    }	

    private void decodeSector5(ClassicCard card) {
        ClassicSector sector5 = card.getSector(5);

	    if (sector5 instanceof UnauthorizedClassicSector)
	        return;

	    // Block 1 and block 2 are copies. Let's use block 2
	    byte[] block0 = sector5.getBlock(0).getData();
	    byte[] block1 = sector5.getBlock(1).getData();
	    byte[] b;

	    b = Utils.reverseBuffer(block0, 6, 4);
	    mLastSpend = Utils.byteArrayToInt(b, 0, 4);
	    b = Utils.reverseBuffer(block0, 0, 3);
	    mLastSpendTime = Utils.byteArrayToInt(b, 0, 3);
	    mLastTransport = block0[3] & 0xff;
	    b = Utils.reverseBuffer(block0, 4, 2);
	    mLastValidator = Utils.byteArrayToInt(b, 0, 2);
	    b = Utils.reverseBuffer(block1, 2, 3);
	    mLastTripTime = Utils.byteArrayToInt(b, 0, 3);
	    mSubwayCounter = block1[0] & 0xff;
	    mGroundCounter = block1[1] & 0xff;
    }	

    public PodorozhnikTransitData(ClassicCard card) {
        mSerial = getSerial(card.getTagId());
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
    public Trip[] getTrips() {
        ArrayList<Trip> items = new ArrayList<>();
        if (mLastTopup != null && mLastTopup != 0)
            items.add (new PodorozhnikTrip(mLastTopupTime, -mLastTopup, Trip.Mode.TICKET_MACHINE,
                    null, Utils.localizeString(R.string.podorozhnik_topup)));
        if (mLastTripTime != null && mLastTripTime != 0) {
            if (mLastSpendTime.equals(mLastTripTime))
                items.add (new PodorozhnikTrip(mLastSpendTime, mLastSpend, guessMode(mLastTransport), mLastValidator,
                        guessAgency(mLastTransport)));
            else {
                items.add (new PodorozhnikTrip(mLastTripTime, null, guessMode(mLastTransport), mLastValidator,
                        guessAgency(mLastTransport)));
                items.add (new PodorozhnikTrip(mLastSpendTime, mLastSpend, Trip.Mode.OTHER, null, null));
            }
        }
        return items.toArray(new Trip[0]);
    }

    private static Trip.Mode guessMode(int lastTransport) {
        if (lastTransport == 1)
            return Trip.Mode.METRO;
        return Trip.Mode.BUS;
        // TODO: Handle trams
    }

    private static String guessAgency(int lastTransport) {
        // Always include "Saint Petersburg" in names here to distinguish from Troika (Moscow)
        // trips on hybrid cards
        if (lastTransport == 1)
            return Utils.localizeString(R.string.led_metro);
        return Utils.localizeString(R.string.led_bus);
        // TODO: Handle trams
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

	    if (mGroundCounter != null)
	        items.add(new ListItem(R.string.ground_trips,
                "" + mGroundCounter));
	    if (mSubwayCounter != null)
	        items.add(new ListItem(R.string.subway_trips,
                "" + mSubwayCounter));

        return items;
    }

    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(TransitCurrency.RUB(mBalance),
                Utils.localizeString(R.string.card_name_podorozhnik), null);
    }

    public static boolean check(ClassicCard card) {
        try {
            byte[] key = card.getSector(4).getKey();
            if (key == null || key.length != 6) {
                // We don't have key data, bail out.
                return false;
            }

            Log.d(TAG, "Checking for Podorozhnik key...");
            return Utils.checkKeyHash(key, KEY_SALT, KEY_DIGEST) >= 0;
        } catch (IndexOutOfBoundsException ignored) {
            // If that sector number is too high, then it's not for us.
        }
        return false;
    }
}
