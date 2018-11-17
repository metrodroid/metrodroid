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
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.card.china.ChinaCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/CityUnion.java
public class CityUnionTransitData extends ChinaTransitData {
    private final int mSerial;
    private final int mCity;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(R.string.card_name_cityunion)
            .setLocation(R.string.location_china_mainland)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Creator<CityUnionTransitData> CREATOR = new Creator<CityUnionTransitData>() {
        public CityUnionTransitData createFromParcel(Parcel parcel) {
            return new CityUnionTransitData(parcel);
        }

        public CityUnionTransitData[] newArray(int size) {
            return new CityUnionTransitData[size];
        }
    };

    public CityUnionTransitData(ChinaCard card) {
        super(card);
        mSerial = parseSerial(card);
        byte[] file15 = getFile(card, 0x15).getBinaryData();

        mValidityStart = Utils.byteArrayToInt(file15, 20, 4);
        mValidityEnd = Utils.byteArrayToInt(file15, 24, 4);
        mCity = Utils.byteArrayToInt(file15, 2, 2);
    }

    @Override
    protected ChinaTrip parseTrip(byte[] data) {
        return new ChinaTrip(data);
    }

    @Override
    public String getSerialNumber() {
        return Integer.toString(mSerial);
    }

    @NonNull
    @Override
    public CardInfo getCardInfo() {
        return CARD_INFO;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeInt(mSerial);
        parcel.writeInt(mCity);
    }

    private CityUnionTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readInt();
        mCity = parcel.readInt();
    }

    public final static ChinaCardTransitFactory FACTORY = new ChinaCardTransitFactory() {
        @Override
        public List<byte[]> getAppNames() {
            return Collections.singletonList(Utils.hexStringToByteArray("A00000000386980701"));
        }

        @Override
        public TransitIdentity parseTransitIdentity(ChinaCard card) {
            return new TransitIdentity(R.string.card_name_cityunion, Integer.toString(parseSerial(card)));
        }

        @Override
        public TransitData parseTransitData(ChinaCard chinaCard) {
            return new CityUnionTransitData(chinaCard);
        }

        @Override
        public CardInfo getCardInfo() {
            return CARD_INFO;
        }
    };

    private static int parseSerial(ChinaCard card) {
        byte[] file15 = getFile(card, 0x15).getBinaryData();
        if (Utils.byteArrayToInt(file15, 2, 2) == 0x2000)
            return Utils.byteArrayToInt(file15, 16,4);
        return Utils.byteArrayToIntReversed(file15, 16,4);
    }

    @Override
    public List<ListItem> getInfo() {
        return Collections.singletonList(new ListItem(R.string.city_union_city, Integer.toHexString(mCity)));
    }
}
