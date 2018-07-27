package au.id.micolous.metrodroid.transit.newshenzhen;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Spanned;

import au.id.micolous.metrodroid.card.iso7816.ISO7816Card;
import au.id.micolous.metrodroid.card.newshenzhen.NewShenzhenCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

public class NewShenzhenTransitData extends TransitData {
    private int mBalance;
    public final String NAME = "Shenzhen Tong";
    private int mSerial;

    public static final Parcelable.Creator<NewShenzhenTransitData> CREATOR = new Parcelable.Creator<NewShenzhenTransitData>() {
        public NewShenzhenTransitData createFromParcel(Parcel parcel) {
            return new NewShenzhenTransitData(parcel);
        }

        public NewShenzhenTransitData[] newArray(int size) {
            return new NewShenzhenTransitData[size];
        }
    };

    public NewShenzhenTransitData(NewShenzhenCard card) {
        // upper bit is some garbage
        int bal = card.getBalance() & 0x7fffffff;
        // restore sign bit
        mBalance = bal | ((bal & 0x40000000) << 1);
        mSerial = parseSerial(card);
    }

    @Nullable
    @Override
    public Integer getBalance() {
        return mBalance;
    }

    @Override
    public Spanned formatCurrencyString(int currency, boolean isBalance) {
        return Utils.formatCurrencyString( currency, isBalance, "CNY");
    }

    @Override
    public String getSerialNumber() {
        return "" + mSerial;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mSerial);
        parcel.writeInt(mBalance);
    }

    public NewShenzhenTransitData(Parcel parcel) {
        mSerial = parcel.readInt();
        mBalance = parcel.readInt();
    }

    public static TransitIdentity parseTransitIdentity(NewShenzhenCard card) {
        return new TransitIdentity("Shenzhen Tong", "" + parseSerial(card));
    }

    private static int parseSerial(NewShenzhenCard card) {
        byte []szttag = ISO7816Card.findAppInfoTag(card.getAppData(), (byte) 0xa5);
        return Utils.byteArrayToInt(Utils.reverseBuffer(Utils.byteArraySlice(szttag, 23,4)));
    }

}
