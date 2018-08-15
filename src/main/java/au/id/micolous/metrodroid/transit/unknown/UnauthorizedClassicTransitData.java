/*
 * UnauthorizedClassicTransitData.java
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
package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle MIFARE Classic with no open sectors
 */
public class UnauthorizedClassicTransitData extends UnauthorizedTransitData {
    public static final Creator<UnauthorizedClassicTransitData> CREATOR = new Creator<UnauthorizedClassicTransitData>() {
        public UnauthorizedClassicTransitData createFromParcel(Parcel parcel) {
            return new UnauthorizedClassicTransitData(parcel);
        }

        public UnauthorizedClassicTransitData[] newArray(int size) {
            return new UnauthorizedClassicTransitData[size];
        }
    };

    public UnauthorizedClassicTransitData(Parcel parcel) {
    }

    public UnauthorizedClassicTransitData() {
    }

    /**
     * This should be the last executed MIFARE Classic check, after all the other checks are done.
     * <p>
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all sectors on the card are locked.
     */
    public static boolean check(ClassicCard card) {
        // check to see if all sectors are blocked
        for (ClassicSector s : card.getSectors()) {
            if (!(s instanceof UnauthorizedClassicSector)) {
                // At least one sector is "open", this is not for us
                return false;
            }
        }
        return true;
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        return new TransitIdentity(Utils.localizeString(R.string.locked_mfc_card), null);
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_mfc_card);
    }
}
