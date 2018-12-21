/*
 * OpalTransitData.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.opal;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

/**
 * Transit data type for Opal (Sydney, AU).
 * <p>
 * This uses the publicly-readable file on the card (7) in order to get the data.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Opal
 */
public class OpalTransitData extends TransitData {
    public static final String NAME = "Opal";
    public static final int APP_ID = 0x314553;
    public static final int FILE_ID = 0x7;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.opal_card)
            .setName(OpalTransitData.NAME)
            .setLocation(R.string.location_sydney_australia)
            .setCardType(CardType.MifareDesfire)
            .setExtraNote(R.string.card_note_opal)
            .build();

    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Australia/Sydney");
    private static final GregorianCalendar OPAL_EPOCH;

    public static final Creator<OpalTransitData> CREATOR = new Creator<OpalTransitData>() {
        public OpalTransitData createFromParcel(Parcel parcel) {
            return new OpalTransitData(parcel);
        }

        public OpalTransitData[] newArray(int size) {
            return new OpalTransitData[size];
        }
    };

    static {
        GregorianCalendar epoch = new GregorianCalendar(TIME_ZONE);
        epoch.set(Calendar.YEAR, 1980);
        epoch.set(Calendar.MONTH, Calendar.JANUARY);
        epoch.set(Calendar.DAY_OF_MONTH, 1);
        epoch.set(Calendar.HOUR_OF_DAY, 0);
        epoch.set(Calendar.MINUTE, 0);
        epoch.set(Calendar.SECOND, 0);
        epoch.set(Calendar.MILLISECOND, 0);

        OPAL_EPOCH = epoch;
    }

    private final int mSerialNumber;
    private final int mBalance; // cents
    private final int mChecksum;
    private final int mWeeklyTrips;
    private final boolean mAutoTopup;
    private final int mAction;
    private final int mMode;
    private final int mMinute;
    private final int mDay;
    private final int mTransactionNumber;
    private final int mLastDigit;

    @SuppressWarnings("UnusedDeclaration")
    public OpalTransitData(Parcel parcel) {
        mSerialNumber = parcel.readInt();
        mBalance = parcel.readInt();
        mChecksum = parcel.readInt();
        mWeeklyTrips = parcel.readInt();
        mAutoTopup = parcel.readByte() == 0x01;
        mAction = parcel.readInt();
        mMode = parcel.readInt();
        mMinute = parcel.readInt();
        mDay = parcel.readInt();
        mTransactionNumber = parcel.readInt();
        mLastDigit = parcel.readInt();
    }

    private OpalTransitData(DesfireCard desfireCard) {
        byte[] data = desfireCard.getApplication(APP_ID).getFile(FILE_ID).getData();

        data = Utils.reverseBuffer(data, 0, 16);

        try {
            mChecksum = Utils.getBitsFromBuffer(data, 0, 16);
            mWeeklyTrips = Utils.getBitsFromBuffer(data, 16, 4);
            mAutoTopup = Utils.getBitsFromBuffer(data, 20, 1) == 0x01;
            mAction = Utils.getBitsFromBuffer(data, 21, 4);
            mMode = Utils.getBitsFromBuffer(data, 25, 3);
            mMinute = Utils.getBitsFromBuffer(data, 28, 11);
            mDay = Utils.getBitsFromBuffer(data, 39, 15);
            mBalance = Utils.getBitsFromBufferSigned(data, 54, 21);
            mTransactionNumber = Utils.getBitsFromBuffer(data, 75, 16);
            // Skip bit here
            mLastDigit = Utils.getBitsFromBuffer(data, 92, 4);
            mSerialNumber = Utils.getBitsFromBuffer(data, 96, 32);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Opal data", ex);
        }
    }

    private static String formatSerialNumber(int serialNumber, int lastDigit) {
        return String.format(Locale.ENGLISH, "308522%09d%01d", serialNumber, lastDigit);
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new OpalTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard desfireCard) {
            byte[] data = desfireCard.getApplication(APP_ID).getFile(FILE_ID).getData();
            data = Utils.reverseBuffer(data, 0, 5);

            int lastDigit = Utils.getBitsFromBuffer(data, 4, 4);
            int serialNumber = Utils.getBitsFromBuffer(data, 8, 32);
            return new TransitIdentity(NAME, formatSerialNumber(serialNumber, lastDigit));
        }
    };

    @Override
    public String getCardName() {
        return NAME;
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return TransitCurrency.AUD(mBalance);
    }

    @Override
    public String getSerialNumber() {
        return formatSerialNumber(mSerialNumber, mLastDigit);
    }

    public Calendar getLastTransactionTime() {
        Calendar cLastTransaction = new GregorianCalendar(TIME_ZONE);
        cLastTransaction.setTimeInMillis(OPAL_EPOCH.getTimeInMillis());
        cLastTransaction.add(Calendar.DATE, mDay);

        // Time is set this way, as in Opal all days have 24 hours, even when there is a DST
        // transition.
        cLastTransaction.set(Calendar.HOUR_OF_DAY, mMinute / 60); // floor-divide
        cLastTransaction.set(Calendar.MINUTE, mMinute % 60);
        return cLastTransaction;
    }

    /**
     * Gets the last mode of travel.
     *
     * Valid values are in OpalData.MODE_*. This does not use the Mode class, due to the merger
     * of Ferry and Light Rail travel.
     */
    public int getLastTransactionMode() {
        return mMode;
    }

    /**
     * Gets the last action performed on the Opal card.
     *
     * Valid values are in OpalData.ACTION_*.
     */
    public int getLastTransaction() {
        return mAction;
    }

    /**
     * Gets the number of weekly trips taken on this Opal card. Maxes out at 15 trips.
     */
    public int getWeeklyTrips() {
        return mWeeklyTrips;
    }

    /**
     * Gets the serial number of the latest transaction on the Opal card.
     */
    public int getLastTransactionNumber() {
        return mTransactionNumber;
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.opal_weekly_trips, Integer.toString(getWeeklyTrips())));
        if (!MetrodroidApplication.hideCardNumbers()) {
            items.add(new ListItem(R.string.checksum, Integer.toString(mChecksum)));
        }

        items.add(new HeaderListItem(R.string.last_transaction));
        if (!MetrodroidApplication.hideCardNumbers()) {
            items.add(new ListItem(R.string.transaction_counter, Integer.toString(getLastTransactionNumber())));
        }
        Calendar cLastTransactionTime = TripObfuscator.maybeObfuscateTS(getLastTransactionTime());
        items.add(new ListItem(R.string.date, Utils.longDateFormat(cLastTransactionTime)));
        items.add(new ListItem(R.string.time, Utils.timeFormat(cLastTransactionTime)));
        items.add(new ListItem(R.string.vehicle_type, OpalData.getLocalisedMode(getLastTransactionMode())));
        items.add(new ListItem(R.string.transaction_type, OpalData.getLocalisedAction(getLastTransaction())));

        return items;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerialNumber);
        parcel.writeInt(mBalance);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mWeeklyTrips);
        parcel.writeByte((byte) (mAutoTopup ? 0x01 : 0x00));
        parcel.writeInt(mAction);
        parcel.writeInt(mMode);
        parcel.writeInt(mMinute);
        parcel.writeInt(mDay);
        parcel.writeInt(mTransactionNumber);
        parcel.writeInt(mLastDigit);
    }

    @Override
    public List<Subscription> getSubscriptions() {
        // Opal has no concept of "subscriptions" (travel pass), only automatic top up.
        if (mAutoTopup) {
            return Collections.singletonList(OpalSubscription.getInstance());
        }
        return Collections.emptyList();
    }

    @Override
    public Uri getOnlineServicesPage() {
        return Uri.parse("https://m.opal.com.au/");
    }
}
