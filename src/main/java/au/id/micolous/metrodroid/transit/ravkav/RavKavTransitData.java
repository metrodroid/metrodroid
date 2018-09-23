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
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.Calypso1545TransitData;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c376n3.lua
// supplemented with personal experimentation
public class RavKavTransitData extends Calypso1545TransitData {
    // 376 = Israel
    private static final int RAVKAV_NETWORK_ID_A = 0x37602;
    private static final int RAVKAV_NETWORK_ID_B = 0x37603;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.ravkav_card)
            .setName(Utils.localizeString(R.string.card_name_ravkav))
            .setLocation(R.string.location_israel)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Parcelable.Creator<RavKavTransitData> CREATOR = new Parcelable.Creator<RavKavTransitData>() {
        public RavKavTransitData createFromParcel(Parcel parcel) {
            return new RavKavTransitData(parcel);
        }

        public RavKavTransitData[] newArray(int size) {
            return new RavKavTransitData[size];
        }
    };

    private static final En1545Container TICKETING_ENV_FIELDS = new En1545Container(
            new En1545FixedInteger("EnvVersionNumber", 3),
            new En1545FixedInteger("EnvNetworkId", 20),
            new En1545FixedInteger("UnknownB", 26),
            En1545FixedInteger.date("EnvApplicationIssue"),
            En1545FixedInteger.date("EnvApplicationValidityEnd"),
            new En1545FixedInteger("PayMethod", 3),
            new En1545FixedInteger("HolderBirthDate", 32),
            new En1545FixedHex("UnknownC", 44),
            new En1545FixedInteger("HolderIdNumber", 30)
    );

    private RavKavTransitData(CalypsoApplication card) {
        super(card, TICKETING_ENV_FIELDS, null, getSerial(card));
    }

    public static String getSerial(CalypsoApplication card) {
        return Long.toString(Utils.byteArrayToLong(card.getTagId()));
    }

    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_ravkav), getSerial(card));
    }

    public static boolean check(byte[] ticketEnv) {
        try {
            int networkID = Utils.getBitsFromBuffer(ticketEnv, 3, 20);
            return RAVKAV_NETWORK_ID_A == networkID || RAVKAV_NETWORK_ID_B == networkID;
        } catch (Exception e) {
            return false;
        }
    }

    public static RavKavTransitData parseTransitData(CalypsoApplication card) {
        return new RavKavTransitData(card);
    }

    @Override
    protected En1545Subscription createSubscription(CalypsoApplication card, byte[] data, En1545Parsed contractList, Integer listNum, int recordNum) {
        byte[] ctr9 = card.getFile(CalypsoApplication.File.TICKETING_COUNTERS_9).getRecord(1).getData();
        return new RavKavSubscription(data, Utils.byteArrayToInt(ctr9, (recordNum - 1) * 3, 3), recordNum);
    }

    @Override
    protected Trip createSpecialEvent(byte[] data) {
        return null;
    }

    @Override
    protected En1545Transaction createTrip(byte[] data) {
        RavKavTransaction t = new RavKavTransaction(data);
        if (t.shouldBeDropped())
            return null;
        return t;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_ravkav);
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> li = new ArrayList<>();
        if (mTicketEnvParsed.getIntOrZero("HolderIdNumber") == 0)
            li.add(new ListItem(R.string.ravkav_type, R.string.ravkav_anon));
        else {
            li.add(new ListItem(R.string.ravkav_type, R.string.ravkav_personal));
        }
        li.addAll(super.getInfo());
        return li;
    }

    @Override
    protected En1545Lookup getLookup() {
        return RavKavLookup.getInstance();
    }

    private RavKavTransitData(Parcel parcel) {
        super(parcel);
    }
}
