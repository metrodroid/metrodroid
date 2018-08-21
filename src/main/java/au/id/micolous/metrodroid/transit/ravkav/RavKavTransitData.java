/*
 * RavKavTransitData.java
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

package au.id.micolous.metrodroid.transit.ravkav;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c376n3.lua
// supplemented with personal experimentation
public class RavKavTransitData extends TransitData {
    // 376 = Israel
    private static final int RAVKAV_NETWORK_ID_A = 0x37602;
    private static final int RAVKAV_NETWORK_ID_B = 0x37603;
    private final String mSerial;
    private final int mBalance;
    private final List<RavKavTrip> mTrips;

    // TODO: subscriptions

    public static final Creator<RavKavTransitData> CREATOR = new Creator<RavKavTransitData>() {
        public RavKavTransitData createFromParcel(Parcel parcel) {
            return new RavKavTransitData(parcel);
        }

        public RavKavTransitData[] newArray(int size) {
            return new RavKavTransitData[size];
        }
    };
    private final int mIssueDate;
    private final int mExpiryDate;
    private final int mDateOfBirth;
    private final int mOwnerId;

    private RavKavTransitData(CalypsoApplication card) {
        mSerial = getSerial(card);

        byte []ticketEnv = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData();
        mIssueDate = Utils.getBitsFromBuffer(ticketEnv, 49, 14);
        mExpiryDate = Utils.getBitsFromBuffer(ticketEnv, 63, 14);
        mDateOfBirth = Utils.getBitsFromBuffer(ticketEnv, 80, 32);
        mOwnerId = Utils.getBitsFromBuffer(ticketEnv, 156, 30);

        mBalance = Utils.byteArrayToInt(card.getFile(CalypsoApplication.File.TICKETING_COUNTERS_1).getRecord(1).getData(),
                0, 3);
        mTrips = new ArrayList<>();
        RavKavTrip last = null;
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_LOG).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            RavKavTrip t = new RavKavTrip(record.getData());
            if (t.shouldBeDropped())
                continue;
            RavKavTrip t = new RavKavTrip(record.getData());
            if (last != null && last.shouldBeMerged(t)) {
                last.merge(t);
                continue;
            }
            last = t;
            mTrips.add(t);
        }
    }

    private static String getSerial(CalypsoApplication card) {
        return Long.toString(Utils.byteArrayToLong(card.getTagId()));
    }

    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_ravkav), getSerial(card));
    }

    public static boolean check(CalypsoApplication card) {
        try {
            int networkID = Utils.getBitsFromBuffer(
                    card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData(),
                    3, 20);
            return RAVKAV_NETWORK_ID_A == networkID || RAVKAV_NETWORK_ID_B == networkID;
        } catch (Exception e) {
            return false;
        }
    }

    public static RavKavTransitData parseTransitData(CalypsoApplication card) {
        return new RavKavTransitData(card);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new RavKavTrip[0]);
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(new TransitCurrency(mBalance, "ILS"),
                null,
                RavKavTrip.parseTime(mExpiryDate * 86400));
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_ravkav);
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> li = new ArrayList<>();
        if (mIssueDate != 0)
            li.add(new ListItem(R.string.ravkav_issue_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(RavKavTrip.parseTime(mIssueDate * 86400)))));
        if (mOwnerId == 0)
            li.add(new ListItem(R.string.ravkav_type, R.string.ravkav_anon));
        else {
            li.add(new ListItem(R.string.ravkav_type, R.string.ravkav_personal));
            li.add(new ListItem(R.string.ravkav_owner_id, Integer.toString(mOwnerId)));
        }
        if (mDateOfBirth != 0)
            li.add(new ListItem(R.string.ravkav_date_of_birth, Utils.longDateFormat(parseDOB(mDateOfBirth))));
        return li;
    }

    private static Calendar parseDOB(int dobNumber) {
        GregorianCalendar g = new GregorianCalendar(RavKavTrip.TZ);
        g.set(Utils.convertBCDtoInteger(dobNumber >> 16),
                Utils.convertBCDtoInteger(((dobNumber >> 8) & 0xff) - 1),
                Utils.convertBCDtoInteger(dobNumber & 0xff),
                0, 0, 0);
        return g;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerial);
        dest.writeInt(mBalance);
        dest.writeInt(mIssueDate);
        dest.writeInt(mExpiryDate);
        dest.writeInt(mDateOfBirth);
        dest.writeInt(mOwnerId);

        dest.writeParcelableArray(mTrips.toArray(new RavKavTrip[0]), flags);
    }

    private RavKavTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mBalance = parcel.readInt();
        mIssueDate = parcel.readInt();
        mExpiryDate = parcel.readInt();
        mDateOfBirth = parcel.readInt();
        mOwnerId = parcel.readInt();
        mTrips = Arrays.asList((RavKavTrip[]) parcel.readParcelableArray(RavKavTrip.class.getClassLoader()));
    }
}
