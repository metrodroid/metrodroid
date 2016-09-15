/*
 * TransitData.java
 *
 * Copyright (C) 2011 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit;

import android.net.Uri;
import android.os.Parcelable;

import com.codebutler.farebot.ui.ListItem;

import java.util.List;

public abstract class TransitData implements Parcelable {
    public abstract String getBalanceString();
    public abstract String getSerialNumber();
    public abstract Trip[] getTrips();

    @Deprecated
    public Refill[] getRefills() { return null; }
    public abstract Subscription[] getSubscriptions();
    public abstract List<ListItem> getInfo();
    public abstract String getCardName();

    /**
     * If a TransitData provider doesn't know some of the stops / stations on a user's card, then
     * it may raise a signal to the user to submit the unknown stations to our web service.
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
     * StubTransitData.
     * @return Uri pointing to an FAQ page, or null if no page is to be supplied.
     */
    public Uri getMoreInfoPage() { return null; }

    /**
     * You may optionally link to a page which allows you to view the online services for the card.
     * @return Uri pointing to online services page, or null if no page is to be supplied.
     */
    public Uri getOnlineServicesPage() {
        return null;
    }
}
