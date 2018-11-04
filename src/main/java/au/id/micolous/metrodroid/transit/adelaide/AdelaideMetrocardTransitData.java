/*
 * AdelaideMetrocardTransitData.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.adelaide;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData;
import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class AdelaideMetrocardTransitData extends En1545TransitData {
    public static final Parcelable.Creator<AdelaideMetrocardTransitData> CREATOR = new Parcelable.Creator<AdelaideMetrocardTransitData>() {
        public AdelaideMetrocardTransitData createFromParcel(Parcel parcel) {
            return new AdelaideMetrocardTransitData(parcel);
        }

        public AdelaideMetrocardTransitData[] newArray(int size) {
            return new AdelaideMetrocardTransitData[size];
        }
    };

    private static final int APP_ID = 0xb006f2;
    private static final String NAME = "Metrocard (Adelaide)";
    private final List<TransactionTrip> mTrips;
    private final List<AdelaideSubscription> mSubs;
    private final AdelaideSubscription mPurse;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_adelaide)
            .setCardType(CardType.MifareDesfire)
            .setExtraNote(R.string.card_note_adelaide)
            .setPreview()
            .build();

    private final long mSerial;

    public AdelaideMetrocardTransitData(DesfireCard card) {
        mSerial = getSerial(card.getTagId());
        DesfireApplication app = card.getApplication(APP_ID);

        // This is basically mapped from Intercode
        // 0 = TICKETING_ENVIRONMENT
        mTicketEnvParsed.append(app.getFile(0).getData(), IntercodeTransitData.TICKET_ENV_FIELDS);

        // 1 is 0-record file on all cards we've seen so far

        // 2 = TICKETING_CONTRACT_LIST, not really useful to use

        List<Transaction> transactionList = new ArrayList<>();

        // 3-6: TICKETING_LOG
        // 7: rotating pointer for log
        // 8 is "HID ADELAIDE" or "NoteAB ADELAIDE"
        // 9-0xb: TICKETING_SPECIAL_EVENTS
        for (int fileId : new int[]{3,4,5,6, 9, 0xa, 0xb}) {
            byte[] data = app.getFile(fileId).getData();
            if (Utils.getBitsFromBuffer(data, 0, 14) == 0)
                continue;
            transactionList.add(new AdelaideTransaction(data));
        }

        // c-f: locked counters
        mSubs = new ArrayList<>();
        AdelaideSubscription purse = null;
        // 10-13: contracts
        for (int fileId : new int[]{0x10, 0x11, 0x12, 0x13}) {
            byte[] data = app.getFile(fileId).getData();
            if (Utils.getBitsFromBuffer(data, 0, 7) == 0)
                continue;
            AdelaideSubscription sub = new AdelaideSubscription(data);
            if (sub.isPurse())
                purse = sub;
            else
                mSubs.add(sub);
        }

        mPurse = purse;

        // 14-17: zero-filled
        // 1b-1c: locked
        // 1d: empty
        // 1e: const

        mTrips = TransactionTrip.merge(transactionList);
    }

    @SuppressWarnings("unchecked")
    private AdelaideMetrocardTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readLong();
        mTrips = parcel.readArrayList(AdelaideTransaction.class.getClassLoader());
        mSubs = parcel.readArrayList(AdelaideTransaction.class.getClassLoader());
        if (parcel.readInt() != 0)
            mPurse = new AdelaideSubscription(parcel);
        else
            mPurse = null;
    }

    @Override
    protected En1545Lookup getLookup() {
        return AdelaideLookup.getInstance();
    }

    public static boolean check(DesfireCard card) {
        return card.getApplication(APP_ID) != null;
    }

    public static TransitIdentity parseTransitIdentity(DesfireCard card) {
        return new TransitIdentity(NAME, formatSerial(getSerial(card.getTagId())));
    }

    private static String formatSerial(long serial) {
        return "01-" + Utils.formatNumber(serial, " ", 3, 4, 4, 4);
    }

    private static long getSerial(byte[] tagId) {
        return Utils.byteArrayToLongReversed(tagId, 1, 6);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeLong(mSerial);
        parcel.writeList(mTrips);
        parcel.writeList(mSubs);
        parcel.writeInt(mPurse != null ? 1 : 0);
        if (mPurse != null)
            mPurse.writeToParcel(parcel, i);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new Trip[0]);
    }

    @Override
    public Subscription[] getSubscriptions() {
        if (mSubs.isEmpty())
            return null;
        return mSubs.toArray(new Subscription[0]);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = super.getInfo();
        if (mPurse != null) {
            if (mPurse.getMachineId() != null) {
                items.add(new ListItem(R.string.machine_id,
                        Integer.toString(mPurse.getMachineId())));
            }

            Calendar purchaseTS = mPurse.getPurchaseTimestamp();
            if (purchaseTS != null) {
                purchaseTS = TripObfuscator.maybeObfuscateTS(purchaseTS);

                items.add(new ListItem(R.string.issue_date, Utils.dateFormat(purchaseTS)));
            }

            Integer purseId = mPurse.getId();
            if (purseId != null)
                items.add(new ListItem(R.string.purse_serial_number, Integer.toHexString(purseId)));
        }
        return items;
    }

    public static boolean earlyCheck(int[] appIds) {
        return ArrayUtils.contains(appIds, APP_ID);
    }
}
