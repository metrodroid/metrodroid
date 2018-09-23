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
import android.support.annotation.Nullable;

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
    public final int describeContents() {
        return 0;
    }

    /**
     * An identifier for the subscription number.
     *
     * If null, this will not be displayed.
     */
    @Nullable
    public Integer getId() {
        return null;
    }

    /**
     * When the subscription starts.
     * <p>
     * If null is returned, then the subscription has no start date.
     *
     * @return Calendar representing subscription start date, or null.
     */
    @Nullable
    public Calendar getValidFrom() {
        return null;
    }

    /**
     * When the subscription ends.
     * <p>
     * If null is returned, then the subscription has never been used, or there is no date limit.
     *
     * For example, a 7 day travel pass may be loaded on the card as "available", but the travel
     * pass has never been used, so it will begin on the date that it is first used.
     *
     * @return Calendar representing the subcription date, or null if the subscription has no end
     *         date.
     * @see #getSubscriptionState()
     */
    @Nullable
    public Calendar getValidTo() {
        return null;
    }

    /**
     * Show a time of day next to {@link #getValidTo()}.
     */
    public boolean validToHasTime() {
        return false;
    }

    @Nullable
    public String getAgencyName(boolean isShort) {
        return null;
    }

    /**
     * The machine ID for the terminal that sold the subscription, or null if unknown.
     *
     * Defaults to null.
     */
    @Nullable
    public Integer getMachineId() {
        return null;
    }

    public abstract String getSubscriptionName();

    /**
     * The number of passengers that the subscription is valid for. If a value greater than 1 is
     * supplied, then this will be displayed in the UI.
     */
    public int getPassengerCount() {
        return 1;
    }

    public enum SubscriptionState {
        /** No state is known, display no UI for the state. */
        UNKNOWN,

        /**
         * The subscription is present on the card, but currently disabled.
         */
        INACTIVE,

        /**
         * The subscription has been purchased, but never used.
         *
         * In systems where a subscription has a fixed start date, this state should only appear
         * if the card was scanned before the start date/time.
         *
         * In systems where a subscription does not have a fixed start date, or does not *yet* have
         * a fixed start date, this means that there are no trips recorded on this subscription.
         */
        UNUSED,

        /**
         * The subscription has been purchased, and has started.
         *
         * In systems where a subscription has a fixed start date, this state should appear if the
         * card was scanned after the start date/time.
         *
         * In systems where a subscription does not have a fixed start date, this state should
         * appear after the first trip has been taken.
         */
        STARTED,

        /**
         * The subscription has been "used up".
         *
         * This is the "final" state when there are a fixed number of trips associated with a
         * subscription, eg: the card holder bought 10 trips, and has taken 10 trips.
         */
        USED,

        /**
         * The subscription has expired.
         *
         * This is the "final" state when the card has been scanned after the end date/time of the
         * subscription.
         */
        EXPIRED,
    }

    public SubscriptionState getSubscriptionState() {
        return SubscriptionState.UNKNOWN;
    }

    /**
     * Where a subscription can be sold by a third party (such as a retailer), this is the name of
     * the retailer.
     *
     * By default, this returns null (and doesn't display any information).
     */
    @Nullable
    public String getSaleAgencyName() {
        return null;
    }

    /**
     * The timestamp that the subscription was purchased at, or null if not known.
     *
     * Returns null by default.
     */
    @Nullable
    public Calendar getPurchaseTimestamp() {
        return null;
    }

    /**
     * If {@link #getPurchaseTimestamp()} contains a valid time element, return true here.
     *
     * Otherwise, return false to hide the time component (default).
     */
    public boolean purchaseTimestampHasTime() {
        return false;
    }

    /**
     * The timestamp that the subscription was last used at, or null if not known.
     *
     * Returns null by default.
     */
    @Nullable
    public Calendar getLastUseTimestamp() {
        return null;
    }

    /**
     * If {@link #getLastUseTimestamp()} contains a valid time element, return true here.
     *
     * Otherwise, return false to hide the time component (default).
     */
    public boolean lastUseTimestampHasTime() {
        return false;
    }

    /**
     * The cost of the subscription, or null if unknown (default).
     */
    @Nullable
    public TransitCurrency cost() {
        return null;
    }

    public enum PaymentMethod {
        UNKNOWN,
        CASH,
        CREDIT_CARD,
        DEBIT_CARD,
        CHEQUE,
        TRANSIT_CARD,
    }

    /**
     * The method by which this subscription was purchased.  If {@link PaymentMethod#UNKNOWN}, then
     * nothing will be displayed.
     */
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.UNKNOWN;
    }

    /**
     * The total number of remaining trips in this subscription.
     *
     * If unknown or there is no limit to the number of trips, return null (default).
     */
    public Integer getRemainingTripCount() {
        return null;
    }

    /**
     * The total number of remaining days that this subscription can be used on.
     *
     * This is distinct to {@link #getValidTo()} -- this is for subscriptions where it can be used
     * on distinct but non-sequential days.
     *
     * Returns null if this is unknown, or there is no restriction.
     */
    public Integer getRemainingDayCount() {
        return null;
    }

    /**
     * Where a subscription limits the number of trips in a day that may be taken, this value
     * indicates the number of trips remaining.
     *
     * For example, if a subscription states that it may be used for 2 trips per day, then this will
     * go 2, 1, 0... as it is used up.
     *
     * Returns null if this is unknown, or there is no restriction.
     */
    public Integer getRemainingTripsInDayCount() {
        return null;
    }

    /**
     * An array of zone numbers for which the subscription is valid.
     *
     * Returns null if there are no restrictions, or the restrictions are unknown (default).
     */
    public int[] getZones() {
        return null;
    }

}
