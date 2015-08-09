package au.id.micolous.farebot.transit.manly_fast_ferry;

import android.os.Parcel;
import android.text.format.DateFormat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.FareBotApplication;
import au.id.micolous.farebot.R;
import au.id.micolous.farebot.card.UnauthorizedException;
import au.id.micolous.farebot.card.classic.ClassicBlock;
import au.id.micolous.farebot.card.classic.ClassicCard;
import au.id.micolous.farebot.card.classic.ClassicSector;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.transit.manly_fast_ferry.record.ManlyFastFerryBalanceRecord;
import au.id.micolous.farebot.transit.manly_fast_ferry.record.ManlyFastFerryMetadataRecord;
import au.id.micolous.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;
import au.id.micolous.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRecord;
import au.id.micolous.farebot.ui.HeaderListItem;
import au.id.micolous.farebot.ui.ListItem;
import au.id.micolous.farebot.util.Utils;

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 *
 * Documentation of format: https://github.com/micolous/farebot/wiki/Manly-Fast-Ferry
 */
public class ManlyFastFerryTransitData extends TransitData {
    private String                 mSerialNumber;
    private GregorianCalendar      mEpochDate;
    private int                    mBalance;
    private ManlyFastFerryTrip[]   mTrips;
    private ManlyFastFerryRefill[] mRefills;


    public static final String NAME = "Manly Fast Ferry";

    public static final byte[] SIGNATURE = {
            0x32,0x32,0x00,0x00
    };

    public static boolean check(ClassicCard card) {
        // TODO: Improve this check
        // The card contains two copies of the card's serial number on the card.
        // Lets use this for now to check that this is a Manly Fast Ferry card.
        byte[] file1, file2;

        try {
            file1 = card.getSector(0).getBlock(1).getData();
            file2 = card.getSector(0).getBlock(2).getData();
        } catch (UnauthorizedException ex) {
            // These blocks of the card are not protected.
            // This must not be a Manly Fast Ferry smartcard.
            return false;
        }

        // Serial number is from byte 10 in file 1 and byte 7 of file 2, for 4 bytes.
        if (!Arrays.equals(Arrays.copyOfRange(file1, 10, 14), Arrays.copyOfRange(file2, 7, 11))) {
            return false;
        }

        // Check a signature (not verified)
        return Arrays.equals(Arrays.copyOfRange(file1, 0, 4), SIGNATURE);
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        byte[] file1 = card.getSector(0).getBlock(1).getData();
        return new TransitIdentity(NAME, Utils.getHexString(Arrays.copyOfRange(file1, 10, 14)));
    }

    // Parcel
    @SuppressWarnings("UnusedDeclaration")
    public ManlyFastFerryTransitData (Parcel parcel) {
        mSerialNumber = parcel.readString();
        mEpochDate = new GregorianCalendar();
        mEpochDate.setTimeInMillis(parcel.readLong());
        mTrips = parcel.createTypedArray(ManlyFastFerryTrip.CREATOR);
        mRefills = parcel.createTypedArray(ManlyFastFerryRefill.CREATOR);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeLong(mEpochDate.getTimeInMillis());
        parcel.writeTypedArray(mTrips, flags);
        parcel.writeTypedArray(mRefills, flags);
    }

    // Decoder
    public ManlyFastFerryTransitData(ClassicCard card) {
        ArrayList<ManlyFastFerryRecord> records = new ArrayList<>();

        // Iterate through blocks on the card and deserialize all the binary data.
        for (ClassicSector sector : card.getSectors()) {
            for (ClassicBlock block : sector.getBlocks()) {
                if (sector.getIndex() == 0 && block.getIndex() == 0) {
                    continue;
                }

                if (block.getIndex() == 3) {
                    continue;
                }

                ManlyFastFerryRecord record = ManlyFastFerryRecord.recordFromBytes(block.getData());

                if (record != null) {
                    records.add(record);
                }
            }

        }

        // Now do a first pass for metadata and balance information.
        for (ManlyFastFerryRecord record : records) {
            if (record instanceof ManlyFastFerryMetadataRecord) {
                mSerialNumber = ((ManlyFastFerryMetadataRecord)record).getCardSerial();
                mEpochDate = ((ManlyFastFerryMetadataRecord)record).getEpochDate();
            } else if (record instanceof ManlyFastFerryBalanceRecord && !((ManlyFastFerryBalanceRecord) record).getIsPreviousBalance()) {
                // Current balance
                mBalance = ((ManlyFastFerryBalanceRecord)record).getBalance();
            }
        }

        // Now generate a transaction list.
        // These need the Epoch to be known first.
        ArrayList<ManlyFastFerryTrip> trips = new ArrayList<>();
        ArrayList<ManlyFastFerryRefill> refills = new ArrayList<>();

        for (ManlyFastFerryRecord record: records) {
            if (record instanceof ManlyFastFerryPurseRecord) {
                ManlyFastFerryPurseRecord purseRecord = (ManlyFastFerryPurseRecord)record;

                // Now convert this.
                if (purseRecord.getIsCredit()) {
                    // Credit
                    refills.add(new ManlyFastFerryRefill(purseRecord, mEpochDate));
                } else {
                    // Debit
                    trips.add(new ManlyFastFerryTrip(purseRecord, mEpochDate));
                }
            }
        }

        mTrips = trips.toArray(new ManlyFastFerryTrip[] {});
        mRefills = refills.toArray(new ManlyFastFerryRefill[] {});
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.);
    }

    // Structures
    @Override public String getSerialNumber () {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public Refill[] getRefills() {
        return mRefills;
    }

    @Override
    public Subscription[] getSubscriptions() {
        // There is no concept of "subscriptions".
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(R.string.general));
        Date cLastTransactionTime = mEpochDate.getTime();
        items.add(new ListItem(R.string.card_epoch, DateFormat.getLongDateFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));

        return items;
    }

    @Override
    public String getCardName() {
        return NAME;
    }


}
