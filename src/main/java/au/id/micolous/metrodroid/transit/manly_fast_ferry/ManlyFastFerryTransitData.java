/*
 * ManlyFastFerryTransitData.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.manly_fast_ferry;

import android.os.Parcel;
import android.support.annotation.NonNull;

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
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 * <p>
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 * <p>
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Manly-Fast-Ferry
 */

public class ManlyFastFerryTransitData extends ErgTransitData {
    public static final String NAME = "Manly Fast Ferry";
    private static final int AGENCY_ID = 0x0227;
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Australia/Sydney");
    static final String CURRENCY = "AUD";

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.manly_fast_ferry_card)
            .setName(ManlyFastFerryTransitData.NAME)
            .setLocation(R.string.location_sydney_australia)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .build();

    // Parcel
    public static final Creator<ManlyFastFerryTransitData> CREATOR = new Creator<ManlyFastFerryTransitData>() {
        @Override
        public ManlyFastFerryTransitData createFromParcel(Parcel in) {
            return new ManlyFastFerryTransitData(in);
        }

        @Override
        public ManlyFastFerryTransitData[] newArray(int size) {
            return new ManlyFastFerryTransitData[size];
        }
    };

    private ManlyFastFerryTransitData(Parcel parcel) {
        super(parcel, CURRENCY);
    }

    private ManlyFastFerryTransitData(ClassicCard card) {
        super(card, CURRENCY);
    }

    public static final ClassicCardTransitFactory FACTORY = new ErgTransitFactory() {
        @Override
        public boolean check(@NonNull ClassicCard card) {
            if (!super.check(card)) {
                return false;
            }

            ErgMetadataRecord metadataRecord = ErgTransitData.getMetadataRecord(card);
            return metadataRecord != null && metadataRecord.getAgency() == AGENCY_ID;
        }

        @Override
        public boolean check(ClassicSector sector0) {
            if (!super.check(sector0)) {
                return false;
            }

            byte[] file2 = sector0.getBlock(2).getData();
            ErgMetadataRecord metadataRecord = ErgMetadataRecord.recordFromBytes(file2);
            return metadataRecord != null && metadataRecord.getAgency() == AGENCY_ID;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            byte[] file2 = card.getSector(0).getBlock(2).getData();
            ErgMetadataRecord metadata = ErgMetadataRecord.recordFromBytes(file2);
            return new TransitIdentity(NAME, metadata.getCardSerialHex());
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new ManlyFastFerryTransitData(classicCard);
        }

        @Override
        public CardInfo earlyCardInfo(List<ClassicSector> sectors) {
            if (check(sectors.get(0)))
                return CARD_INFO;
            return null;
        }

        @Override
        public int earlySectors() {
            return 1;
        }
    };

    @Override
    protected ErgTrip newTrip(ErgPurseRecord purse, GregorianCalendar epoch) {
        return new ManlyFastFerryTrip(purse, epoch);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    protected TimeZone getTimezone() {
        return TIME_ZONE;
    }
}
