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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.china.ChinaCardTransitFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/ShenzhenTong.java
public class NewShenzhenTransitData extends ChinaTransitData {
    private final int mSerial;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.szt_card)
            .setName(R.string.card_name_szt)
            .setLocation(R.string.location_shenzhen)
            .setCardType(CardType.FeliCa)
            .setPreview()
            .build();

    public static final Parcelable.Creator<NewShenzhenTransitData> CREATOR = new Parcelable.Creator<NewShenzhenTransitData>() {
        public NewShenzhenTransitData createFromParcel(Parcel parcel) {
            return new NewShenzhenTransitData(parcel);
        }

        public NewShenzhenTransitData[] newArray(int size) {
            return new NewShenzhenTransitData[size];
        }
    };

    public NewShenzhenTransitData(ChinaCard card) {
        super(card);
        mSerial = parseSerial(card);
        byte []szttag = getTagInfo(card);

        mValidityStart = Utils.byteArrayToInt(szttag, 20, 4);
        mValidityEnd = Utils.byteArrayToInt(szttag, 24, 4);
    }

    @Override
    protected ChinaTrip parseTrip(byte[] data) {
        return new NewShenzhenTrip(data);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
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
    }

    private NewShenzhenTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readInt();
    }

    public final static ChinaCardTransitFactory FACTORY = new ChinaCardTransitFactory() {
        @Override
        public List<byte[]> getAppNames() {
            return Collections.singletonList(Utils.stringToByteArray("PAY.SZT"));
        }

        @Override
        public TransitIdentity parseTransitIdentity(ChinaCard card) {
            return new TransitIdentity(R.string.card_name_szt, formatSerial(parseSerial(card)));
        }

        @Override
        public TransitData parseTransitData(ChinaCard chinaCard) {
            return new NewShenzhenTransitData(chinaCard);
        }

        @Override
        public CardInfo getCardInfo() {
            return CARD_INFO;
        }
    };

    private static String formatSerial(int sn) {
        int dig = sn;
        int digsum = 0;
        while(dig > 0) {
            digsum += dig % 10;
            dig /= 10;
        }
        digsum %= 10;
        // Sum of digits must be divisible by 10
        int lastDigit = (10 - digsum) % 10;
        return Integer.toString(sn) + "(" + Integer.toString(lastDigit) + ")";
    }

    private static byte[] getTagInfo(ChinaCard card) {
        ISO7816File file15 = getFile(card, 0x15);
        if (file15 != null)
            return file15.getBinaryData();
        byte []szttag = ISO7816Application.findBERTLV(card.getAppData(), 5,  5, true);
        return ISO7816Application.findBERTLV(szttag, 4,  0xc, false);
    }

    private static int parseSerial(ChinaCard card) {
        return Utils.byteArrayToIntReversed(getTagInfo(card), 16,4);
    }
}
