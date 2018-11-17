/*
 * TmoneyTransitData.java
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

package au.id.micolous.metrodroid.transit.tmoney;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public class TMoneyTransitData extends TransitData {
    public static final Parcelable.Creator<TMoneyTransitData> CREATOR = new Parcelable.Creator<TMoneyTransitData>() {
        public TMoneyTransitData createFromParcel(Parcel parcel) {
            return new TMoneyTransitData(parcel);
        }

        public TMoneyTransitData[] newArray(int size) {
            return new TMoneyTransitData[size];
        }
    };

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.tmoney_card)
            .setName(Utils.localizeString(R.string.card_name_tmoney))
            .setLocation(R.string.location_seoul)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    private final String mSerialNumber;
    private final int mBalance;
    private final String mDate;
    private final List<TMoneyTrip> mTrips;

    public TMoneyTransitData(TMoneyCard tMoneyCard) {
        super();
        mSerialNumber = parseSerial(tMoneyCard);
        mBalance = tMoneyCard.getBalance();
        mDate = parseDate(tMoneyCard);
        mTrips = new ArrayList<>();
        for (ISO7816Record record : tMoneyCard.getFile(ISO7816Selector.makeSelector(TMoneyCard.FILE_NAME, 4)).getRecords()) {
            TMoneyTrip t = TMoneyTrip.parseTrip(record.getData());
            if (t == null)
                continue;
            mTrips.add(t);
        }
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return TransitCurrency.KRW(mBalance);
    }


    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_tmoney);
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new ListItem(R.string.tmoney_date, mDate));

        return items;
    }

    @Override
    public List<TMoneyTrip> getTrips() {
        return mTrips;
    }

    @SuppressWarnings("UnusedDeclaration")
    public TMoneyTransitData(Parcel p) {
        mSerialNumber = p.readString();
        mBalance = p.readInt();
        mDate = p.readString();
        mTrips = Arrays.asList((TMoneyTrip[]) p.readParcelableArray(TMoneyTrip.class.getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerialNumber);
        dest.writeInt(mBalance);
        dest.writeString(mDate);
        dest.writeParcelableArray(mTrips.toArray(new TMoneyTrip[0]), flags);
    }

    public static TransitIdentity parseTransitIdentity(TMoneyCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_tmoney), parseSerial(card));
    }

    private static byte[] getSerialTag(TMoneyCard card) {
        return ISO7816Application.findBERTLV(card.getAppData(), 5, 0x10, false);
    }

    private static String parseSerial(TMoneyCard card) {
        return Utils.groupString(Utils.getHexString(getSerialTag(card), 4, 8), " ", 4, 4, 4);
    }

    private static String parseDate(TMoneyCard card) {
        byte []tmoneytag = getSerialTag(card);
        return Utils.getHexString(tmoneytag, 17, 2) + "/"
                + Utils.getHexString(tmoneytag, 19, 1);
    }
}
