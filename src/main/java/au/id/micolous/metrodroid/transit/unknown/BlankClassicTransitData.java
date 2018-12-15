/*
 * BlankClassicTransitData.java
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
package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.InvalidClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle MIFARE Classic with no non-default data
 */
public class BlankClassicTransitData extends TransitData {
    public static final Creator<BlankClassicTransitData> CREATOR = new Creator<BlankClassicTransitData>() {
        public BlankClassicTransitData createFromParcel(Parcel parcel) {
            return new BlankClassicTransitData(parcel);
        }

        public BlankClassicTransitData[] newArray(int size) {
            return new BlankClassicTransitData[size];
        }
    };

    private BlankClassicTransitData() {

    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {
        /**
         * @param card Card to read.
         * @return true if all sectors on the card are blank.
         */
        @Override
        public boolean check(@NonNull ClassicCard card) {
            List<ClassicSector> sectors = card.getSectors();
            boolean allZero = true, allFF = true;
            // check to see if all sectors are blocked
            for (ClassicSector s : sectors) {
                if ((s instanceof UnauthorizedClassicSector) || (s instanceof InvalidClassicSector))
                    return false;

                int numBlocks = s.getBlocks().size();

                for (ClassicBlock bl : s.getBlocks()) {
                    // Manufacturer data
                    if (s.getIndex() == 0 && bl.getIndex() == 0)
                        continue;
                    if (bl.getIndex() == numBlocks - 1)
                        continue;
                    for (byte b : bl.getData()) {
                        if (b != 0)
                            allZero = false;
                        if (b != -1)
                            allFF = false;
                        if (!allZero && !allFF)
                            return false;
                    }
                }
            }
            return true;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(Utils.localizeString(R.string.blank_mfc_card), null);
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new BlankClassicTransitData();
        }
    };

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.blank_mfc_card);
    }


    @Override
    public void writeToParcel(Parcel dest, int i) {
    }

    public BlankClassicTransitData(Parcel p) {
    }
}
