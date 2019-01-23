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
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.china.ChinaCardTransitFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/ShenzhenTong.java
public class NewShenzhenTransitData extends ChinaTransitData {
    private final int mSerial;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.szt_card)
            .setName(Localizer.INSTANCE.localizeString(R.string.card_name_szt))
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

    private NewShenzhenTransitData(ChinaCard card) {
        super(card);
        mSerial = parseSerial(card);
        ImmutableByteArray szttag = getTagInfo(card);

        if (szttag != null) {
            mValidityStart = szttag.byteArrayToInt(20, 4);
            mValidityEnd = szttag.byteArrayToInt(24, 4);
        }
    }

    @Override
    protected ChinaTrip parseTrip(ImmutableByteArray data) {
        return new NewShenzhenTrip(data);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return Localizer.INSTANCE.localizeString(R.string.card_name_szt);
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
        public List<ImmutableByteArray> getAppNames() {
            return Collections.singletonList(ImmutableByteArray.Companion.fromASCII("PAY.SZT"));
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ChinaCard card) {
            return new TransitIdentity(Localizer.INSTANCE.localizeString(R.string.card_name_szt), formatSerial(parseSerial(card)));
        }

        @Override
        public TransitData parseTransitData(@NonNull ChinaCard chinaCard) {
            return new NewShenzhenTransitData(chinaCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @NonNls
    private static String formatSerial(int sn) {
        int digsum = NumberUtils.INSTANCE.getDigitSum(sn);
        // Sum of digits must be divisible by 10
        int lastDigit = (10 - (digsum % 10)) % 10;
        return Integer.toString(sn) + "(" + Integer.toString(lastDigit) + ")";
    }

    @Nullable
    private static ImmutableByteArray getTagInfo(ChinaCard card) {
        ISO7816File file15 = getFile(card, 0x15);
        if (file15 != null)
            return file15.getBinaryData();
        ImmutableByteArray szttag = ISO7816TLV.INSTANCE.findBERTLV(card.getAppData(), "a5", true);
        if (szttag == null)
            return null;
        return ISO7816TLV.INSTANCE.findBERTLV(szttag, "8c", false);
    }

    private static int parseSerial(ChinaCard card) {
        ImmutableByteArray ti = getTagInfo(card);
        if (ti == null)
            return 0;
        return ti.byteArrayToIntReversed(16,4);
    }
}
