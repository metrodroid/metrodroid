/*
 * TmoneyCard.java
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
import android.text.Spanned;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card;
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public class TMoneyTransitData extends TransitData {
    public static final String NAME = "T-Money";
    public static final String LONG_NAME = "T-Money card";
    public static final Parcelable.Creator<TMoneyTransitData> CREATOR = new Parcelable.Creator<TMoneyTransitData>() {
        public TMoneyTransitData createFromParcel(Parcel parcel) {
            return new TMoneyTransitData(parcel);
        }

        public TMoneyTransitData[] newArray(int size) {
            return new TMoneyTransitData[size];
        }
    };

    private static final String TAG = "TMoneyTransitData";
    private static final byte TMONEY_INFO_TAG = (byte )0xb0;

    private String mSerialNumber;
    private int mBalance;
    private String mDate;

    public TMoneyTransitData(TMoneyCard tMoneyCard) {
        super();
        mSerialNumber = parseSerial(tMoneyCard);
        mBalance = tMoneyCard.getBalance();
        mDate = parseDate(tMoneyCard);
    }

    @Nullable
    @Override
    public Integer getBalance() {
        return Integer.valueOf(mBalance);
    }

    @Override
    public Spanned formatCurrencyString(int amount, boolean isBalance) {
        return Utils.formatCurrencyString(amount, isBalance, "KRW", 1.0);
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new ListItem(R.string.tmoney_date, mDate));

        return items;
    }


    @SuppressWarnings("UnusedDeclaration")
    public TMoneyTransitData(Parcel p) {
        mSerialNumber = p.readString();
        mBalance = p.readInt();
        mDate = p.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(mSerialNumber);
        dest.writeInt(mBalance);
        dest.writeString(mDate);
    }

    public static TransitIdentity parseTransitIdentity(TMoneyCard card) {
        return new TransitIdentity(NAME, parseSerial(card));
    }

    private static String parseSerial(TMoneyCard card) {
        byte []tmoneytag = ISO7816Card.findAppInfoTag(card.getAppData(), TMONEY_INFO_TAG);
        return Utils.getHexString(tmoneytag, 4, 8);
    }

    private static String parseDate(TMoneyCard card) {
        byte []tmoneytag = ISO7816Card.findAppInfoTag(card.getAppData(), TMONEY_INFO_TAG);
        return Utils.getHexString(tmoneytag, 17, 2) + "/"
                + Utils.getHexString(tmoneytag, 19, 1);
    }
}
