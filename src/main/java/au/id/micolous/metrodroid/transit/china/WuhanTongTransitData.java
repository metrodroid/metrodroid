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
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/WuhanTong.java
public class WuhanTongTransitData extends ChinaTransitData {
    private final String mSerial;

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(Utils.localizeString(R.string.card_name_wuhantong))
            .setLocation(R.string.location_wuhan)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Creator<WuhanTongTransitData> CREATOR = new Creator<WuhanTongTransitData>() {
        public WuhanTongTransitData createFromParcel(Parcel parcel) {
            return new WuhanTongTransitData(parcel);
        }

        public WuhanTongTransitData[] newArray(int size) {
            return new WuhanTongTransitData[size];
        }
    };

    private WuhanTongTransitData(ChinaCard card) {
        super(card);
        mSerial = parseSerial(card);
        ImmutableByteArray file5 = getFile(card, 0x5).getBinaryData();

        if (file5 != null) {
            mValidityStart = file5.byteArrayToInt(20, 4);
            mValidityEnd = file5.byteArrayToInt(16, 4);
        }
    }

    @Override
    protected ChinaTrip parseTrip(ImmutableByteArray data) {
        return new ChinaTrip(data);
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_wuhantong);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(mSerial);
    }

    private WuhanTongTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readString();
    }

    public final static ChinaCardTransitFactory FACTORY = new ChinaCardTransitFactory() {
        @Override
        public List<ImmutableByteArray> getAppNames() {
            return Collections.singletonList(ImmutableByteArray.Companion.fromASCII("AP1.WHCTC"));
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ChinaCard card) {
            return new TransitIdentity(Utils.localizeString(R.string.card_name_wuhantong), parseSerial(card));
        }

        @Override
        public TransitData parseTransitData(@NonNull ChinaCard chinaCard) {
            return new WuhanTongTransitData(chinaCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    private static String parseSerial(ChinaCard card) {
        ISO7816File filea = getFile(card, 0xa);

        if (filea == null)
            return null;
        return filea.getBinaryData().getHexString(0, 5);
    }
}
