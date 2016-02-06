package com.codebutler.farebot.transit.myki;

import android.net.Uri;
import android.os.Parcel;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.stub.StubTransitData;
import com.codebutler.farebot.util.Utils;

/**
 * Transit data type for Myki (Melbourne, AU).
 *
 * Documentation of format: https://github.com/micolous/farebot/wiki/Myki
 */
public class MykiTransitData extends StubTransitData {
    public static final String NAME = "Myki";
    private long   mSerialNumber1;
    private long   mSerialNumber2;

    public static boolean check (Card card) {
        return (card instanceof DesfireCard) &&
                (((DesfireCard) card).getApplication(4594) != null) &&
                (((DesfireCard) card).getApplication(15732978) != null);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MykiTransitData (Parcel parcel) {
        mSerialNumber1 = parcel.readLong();
        mSerialNumber2 = parcel.readLong();
    }

    public MykiTransitData (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] metadata = desfireCard.getApplication(4594).getFile(15).getData();
        metadata = Utils.reverseBuffer(metadata, 0, 16);

        try {
            mSerialNumber1 = Utils.getBitsFromBuffer(metadata, 96, 32);
            mSerialNumber2 = Utils.getBitsFromBuffer(metadata, 64, 32);
        } catch (Exception ex){
            throw new RuntimeException("Error parsing Myki data", ex);
        }
    }

    @Override public String getCardName () {
        return NAME;
    }

    @Override public String getSerialNumber () {
        return formatSerialNumber(mSerialNumber1, mSerialNumber2);
    }

    private static String formatSerialNumber(long serialNumber1, long serialNumber2) {
        String formattedSerial = String.format("%06d%08d", serialNumber1, serialNumber2);
        return formattedSerial + Utils.calculateLuhn(formattedSerial);
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(4594).getFile(15).getData();
        data = Utils.reverseBuffer(data, 0, 16);

        long serialNumber1 = Utils.getBitsFromBuffer(data, 96, 32);
        long serialNumber2 = Utils.getBitsFromBuffer(data, 64, 32);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber1, serialNumber2));
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber1);
        parcel.writeLong(mSerialNumber2);
    }

    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/farebot/myki");
    }
}