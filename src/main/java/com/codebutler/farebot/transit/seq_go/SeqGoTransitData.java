package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.List;

import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU)
 *
 * Documentation of format: https://github.com/micolous/farebot/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
public class SeqGoTransitData extends TransitData {

    static final String NAME = "Go card";
    static final byte[] MANUFACTURER = {
        0x16, 0x18, 0x1A, 0x1B,
        0x1C, 0x1D, 0x1E, 0x1F
    };

    long mSerialNumber;



    public static boolean check(ClassicCard card) {
        try {
            byte[] blockData = card.getSector(0).getBlock(1).getData();
            return Arrays.equals(Arrays.copyOfRange(blockData, 1, 9), MANUFACTURER);
        } catch (UnauthorizedException ex) {
            // It is not possible to identify the card without a key
            return false;
        }
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        long serialNumber = Utils.byteArrayToLong(serialData, 0, 4);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber));
    }

    private static String formatSerialNumber(long serialNumber) {
        return String.format("016%012dx", serialNumber);

    }

    @SuppressWarnings("UnusedDeclaration")
    public SeqGoTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
    }

    public SeqGoTransitData(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 16);

        mSerialNumber = Utils.byteArrayToLong(serialData, 12, 4);

    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getSerialNumber() {
        return formatSerialNumber(mSerialNumber);
    }

    @Override
    public Trip[] getTrips() {
        return null;
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mSerialNumber);
    }
}
