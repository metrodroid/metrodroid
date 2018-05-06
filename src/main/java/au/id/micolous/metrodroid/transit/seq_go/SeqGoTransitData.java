/*
 * SeqGoTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go;

import android.net.Uri;
import android.os.Parcel;
import android.text.Spanned;

import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
public class SeqGoTransitData extends NextfareTransitData {

    public static final String NAME = "Go card";
    public static final Creator<SeqGoTransitData> CREATOR = new Creator<SeqGoTransitData>() {
        public SeqGoTransitData createFromParcel(Parcel parcel) {
            return new SeqGoTransitData(parcel);
        }

        public SeqGoTransitData[] newArray(int size) {
            return new SeqGoTransitData[size];
        }
    };
    static final byte[] MANUFACTURER = {
            0x16, 0x18, 0x1A, 0x1B,
            0x1C, 0x1D, 0x1E, 0x1F
    };

    static final byte[] SYSTEM_CODE1 = {
            0x5A, 0x5B, 0x20, 0x21, 0x22, 0x23
    };

    static final byte[] SYSTEM_CODE2 = {
            0x20, 0x21, 0x22, 0x23, 0x01, 0x01
    };

    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Australia/Brisbane");

    private static final String TAG = "SeqGoTransitData";
    private SeqGoTicketType mTicketType;

    public SeqGoTransitData(Parcel parcel) {
        super(parcel);
        mTicketType = (SeqGoTicketType) parcel.readSerializable();
    }

    public SeqGoTransitData(ClassicCard card) {
        super(card);
        if (mConfig != null) {
            mTicketType = SeqGoData.TICKET_TYPE_MAP.get(mConfig.getTicketType(), SeqGoTicketType.UNKNOWN);
        }
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return NextfareTransitData.parseTransitIdentity(card, NAME);
    }

    public static boolean check(ClassicCard card) {
        try {
            byte[] blockData = card.getSector(0).getBlock(1).getData();
            if (!Arrays.equals(Arrays.copyOfRange(blockData, 1, 9), MANUFACTURER)) {
                return false;
            }

            byte[] systemCode = Arrays.copyOfRange(blockData, 9, 15);
            //Log.d(TAG, "SystemCode = " + Utils.getHexString(systemCode));
            return Arrays.equals(systemCode, SYSTEM_CODE1) || Arrays.equals(systemCode, SYSTEM_CODE2);
        } catch (UnauthorizedException ex) {
            // It is not possible to identify the card without a key
            return false;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeSerializable(mTicketType);
    }

    @Override
    protected NextfareTrip newTrip() {
        return new SeqGoTrip();
    }

    @Override
    protected NextfareTrip newRefill(NextfareTopupRecord record) {
        return new SeqGoRefill(record);
    }

    @Override
    protected Trip.Mode lookupMode(int mode, int stationId) {
        return SeqGoData.VEHICLES.get(mode, Trip.Mode.OTHER);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/seqgo");
    }

    @Override
    public Uri getOnlineServicesPage() {
        return Uri.parse("https://gocard.translink.com.au/");
    }

    /**
     * The base implementation of hasUnknownStations from Nextfare always returns false, but we can
     * return the correct value for Go card.
     *
     * @return true if there are unknown station IDs on the card.
     */
    @Override
    public boolean hasUnknownStations() {
        return mHasUnknownStations;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.seqgo_ticket_type, mTicketType.getDescription()));

        items.addAll(super.getInfo());
        return items;
    }

    @Override
    public Spanned formatCurrencyString(int currency, boolean isBalance) {
        return Utils.formatCurrencyString(currency, isBalance, "AUD");
    }

    @Override
    protected TimeZone getTimezone() {
        return TIME_ZONE;
    }
}
