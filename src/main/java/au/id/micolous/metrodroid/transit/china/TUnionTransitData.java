/*
 * NewShenzhenTransitData.java
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

package au.id.micolous.metrodroid.transit.china;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/TUnion.java
public class TUnionTransitData extends ChinaTransitData {
    private final String mSerial;
    private final int mNegativeBalance;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(Utils.localizeString(R.string.card_name_tunion))
            .setLocation(R.string.location_tunion)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Creator<TUnionTransitData> CREATOR = new Creator<TUnionTransitData>() {
        public TUnionTransitData createFromParcel(Parcel parcel) {
            return new TUnionTransitData(parcel);
        }

        public TUnionTransitData[] newArray(int size) {
            return new TUnionTransitData[size];
        }
    };

    public TUnionTransitData(ChinaCard card) {
        super(card);
        mSerial = parseSerial(card);
        byte[] file15 = getFile(card, 0x15).getBinaryData();

        mValidityStart = Utils.byteArrayToInt(file15, 20, 4);
        mValidityEnd = Utils.byteArrayToInt(file15, 24, 4);
        mNegativeBalance = Utils.getBitsFromBuffer(card.getBalance(1), 1, 31);
    }

    @Override
    protected ChinaTrip parseTrip(byte[] data) {
        return new ChinaTrip(data);
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_tunion);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(mSerial);
        parcel.writeInt(mNegativeBalance);
    }

    private TUnionTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readString();
        mNegativeBalance = parcel.readInt();
    }

    public static TransitIdentity parseTransitIdentity(ChinaCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_tunion), parseSerial(card));
    }

    private static String parseSerial(ChinaCard card) {
        byte[] file15 = getFile(card, 0x15).getBinaryData();
        return Utils.getHexString(file15, 10, 10).substring(1);
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        if (mBalance > 0)
            return new TransitBalanceStored(TransitCurrency.CNY(mBalance),
                    null, parseHexDate(mValidityStart), parseHexDate(mValidityEnd));
        return new TransitBalanceStored(TransitCurrency.CNY(mBalance-mNegativeBalance),
                null, parseHexDate(mValidityStart), parseHexDate(mValidityEnd));
    }
}
