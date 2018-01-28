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
package au.id.micolous.farebot.transit.unknown;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.card.ultralight.UltralightCard;
import au.id.micolous.farebot.card.ultralight.UltralightPage;
import au.id.micolous.farebot.card.ultralight.UnauthorizedUltralightPage;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.util.Utils;

import au.id.micolous.farebot.R;

/**
 * Handle MiFare Classic with no open sectors
 */
public class UnauthorizedUltralightTransitData extends TransitData {
    /**
     * This should be the last executed Mifare Classic check, after all the other checks are done.
     * <p>
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all sectors on the card are locked.
     */
    public static boolean check(UltralightCard card) {
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

    public static TransitIdentity parseTransitIdentity(Card card) {
        return new TransitIdentity(Utils.localizeString(R.string.locked_card), null);
    }


    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_card);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }

    @Override
    public String formatCurrencyString(int currency, boolean isBalance) {
        return null;
    }

    @Nullable
    @Override
    public Integer getBalance() {
        return null;
    }
}
