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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.ui.ListItem;

public abstract class TransitData implements Parcelable {

    /**
     * Balance of the card's "purse".
     *
     * Cards with a single "purse" balance should implement this method, and
     * {@link TransitData#getBalances()} will convert single-purse cards.  Most transit cards have
     * only one purse so should use this.
     *
     * Cards with multiple "purse" balances (generally: hybrid transit cards that can act as
     * multiple transit cards) must implement {@link TransitData#getBalances()} instead.
     *
     * If the balance is not known, this does not need to be implemented.
     *
     * UI code must call {@link TransitData#getBalances()} to get the balances of purses in the
     * card, even in the case there is only one purse.
     *
     * @see TransitData#getBalances()
     * @return The balance of the card, or null if it is not known.
     */
    @Nullable
    protected TransitBalance getBalance() {
        return null;
    }

    /**
     * Balances of the card's "purses".
     *
     * Cards with multiple "purse" balances (generally: hybrid transit cards that can act as
     * multiple transit cards) must implement this method, and must not implement
     * {@link TransitData#getBalance()}.
     *
     * Cards with a single "purse" balance should implement {@link TransitData#getBalance()}
     * instead.
     *
     * @return The balance of the card, or null if it is not known.
     */
    @Nullable
    public List<TransitBalance> getBalances() {
        TransitBalance b = getBalance();
        if (b == null)
            return null;
        return Collections.singletonList(b);
    }

    @Nullable
    public abstract String getSerialNumber();

    /**
     * Lists all trips on the card.  May return null if the trip information cannot be read.
     *
     * @return Array of Trip[], or null if not supported.
     */
    @Nullable
    public List<? extends Trip> getTrips() {
        return null;
    }

    @Nullable
    public List<? extends Subscription> getSubscriptions() {
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
     * <li>Pass all currency amounts through
     * {@link TransitCurrency#formatCurrencyString(boolean)} and
     * {@link TransitCurrency#maybeObfuscateBalance()}.</li>
     * </ul>
     */
    @Nullable
    public List<ListItem> getInfo() {
        return null;
    }

    @NonNull
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

    public int describeContents() {
        return 0;
    }

    /**
     * You can optionally add a link to an FAQ page for the card.  This will be shown in the ...
     * drop down menu for cards that are supported, and on the main page for subclasses of
     * {@link au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData}.
     *
     * @return Uri pointing to an FAQ page, or null if no page is to be supplied.
     */
    @Nullable
    public Uri getMoreInfoPage() {
        return null;
    }

    /**
     * You may optionally link to a page which allows you to view the online services for the card.
     *
     * @return Uri pointing to online services page, or null if no page is to be supplied.
     */
    @Nullable
    public Uri getOnlineServicesPage() {
        return null;
    }

    @Nullable
    public String getWarning() {
        return null;
    }
}
