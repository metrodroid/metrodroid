/*
 * UnauthorizedUltralightTransitData.java
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
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.card.ultralight.UnauthorizedUltralightPage;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle MIFARE Ultralight with no open pages
 */
public class UnauthorizedUltralightTransitData extends UnauthorizedTransitData {
    public static final Creator<UnauthorizedUltralightTransitData> CREATOR = new Creator<UnauthorizedUltralightTransitData>() {
        public UnauthorizedUltralightTransitData createFromParcel(Parcel parcel) {
            return new UnauthorizedUltralightTransitData();
        }

        public UnauthorizedUltralightTransitData[] newArray(int size) {
            return new UnauthorizedUltralightTransitData[size];
        }
    };

    public UnauthorizedUltralightTransitData() {
    }

    public final static UltralightCardTransitFactory FACTORY = new UltralightCardTransitFactory() {
        /**
         * This should be the last executed MIFARE Ultralight check, after all the other checks are done.
         * <p>
         * This is because it will catch others' cards.
         *
         * @param card Card to read.
         * @return true if all sectors on the card are locked.
         */
        @Override
        public boolean check(@NonNull UltralightCard card) {
            // check to see if all sectors are blocked
            for (UltralightPage p : card.getPages()) {
                if (p.getIndex() >= 4) {
                    // User memory is page 4 and above
                    if (!(p instanceof UnauthorizedUltralightPage)) {
                        // At least one page is "open", this is not for us
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public TransitData parseTransitData(@NonNull UltralightCard ultralightCard) {
            return new UnauthorizedUltralightTransitData();
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull UltralightCard card) {
            return new TransitIdentity(Utils.localizeString(R.string.locked_mfu_card), null);
        }
    };

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_mfu_card);
    }
}
