/*
 * ErgTransitData.java
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.erg;

import android.os.Parcel;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.time.TimestampFormatter;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.erg.record.ErgBalanceRecord;
import au.id.micolous.metrodroid.transit.erg.record.ErgIndexRecord;
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord;
import au.id.micolous.metrodroid.transit.erg.record.ErgPreambleRecord;
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord;
import au.id.micolous.metrodroid.transit.erg.record.ErgRecord;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

/**
 * Transit data type for ERG/Videlli/Vix MIFARE Classic cards.
 *
 * Wiki: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
public class ErgTransitData extends TransitData {
    // Flipping this to true shows more data from the records in Logcat.
    private static final boolean DEBUG = true;
    private static final String TAG = ErgTransitData.class.getSimpleName();

    static final String NAME = "ERG";
    public static final byte[] SIGNATURE = {
            0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01
    };

    private String mSerialNumber;
    private int mEpochDate;
    private int mAgencyID;
    private int mBalance;
    private final List<? extends Trip> mTrips;

    // Parcel
    public static final Creator<ErgTransitData> CREATOR = new Creator<ErgTransitData>() {
        @Override
        public ErgTransitData createFromParcel(Parcel in) {
            return new ErgTransitData(in);
        }

        @Override
        public ErgTransitData[] newArray(int size) {
            return new ErgTransitData[size];
        }
    };
    private final String mCurrency;

    protected ErgTransitData(Parcel parcel) {
        mSerialNumber = parcel.readString();
        mEpochDate = parcel.readInt();
        //noinspection unchecked
        mTrips = parcel.readArrayList(getClass().getClassLoader());
        mCurrency = parcel.readString();
    }

    public ErgTransitData(ClassicCard card) {
        this(card, "XXX");
    }

    // Decoder
    protected ErgTransitData(ClassicCard card, String currency) {
        List<ErgRecord> records = new ArrayList<>();

        mCurrency = currency;

        // Read the index data
        final ErgIndexRecord index1 = ErgIndexRecord.Companion.recordFromSector(card.getSector(1));
        final ErgIndexRecord index2 = ErgIndexRecord.Companion.recordFromSector(card.getSector(2));
        if (DEBUG) {
            Log.d(TAG, "Index 1: " + index1.toString());
            Log.d(TAG, "Index 2: " + index2.toString());
        }

        final ErgIndexRecord activeIndex = index1.getVersion() > index2.getVersion() ? index1 : index2;

        ErgMetadataRecord metadataRecord = null;
        ErgPreambleRecord preambleRecord = null;

        // Iterate through blocks on the card and deserialize all the binary data.
        sectorLoop:
        for (ClassicSector sector : card.getSectors()) {
            final int sectorNum = sector.getIndex();
            blockLoop:
            for (ClassicBlock block : sector.getBlocks()) {
                final int blockNum = block.getIndex();
                final ImmutableByteArray data = block.getData();

                if (block.getIndex() == 3) {
                    continue;
                }

                switch (sectorNum) {
                    case 0:
                        switch (blockNum) {
                            case 0:
                                continue blockLoop;
                            case 1:
                                preambleRecord = ErgPreambleRecord.Companion.recordFromBytes(data);
                                continue blockLoop;
                            case 2:
                                metadataRecord = ErgMetadataRecord.Companion.recordFromBytes(data);
                                continue blockLoop;
                        }

                    case 1:
                    case 2:
                        // Skip indexes, we already read this.
                        continue sectorLoop;
                }

                // Fallback to using indexes
                ErgRecord record = activeIndex.readRecord(sectorNum, blockNum, data);

                if (record != null) {
                    Log.d(TAG, String.format(Locale.ENGLISH, "Sector %d, Block %d: %s",
                            sectorNum, blockNum,
                            DEBUG ? record.toString() : record.getClass().getSimpleName()));
                    if (DEBUG) {
                        Log.d(TAG, data.getHexString());
                    }
                }

                if (record != null) {
                    records.add(record);
                }
            }
        }

        if (metadataRecord != null) {
            mSerialNumber = formatSerialNumber(metadataRecord);
            mEpochDate = metadataRecord.getEpochDate();
            mAgencyID = metadataRecord.getAgencyID();
        }

        List<ErgTransaction> txns = new ArrayList<>();

        for (ErgRecord record : records) {
            if (record instanceof ErgBalanceRecord) {
                mBalance = ((ErgBalanceRecord) record).getBalance();
            } else if (record instanceof ErgPurseRecord) {
                ErgPurseRecord purseRecord = (ErgPurseRecord) record;
                txns.add(newTrip(purseRecord, mEpochDate));
            }
        }

        Collections.sort(txns, new Transaction.Comparator());

        // Merge trips as appropriate
        mTrips = TransactionTrip.merge(txns);
    }

    public static final ClassicCardTransitFactory FALLBACK_FACTORY = new ErgTransitFactory();

    @Nullable
    static ErgMetadataRecord getMetadataRecord(ClassicSector sector0) {
        ImmutableByteArray file2;
        try {
            file2 = sector0.getBlock(2).getData();
        } catch (UnauthorizedException ex) {
            // Can't be for us...
            return null;
        }

        return ErgMetadataRecord.Companion.recordFromBytes(file2);
    }

    @Nullable
    static ErgMetadataRecord getMetadataRecord(ClassicCard card) {
        try {
            return getMetadataRecord(card.getSector(0));
        } catch (UnauthorizedException ex) {
            // Can't be for us...
            return null;
        }
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeInt(mEpochDate);
        parcel.writeList(mTrips);
        parcel.writeString(mCurrency);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    @Nullable
    public TransitBalance getBalance() {
        return new TransitCurrency(mBalance, mCurrency);
    }

    // Structures
    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public List<? extends Trip> getTrips() {
        return mTrips;
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.card_epoch,
                TimestampFormatter.INSTANCE.longDateFormat(TripObfuscator.INSTANCE.maybeObfuscateTS(
                        TimestampFormatterKt.calendar2ts(mEpochDate)))));
        items.add(new ListItem(R.string.erg_agency_id,
                NumberUtils.INSTANCE.longToHex(mAgencyID)));
        return items;
    }

    @NotNull
    @Override
    public String getCardName() {
        return NAME;
    }

    /**
     * Allows you to override the constructor for new trips, to hook in your own station ID code.
     *
     * @return Subclass of ErgTransaction.
     */
    protected ErgTransaction newTrip(@NotNull ErgPurseRecord purse, int epoch) {
        return new ErgTransaction(purse, epoch, mCurrency, getTimezone());
    }

    /**
     * Some cards format the serial number in decimal rather than hex. By default, this uses hex.
     *
     * This can be overridden in subclasses to format the serial number correctly.
     * @param metadataRecord Metadata record for this card.
     * @return Formatted serial number, as string.
     */
    protected String formatSerialNumber(ErgMetadataRecord metadataRecord) {
        return metadataRecord.getCardSerialHex();
    }

    /**
     * Allows you to override the timezone used for all dates and times. Default timezone is the
     * current Android OS timezone.
     *
     * @return TimeZone for the card.
     */
    @NotNull
    protected TimeZone getTimezone() {
        // If we don't know the timezone, assume it is Android local timezone.
        return TimeZone.getDefault();
    }
}
