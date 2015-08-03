package au.id.micolous.farebot.transit.myki;

import android.os.Parcel;

import au.id.micolous.farebot.R;
import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.card.desfire.DesfireCard;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.ui.HeaderListItem;
import au.id.micolous.farebot.ui.ListItem;
import au.id.micolous.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Myki (Melbourne, AU).
 *
 * Documentation of format: https://github.com/codebutler/farebot/wiki/Myki
 */
public class MykiTransitData extends TransitData {
    private long   mSerialNumber1;
    private long   mSerialNumber2;
    private int    mBalance; // cents
    private int    mLastDigit;

    public static boolean check (Card card) {
        return (card instanceof DesfireCard) &&
                (((DesfireCard) card).getApplication(4594) != null) &&
                (((DesfireCard) card).getApplication(15732978) != null);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MykiTransitData (Parcel parcel) {
        mSerialNumber1 = parcel.readLong();
        mSerialNumber2 = parcel.readLong();
        mLastDigit     = parcel.readInt();
        mBalance       = parcel.readInt();
    }

    public MykiTransitData (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] metadata = desfireCard.getApplication(4594).getFile(15).getData();
        metadata = Utils.reverseBuffer(metadata, 0, 16);
        byte[] balancedata = desfireCard.getApplication(15732978).getFile(15).getData();
        balancedata = Utils.reverseBuffer(balancedata, 0, 16);

        try {
            mSerialNumber1 = Utils.getBitsFromBuffer(metadata, 96, 32);
            mSerialNumber2 = Utils.getBitsFromBuffer(metadata, 64, 32);
            //mLastDigit = Utils.getBitsFromBuffer(metadata, 8, 8);

            // TODO: check this value
            //mBalance = Utils.getBitsFromBuffer(balancedata, 96, 32);
        } catch (Exception ex){
            throw new RuntimeException("Error parsing Myki data", ex);
        }
    }

    @Override public String getCardName () {
        return "Myki";
    }


    @Override public String getBalanceString () {
        return null;
        //return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.);
    }

    @Override public String getSerialNumber () {
        return formatSerialNumber(mSerialNumber1, mSerialNumber2, mLastDigit);
    }

    private static String formatSerialNumber(long serialNumber1, long serialNumber2, int lastDigit) {
        return String.format("%06d%08d%01d", serialNumber1, serialNumber2, lastDigit);

    }

    @Override public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();


        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem("Myki support is experimental!", "Please report success and failure."));
/*        items.add(new ListItem(R.string.opal_weekly_trips, Integer.toString(mWeeklyTrips)));
        items.add(new ListItem(R.string.checksum, Integer.toString(mChecksum)));
*/
        items.add(new HeaderListItem(R.string.last_transaction));
/*        items.add(new ListItem(R.string.transaction_sequence, Integer.toString(mTransactionNumber)));
        Date cLastTransactionTime = getLastTransactionTime().getTime();
        items.add(new ListItem(R.string.date, DateFormat.getLongDateFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));
        items.add(new ListItem(R.string.time, DateFormat.getTimeFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));
        items.add(new ListItem(R.string.vehicle_type, getVehicleType(mVehicleType)));
        items.add(new ListItem(R.string.transaction_type, getActionType(mActionType)));
*/
        return items;
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(4594).getFile(15).getData();
        data = Utils.reverseBuffer(data, 0, 16);

        long serialNumber1 = Utils.getBitsFromBuffer(data, 96, 32);
        long serialNumber2 = Utils.getBitsFromBuffer(data, 64, 32);
        int lastDigit = Utils.getBitsFromBuffer(data, 8, 8);
        return new TransitIdentity("Myki", formatSerialNumber(serialNumber1, serialNumber2, lastDigit));
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber1);
        parcel.writeLong(mSerialNumber2);
        parcel.writeInt(mLastDigit);
        parcel.writeInt(mBalance);
    }

    @Override public Subscription[] getSubscriptions() {
        // TODO: handle Myki travel pass
        return null;
    }

    public static String getVehicleType(int vehicleType) {
        //if (OpalData.VEHICLES.containsKey(vehicleType)) {
        //    return Utils.localizeString(OpalData.VEHICLES.get(vehicleType));
        //}
        return Utils.localizeString(R.string.unknown_format, "0x" + Long.toString(vehicleType, 16));
    }

    public static String getActionType(int actionType) {
        //if (OpalData.ACTIONS.containsKey(actionType)) {
        //    return Utils.localizeString(OpalData.ACTIONS.get(actionType));
        //}

        return Utils.localizeString(R.string.unknown_format, "0x" + Long.toString(actionType, 16));
    }

    // Unsupported elements
    @Override public Refill[] getRefills () { return null; }
    @Override public Trip[] getTrips () {
        return null;
    }


}