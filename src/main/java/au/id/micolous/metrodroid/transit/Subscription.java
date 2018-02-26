/*
 * Subscription.java
 *
 * Copyright (C) 2011 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 *
 * Based on code from http://www.huuf.info/OV/
 * by Huuf. See project URL for complete author information.
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

import android.os.Parcelable;

import java.util.Calendar;

/**
 * Represents subscriptions on a card.  Subscriptions can be used to represent a number of different
 * things "loaded" on to the card.
 * <p>
 * Travel Pass or Season Pass: a card may, for example, allow travel passes that allow unlimited
 * travel (on certain modes of transport, or with certain operating companies) for a period of time
 * (eg: 7 days, 30 days, 1 year...)
 * <p>
 * Automatic top-up: a card may be linked to a credit card or other payment instrument, which will
 * be used to "top-up" or "refill" a card in the event a trip takes the balance below $0.
 */
public abstract class Subscription implements Parcelable {
    public abstract int getId();

    /**
     * When the subscription starts.
     * <p>
     * If null is returned, then the subscription has no start date.
     *
     * @return Calendar representing subscription start date, or null.
     */
    public abstract Calendar getValidFrom();

    /**
     * When the subscription ends.
     * <p>
     * If null is returned, then the subscription has never been used.  For example, a 7 day travel
     * pass may be loaded on the card as "available", but the travel pass has never been used, so
     * it will begin on the date that it is first used.
     *
     * @return Calendar representing the subcription date, or null if the subscription is unused.
     */
    public abstract Calendar getValidTo();

    public abstract String getAgencyName();

    public abstract String getShortAgencyName();

    public abstract int getMachineId();

    public abstract String getSubscriptionName();

    public abstract String getActivation();

    public final int describeContents() {
        return 0;
    }
}
