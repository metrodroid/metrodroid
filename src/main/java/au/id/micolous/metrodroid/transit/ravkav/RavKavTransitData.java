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
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
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
import au.id.micolous.metrodroid.util.ImmutableByteArray;

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c376n3.lua
// supplemented with personal experimentation
public class RavKavTransitData extends Calypso1545TransitData {
    // 376 = Israel
    private static final int RAVKAV_NETWORK_ID_A = 0x37602;
    private static final int RAVKAV_NETWORK_ID_B = 0x37603;

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.ravkav_card)
            .setName(Localizer.INSTANCE.localizeString(R.string.card_name_ravkav))
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
            new En1545FixedInteger(ENV_VERSION_NUMBER, 3),
            new En1545FixedInteger(ENV_NETWORK_ID, 20),
            new En1545FixedInteger(ENV_UNKNOWN_A, 26),
            En1545FixedInteger.date(ENV_APPLICATION_ISSUE),
            En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
            new En1545FixedInteger("PayMethod", 3),
            new En1545FixedInteger(HOLDER_BIRTH_DATE, 32),
            new En1545FixedHex(ENV_UNKNOWN_B, 44),
            new En1545FixedInteger(HOLDER_ID_NUMBER, 30)
    );

    private RavKavTransitData(CalypsoApplication card) {
        super(card, TICKETING_ENV_FIELDS, null, getSerial(card));
    }

    private static String getSerial(CalypsoApplication card) {
        ImmutableByteArray appFci = card.getAppFci();
        if (appFci == null)
            return null;
        ImmutableByteArray a5 = ISO7816TLV.INSTANCE.findBERTLV(appFci, "a5", true);
                if (a5 == null)
        return null;
        ImmutableByteArray bf0c = ISO7816TLV.INSTANCE.findBERTLV(a5, "bf0c", true);
        if (bf0c == null)
            return null;
        ImmutableByteArray c7 = ISO7816TLV.INSTANCE.findBERTLV(bf0c, "c7", true);
        if (c7 == null)
            return null;
        return Long.toString(c7.byteArrayToLong(4, 4));
    }

    public final static CalypsoCardTransitFactory FACTORY = new CalypsoCardTransitFactory() {
        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull CalypsoApplication card) {
            return new TransitIdentity(Localizer.INSTANCE.localizeString(R.string.card_name_ravkav), getSerial(card));
        }

        @Override
        public boolean check(ImmutableByteArray ticketEnv) {
            try {
                int networkID = ticketEnv.getBitsFromBuffer(3, 20);
                return RAVKAV_NETWORK_ID_A == networkID || RAVKAV_NETWORK_ID_B == networkID;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public CardInfo getCardInfo(ImmutableByteArray tenv) {
            return CARD_INFO;
        }

        @Override
        public RavKavTransitData parseTransitData(@NonNull CalypsoApplication card) {
            return new RavKavTransitData(card);
        }
    };

    @Override
    protected En1545Subscription createSubscription(ImmutableByteArray data, En1545Parsed contractList,
                                                    Integer listNum, int recordNum, Integer counter) {
        return new RavKavSubscription(data, counter);
    }

    @Override
    protected En1545Transaction createTrip(ImmutableByteArray data) {
        RavKavTransaction t = new RavKavTransaction(data);
        if (t.shouldBeDropped())
            return null;
        return t;
    }

    @Override
    public String getCardName() {
        return Localizer.INSTANCE.localizeString(R.string.card_name_ravkav);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = new ArrayList<>();
        if (mTicketEnvParsed.getIntOrZero(HOLDER_ID_NUMBER) == 0) {
            li.add(new ListItem(R.string.card_type, R.string.card_type_anonymous));
        } else {
            li.add(new ListItem(R.string.card_type, R.string.card_type_personal));
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
