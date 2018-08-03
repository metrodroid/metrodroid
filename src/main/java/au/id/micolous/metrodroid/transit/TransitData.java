/*
 * TransitData.java
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.transit;

import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.ui.ListItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class TransitData implements Parcelable {

    /**
     * Balance of the card. The value is passed to getBalanceString for formatting purposes.
     *
     * @return The balance of the card, or null if it is not known.
     */
    @Nullable
    public TransitCurrency getBalance() {
        return null;
    }

    @Nullable
    public List<TransitBalance> getBalances() {
        TransitCurrency b = getBalance();
        if (b == null)
            return null;
        return Arrays.asList(new TransitBalanceStored(b));
    }

    public abstract String getSerialNumber();

    /**
     * Lists all trips on the card.  May return null if the trip information cannot be read.
     *
     * @return Array of Trip[], or null if not supported.
     */
    public Trip[] getTrips() {
        return null;
    }

    @Deprecated
    public Refill[] getRefills() {
        return null;
    }

    public Subscription[] getSubscriptions() {
        return null;
    }

    /**
     * Allows {@link TransitData} implementors to show extra information that doesn't fit within the
     * standard bounds of the interface.  By default, this returns null, so the "Info" tab will not
     * be displayed.
     * <p>
     * Note: in order to support obfuscation / hiding behaviour, if you implement this method, you
     * also need to use some other functionality:
     * <ul>
     * <li>Check for {@link MetrodroidApplication#hideCardNumbers()} whenever you show a card
     * number, or other mark (such as a name) that could be used to identify this card or its
     * holder.</li>
     *
     * <li>Pass {@link java.util.Calendar}/{@link java.util.Date} objects (timestamps) through
     * {@link au.id.micolous.metrodroid.util.TripObfuscator#maybeObfuscateTS}.  This also
     * works on epoch timestamps (expressed as seconds since UTC).</li>
     *
     * <li>Pass all currency amounts through {@link au.id.micolous.metrodroid.transit.TransitCurrency#formatCurrencyString(boolean)} and
     * {@link au.id.micolous.metrodroid.transit.TransitCurrency#maybeObfuscate()}.</li>
     * </ul>
     */
    public List<ListItem> getInfo() {
        return null;
    }

    public abstract String getCardName();

    /**
     * If a {@link TransitData} provider doesn't know some of the stops / stations on a user's card,
     * then it may raise a signal to the user to submit the unknown stations to our web service.
     *
     * @return false if all stations are known (default), true if there are unknown stations
     */
    public boolean hasUnknownStations() {
        return false;
    }

    public final int describeContents() {
        return 0;
    }

    /**
     * You can optionally add a link to an FAQ page for the card.  This will be shown in the ...
     * drop down menu for cards that are supported, and on the main page for subclasses of
     * {@link au.id.micolous.metrodroid.transit.stub.StubTransitData}.
     *
     * @return Uri pointing to an FAQ page, or null if no page is to be supplied.
     */
    public Uri getMoreInfoPage() {
        return null;
    }

    /**
     * You may optionally link to a page which allows you to view the online services for the card.
     *
     * @return Uri pointing to online services page, or null if no page is to be supplied.
     */
    public Uri getOnlineServicesPage() {
        return null;
    }
}
