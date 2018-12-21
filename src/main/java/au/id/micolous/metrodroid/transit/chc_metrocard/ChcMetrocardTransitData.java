/*
 * ChcMetrocardTransitData.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.chc_metrocard;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.erg.ErgTransitData;
import au.id.micolous.metrodroid.transit.erg.ErgTrip;
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord;
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord;

/**
 * Transit data type for Metrocard (Christchurch, NZ).
 * <p>
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */

public class ChcMetrocardTransitData extends ErgTransitData {
    public static final String NAME = "Metrocard";
    private static final int AGENCY_ID = 0x0136;
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Pacific/Auckland");
    static final String CURRENCY = "NZD";

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.chc_metrocard)
            .setName(ChcMetrocardTransitData.NAME)
            .setLocation(R.string.location_christchurch_nz)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview()
            .build();

    // Parcel
    public static final Creator<ChcMetrocardTransitData> CREATOR = new Creator<ChcMetrocardTransitData>() {
        @Override
        public ChcMetrocardTransitData createFromParcel(Parcel in) {
            return new ChcMetrocardTransitData(in);
        }

        @Override
        public ChcMetrocardTransitData[] newArray(int size) {
            return new ChcMetrocardTransitData[size];
        }
    };

    public ChcMetrocardTransitData(Parcel parcel) {
        super(parcel, CURRENCY);
    }

    public ChcMetrocardTransitData(ClassicCard card) {
        super(card, CURRENCY);
    }

    public static final ClassicCardTransitFactory FACTORY = new ErgTransitFactory() {
        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new ChcMetrocardTransitData(classicCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @Override
    protected ErgTrip newTrip(ErgPurseRecord purse, GregorianCalendar epoch) {
        return new ChcMetrocardTrip(purse, epoch);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    protected String formatSerialNumber(ErgMetadataRecord metadataRecord) {
        return Integer.toString(metadataRecord.getCardSerialDec());
    }

    @Override
    protected TimeZone getTimezone() {
        return TIME_ZONE;
    }
}
