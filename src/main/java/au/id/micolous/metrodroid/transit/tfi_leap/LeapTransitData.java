/*
 * LeapTransitData.java
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

package au.id.micolous.metrodroid.transit.tfi_leap;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class LeapTransitData extends TransitData {
    private static final int APP_ID = 0xaf1122;

    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Dublin");

    public static final Parcelable.Creator<LeapTransitData> CREATOR = new Parcelable.Creator<LeapTransitData>() {
        public LeapTransitData createFromParcel(Parcel parcel) {
            return new LeapTransitData(parcel);
        }

        public LeapTransitData[] newArray(int size) {
            return new LeapTransitData[size];
        }
    };

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(R.string.card_name_ie_leap)
            .setLocation(R.string.location_ireland)
            .setCardType(CardType.MifareDesfire)
            .setImageId(R.drawable.leap_card, R.drawable.iso7810_id1_alpha)
            .setExtraNote(R.string.card_note_leap)
            .setPreview()
            .build();

    private static final int BLOCK_SIZE = 0x180;
    private static final long LEAP_EPOCH;
    static final String LEAP_STR = "tfi_leap";
    private boolean mLocked;
    private Calendar mIssueDate;
    private String mSerial;
    private Integer mBalance;
    private Integer mIssuerId;
    private Calendar mInitDate;
    private Calendar mExpiryDate;
    private AccumulatorBlock mDailyAccumulators;
    private AccumulatorBlock mWeeklyAccumulators;
    private List<LeapTrip> mTrips;

    static {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.set(1997,0,1,0,0, 0);
        LEAP_EPOCH = g.getTimeInMillis();
    }

    private static class AccumulatorBlock {
        private final int[] mAccumulators;
        private final int[] mAccumulatorAgencies;
        private final Integer mAccumulatorRegion;
        private final Integer mAccumulatorScheme;
        private final Calendar mAccumulatorStart;

        AccumulatorBlock(byte []file, int offset) {
            mAccumulatorStart = parseDate(file,offset);
            mAccumulatorRegion = (int) file[offset + 4];
            mAccumulatorScheme = Utils.byteArrayToInt(file, offset+5, 3);
            mAccumulatorAgencies = new int[4];
            for (int i = 0; i < 4; i++)
                mAccumulatorAgencies[i] = Utils.byteArrayToInt(file, offset+8+2*i, 2);
            mAccumulators = new int[4];
            for (int i = 0; i < 4; i++)
                mAccumulators[i] = parseBalance(file, offset + 0x10+3*i);
            // 4 bytes hash
        }

        private List<ListItem> getInfo() {
            // Fare cap explanation: https://about.leapcard.ie/fare-capping
            //
            // There are two types of caps:
            // - Daily travel spend
            // - Weekly travel spend
            //
            // There are then two levels of caps:
            // - Single-operator spend (and each operator has different thresholds)
            // - All-operator spend (which applies to the sum of all fares)
            //
            // Certair services are excluded from the caps.
            ArrayList<ListItem> items = new ArrayList<>();

            items.add(new ListItem(
                    R.string.leap_period_start,
                    Utils.dateTimeFormat(TripObfuscator.maybeObfuscateTS(mAccumulatorStart))));
            items.add(new ListItem(R.string.leap_accumulator_region, Integer.toString(mAccumulatorRegion)));
            items.add(new ListItem(R.string.leap_accumulator_total, TransitCurrency.EUR(mAccumulatorScheme).maybeObfuscateBalance().formatCurrencyString(true)));

            for (int i = 0; i < mAccumulators.length; i++) {
        		if (mAccumulators[i] == 0)
		            continue;

        		items.add(new ListItem(
        		        new SpannableString(Utils.localizeString(R.string.leap_accumulator_agency,
                                StationTableReader.getOperatorName(LEAP_STR, mAccumulatorAgencies[i],
                                        false))),
                        TransitCurrency.EUR(mAccumulators[i]).maybeObfuscateBalance().formatCurrencyString(true)
                ));
            }

            return items;
        }
    }

    private static int chooseBlock(byte []file, int txidoffset) {
        int txIdA = Utils.byteArrayToInt(file, txidoffset, 2);
        int txIdB = Utils.byteArrayToInt(file, BLOCK_SIZE+txidoffset, 2);

        if (txIdA > txIdB) {
            return 0;
        }
        return BLOCK_SIZE;
    }

    public LeapTransitData(DesfireCard card) {
        DesfireApplication app = card.getApplication(APP_ID);
        if (app.getFile(2) instanceof UnauthorizedDesfireFile) {
            mLocked = true;
            return;
        }
        mSerial = getSerial(card);
        byte file2[] = app.getFile(2).getData();
        mIssuerId = Utils.byteArrayToInt(file2, 0x22, 3);

        byte file4[] = app.getFile(4).getData();
        mIssueDate = parseDate(file4, 0x22);

        byte file6[] = app.getFile(6).getData();

        int balanceBlock = chooseBlock(file6, 6);
        // 1 byte unknown
        mInitDate = parseDate(file6, balanceBlock + 1);
        mExpiryDate = (Calendar) mInitDate.clone();
        mExpiryDate.add(Calendar.YEAR, 12);
        // 1 byte unknown
        mBalance = parseBalance(file6, balanceBlock + 9);
        // offset: 0xc

        //offset 0x20
        ArrayList<LeapTrip> trips = new ArrayList<>();

        trips.add(LeapTrip.parseTopup(file6, 0x20));
        trips.add(LeapTrip.parseTopup(file6, 0x35));
        trips.add(LeapTrip.parseTopup(file6, 0x20 + BLOCK_SIZE));
        trips.add(LeapTrip.parseTopup(file6, 0x35 + BLOCK_SIZE));

        trips.add(LeapTrip.parsePurseTrip(file6, 0x80));
        trips.add(LeapTrip.parsePurseTrip(file6, 0x90));
        trips.add(LeapTrip.parsePurseTrip(file6, 0x80 + BLOCK_SIZE));
        trips.add(LeapTrip.parsePurseTrip(file6, 0x90 + BLOCK_SIZE));

        // 5 bytes unknown
        // 50 bytes zero
        // offset 0x80

        int capBlock = chooseBlock(file6, 0xa6);
        // offset: 0xa8

        // offset 0x140
        mDailyAccumulators = new AccumulatorBlock(file6, capBlock+0x140);
        mWeeklyAccumulators = new AccumulatorBlock(file6, capBlock+0x160);
        // offset: 0x180

        byte file9[] = app.getFile(9).getData();
        for (int i = 0; i < 7; i++)
            trips.add(LeapTrip.parseTrip(file9, 0x80 * i));

        mTrips = LeapTrip.postprocess(trips);
    }

    @Override
    public List<LeapTrip> getTrips() {
        return mTrips;
    }

    public static int parseBalance(byte[] file, int offset) {
        return Utils.getBitsFromBufferSigned(file, offset * 8, 24);
    }

    public static Calendar parseDate(byte[] file, int offset) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(LEAP_EPOCH);

        int sec = Utils.byteArrayToInt(file, offset, 4);
        g.add(GregorianCalendar.SECOND, sec);
        return g;
    }

    private static String getSerial(DesfireCard card) {
        DesfireApplication app = card.getApplication(APP_ID);
        int serial = Utils.byteArrayToInt(app.getFile(2).getData(),0x25, 4);
        Calendar initDate = parseDate(app.getFile(6).getData(), 1);
        // luhn checksum of number without date is always 6
        int checkDigit = (Utils.calculateLuhn(Integer.toString(serial)) + 6) % 10;
        return Utils.formatNumber(serial, " ", 5 , 4) + checkDigit + " "
                + String.format(Locale.ENGLISH, "%02d%02d",
                initDate.get(Calendar.MONTH) + 1,
                initDate.get(Calendar.YEAR) % 100);
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID);
        }

        @Override
        protected CardInfo getCardInfo() {
            return CARD_INFO;
        }

        @Override
        public TransitData parseTransitData(DesfireCard desfireCard) {
            return new LeapTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(DesfireCard card) {
            try {
                return new TransitIdentity(R.string.card_name_ie_leap, getSerial(card));
            } catch (Exception e) {
                return new TransitIdentity(
                        R.string.locked_leap, null);
            }
        }
    };

    @Nullable
    @Override
    public TransitBalance getBalance() {
        if (mLocked)
            return null;
        return new TransitBalanceStored(TransitCurrency.EUR(mBalance),
                null, mExpiryDate);
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        if (mLocked) {
            items.add(new ListItem(R.string.leap_locked_warning, ""));
            return items;
        }
        items.add(new ListItem(R.string.initialisation_date,
                Utils.dateTimeFormat(TripObfuscator.maybeObfuscateTS(mInitDate))));
        items.add(new ListItem(R.string.issue_date,
                Utils.dateTimeFormat(TripObfuscator.maybeObfuscateTS(mIssueDate))));
        if (MetrodroidApplication.hideCardNumbers()) {
            items.add(new ListItem(R.string.card_issuer, Integer.toString(mIssuerId)));
        }
        items.add(new HeaderListItem(R.string.leap_daily_accumulators));
        items.addAll(mDailyAccumulators.getInfo());

        items.add(new HeaderListItem(R.string.leap_weekly_accumulators));
        items.addAll(mWeeklyAccumulators.getInfo());

        return items;
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @NonNull
    @Override
    public CardInfo getCardInfo() {
        return CARD_INFO;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mSerial);
        parcel.writeInt(mBalance);
        Utils.parcelCalendar(parcel, mInitDate);
        Utils.parcelCalendar(parcel, mExpiryDate);
        parcel.writeInt(mIssuerId);
        parcel.writeTypedList(mTrips);
    }

    private LeapTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mBalance = parcel.readInt();
        mInitDate = Utils.unparcelCalendar(parcel);
        mExpiryDate = Utils.unparcelCalendar(parcel);
        mIssuerId = parcel.readInt();
        mTrips = new ArrayList<>();
        parcel.readTypedList(mTrips, LeapTrip.CREATOR);
    }

    public static boolean earlyCheck(int appId) {
        return appId == APP_ID;
    }

    @Nullable
    public static String getNotice() {
        return StationTableReader.getNotice(LEAP_STR);
    }
}
