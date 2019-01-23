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

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV;
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

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
            .setName(Localizer.INSTANCE.localizeString(R.string.card_name_tmoney))
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
        for (ISO7816Record record : tMoneyCard.getTransactionRecords()) {
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
        return Localizer.INSTANCE.localizeString(R.string.card_name_tmoney);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();

        items.add(new ListItem(R.string.tmoney_date, mDate));

        return items;
    }

    @Override
    public List<TMoneyTrip> getTrips() {
        return mTrips;
    }

    private TMoneyTransitData(Parcel p) {
        mSerialNumber = p.readString();
        mBalance = p.readInt();
        mDate = p.readString();
        //noinspection unchecked
        mTrips = p.readArrayList(TMoneyTrip.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerialNumber);
        dest.writeInt(mBalance);
        dest.writeString(mDate);
        dest.writeList(mTrips);
    }

    public static TransitIdentity parseTransitIdentity(TMoneyCard card) {
        return new TransitIdentity(Localizer.INSTANCE.localizeString(R.string.card_name_tmoney), parseSerial(card));
    }

    private static ImmutableByteArray getSerialTag(TMoneyCard card) {
        return ISO7816TLV.INSTANCE.findBERTLV(card.getAppData(), "b0", false);
    }

    private static String parseSerial(TMoneyCard card) {
        return NumberUtils.INSTANCE.groupString(getSerialTag(card).getHexString(4, 8), " ", 4, 4, 4);
    }

    @NonNls
    private static String parseDate(TMoneyCard card) {
        ImmutableByteArray tmoneytag = getSerialTag(card);
        return tmoneytag.getHexString(17, 2) + "/"
                + tmoneytag.getHexString(19, 1);
    }
}
