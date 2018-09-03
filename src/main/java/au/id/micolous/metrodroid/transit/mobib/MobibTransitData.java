/*
 * MobibTransitData.java
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

package au.id.micolous.metrodroid.transit.mobib;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.calypso.CalypsoData;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedString;
import au.id.micolous.metrodroid.transit.en1545.En1545Parser;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/*
 * Reference:
 * - https://github.com/zoobab/mobib-extractor
 */
public class MobibTransitData extends TransitData {
    // 56 = Belgium
    private static final int MOBIB_NETWORK_ID = 0x56001;
    public static final String NAME = "Mobib";
    private final String mSerial;
    private final int mExpiry;
    private final int mPurchase;
    private final String mHolder;
    private final int mHolderType;
    private final int mDateOfBirth;
    private final int mZipCode;
    private final int mTotalTrips;
    private final List<MobibSubscription> mSubscriptions;
    private final List<MobibTrip> mTrips;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(MobibTransitData.NAME)
            .setCardType(CardType.ISO7816)
            .setLocation(R.string.location_brussels)
            .build();

    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Bruxelles");
    private static final long MOBIB_EPOCH = CalypsoData.TRAVEL_EPOCH.getTimeInMillis();

    public static final Creator<MobibTransitData> CREATOR = new Creator<MobibTransitData>() {
        @NonNull
        public MobibTransitData createFromParcel(Parcel parcel) {
            return new MobibTransitData(parcel);
        }

        @NonNull
        public MobibTransitData[] newArray(int size) {
            return new MobibTransitData[size];
        }
    };


    private MobibTransitData(CalypsoApplication card) {
        ISO7816File holderFile = card.getFile(CalypsoApplication.File.HOLDER_EXTENDED);
        byte[] holder = Utils.concatByteArrays(holderFile.getRecord(1).getData(),
                holderFile.getRecord(2).getData());
        mHolderType = Utils.getBitsFromBuffer(holder, 200, 2);
        mHolder = En1545FixedString.parseString(holder, 205, holder.length * 8 - 205);
        mDateOfBirth = Utils.byteArrayToInt(holder, 21, 4);
        mSerial = getSerial(card);
        mPurchase = Utils.getBitsFromBuffer(card.getFile(CalypsoApplication.File.EP_LOAD_LOG)
                        .getRecord(1).getData(),
                2, 14);
        byte ticketEnv[] = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                .getRecord(1).getData();
        mExpiry = Utils.getBitsFromBuffer(ticketEnv,
                45, 14);
        mZipCode = Utils.getBitsFromBuffer(ticketEnv, 22 * 8 + 3, 14);
        mTrips = new ArrayList<>();
        int totalTrips = 0;
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_LOG).getRecords()) {
            int tripCtr = Utils.getBitsFromBuffer(record.getData(), 17 * 8 + 3,
                    23);
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            mTrips.add(new MobibTrip(record.getData()));
            if (totalTrips < tripCtr)
                totalTrips = tripCtr;
        }
        mTotalTrips = totalTrips;
        mSubscriptions = new ArrayList<>();
        int i = 0;
        byte[] ctr9 = card.getFile(CalypsoApplication.File.TICKETING_COUNTERS_9).getRecord(1).getData();
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            mSubscriptions.add(new MobibSubscription(record.getData(),
                    Utils.byteArrayToInt(ctr9, (record.getIndex() - 1) * 3, 3),
                    i++));
        }
    }

    public static Calendar parseTime(int d, int t) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(MOBIB_EPOCH);
        g.add(Calendar.DAY_OF_YEAR, d);
        g.add(Calendar.MINUTE, t);
        return g;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList <ListItem> li = new ArrayList<>();
        if (mExpiry != 0)
            li.add(new ListItem(R.string.opus_card_expiry_date,
                    Utils.longDateFormat(parseTime(mExpiry, 0))));
        if (mPurchase != 0)
            li.add(new ListItem(R.string.mobib_card_purchase_date,
                    Utils.longDateFormat(parseTime(mPurchase, 0))));
        li.add(new ListItem(R.string.mobib_total_trips, Integer.toString(mTotalTrips)));
        if (mHolderType == 0) {
            li.add(new ListItem(R.string.mobib_card_type, R.string.mobib_anon));
        } else {
            li.add(new ListItem(R.string.mobib_card_type, R.string.mobib_personal));
        }
        if (mHolderType != 0 && !MetrodroidApplication.hideCardNumbers()
                && !MetrodroidApplication.obfuscateTripDates()) {
            li.add(new ListItem(R.string.mobib_card_holder,
                    mHolder));
            switch (mHolderType) {
                case 1:
                    li.add(new ListItem(R.string.mobib_card_holder_gender,
                            R.string.mobib_card_holder_male));
                    break;
                case 2:
                    li.add(new ListItem(R.string.mobib_card_holder_gender,
                            R.string.mobib_card_holder_female));
                    break;
                default:
                    li.add(new ListItem(R.string.mobib_card_holder_gender,
                            Integer.toHexString(mHolderType)));
                    break;
            }
            li.add(new ListItem(R.string.mobib_card_dob,
                    Utils.longDateFormat(En1545Parser.parseBCDDate(mDateOfBirth, TZ))));
            li.add(new ListItem(R.string.mobib_card_zip, Integer.toString(mZipCode)));
        }
        return li;
    }

    private static String getSerial(CalypsoApplication card) {
        byte[] holder = card.getFile(CalypsoApplication.File.HOLDER_EXTENDED).getRecord(1).getData();
        return String.format(Locale.ENGLISH,
                "%06d%06d%06d%01d",
                Utils.convertBCDtoInteger(Utils.getBitsFromBuffer(holder, 18, 24)),
                Utils.convertBCDtoInteger(Utils.getBitsFromBuffer(holder, 42, 24)),
                Utils.convertBCDtoInteger(Utils.getBitsFromBuffer(holder, 66, 24)),
                Utils.convertBCDtoInteger(Utils.getBitsFromBuffer(holder, 90, 4)));
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        return new TransitIdentity(NAME, getSerial(card));
    }

    public static boolean check(CalypsoApplication card) {
        try {
            return MOBIB_NETWORK_ID == Utils.getBitsFromBuffer(card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData(),
                    13, 24);
        } catch (Exception e) {
            return false;
        }
    }

    @NonNull
    public static MobibTransitData parseTransitData(CalypsoApplication card) {
        return new MobibTransitData(card);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new MobibTrip[0]);
    }

    @Override
    public Subscription[] getSubscriptions() {
        return mSubscriptions.toArray(new MobibSubscription[0]);
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
        dest.writeInt(mPurchase);
        dest.writeString(mHolder);
        dest.writeInt(mHolderType);
        dest.writeInt(mDateOfBirth);
        dest.writeInt(mZipCode);
        dest.writeInt(mTotalTrips);
        dest.writeParcelableArray(mTrips.toArray(new MobibTrip[0]), flags);
        dest.writeParcelableArray(mSubscriptions.toArray(new MobibSubscription[0]), flags);
    }

    private MobibTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mExpiry = parcel.readInt();
        mPurchase = parcel.readInt();
        mHolder = parcel.readString();
        mHolderType = parcel.readInt();
        mDateOfBirth = parcel.readInt();
        mZipCode = parcel.readInt();
        mTotalTrips = parcel.readInt();
        mTrips = Arrays.asList((MobibTrip[]) parcel.readParcelableArray(MobibTrip.class.getClassLoader()));
        mSubscriptions = Arrays.asList((MobibSubscription[]) parcel.readParcelableArray(MobibSubscription.class.getClassLoader()  ));
    }
}
