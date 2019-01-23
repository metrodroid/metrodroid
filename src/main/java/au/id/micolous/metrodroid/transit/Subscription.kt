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

package au.id.micolous.metrodroid.transit

import android.os.Parcelable
import android.support.annotation.StringRes

import java.util.ArrayList
import java.util.Calendar

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.TripObfuscator
import au.id.micolous.metrodroid.util.Utils

/**
 * Represents subscriptions on a card.  Subscriptions can be used to represent a number of different
 * things "loaded" on to the card.
 *
 *
 * Travel Pass or Season Pass: a card may, for example, allow travel passes that allow unlimited
 * travel (on certain modes of transport, or with certain operating companies) for a period of time
 * (eg: 7 days, 30 days, 1 year...)
 *
 *
 * Automatic top-up: a card may be linked to a credit card or other payment instrument, which will
 * be used to "top-up" or "refill" a card in the event a trip takes the balance below $0.
 */
abstract class Subscription : Parcelable {

    /**
     * An identifier for the subscription number.
     *
     * If null, this will not be displayed.
     */
    open val id: Int?
        get() = null

    /**
     * When the subscription starts.
     *
     *
     * If null is returned, then the subscription has no start date.
     *
     * @return Calendar representing subscription start date, or null.
     */
    open val validFrom: Calendar?
        get() = null

    /**
     * When the subscription ends.
     *
     *
     * If null is returned, then the subscription has never been used, or there is no date limit.
     *
     * For example, a 7 day travel pass may be loaded on the card as "available", but the travel
     * pass has never been used, so it will begin on the date that it is first used.
     *
     * @return Calendar representing the subscription date, or null if the subscription has no end
     * date.
     * @see .getSubscriptionState
     */
    open val validTo: Calendar?
        get() = null

    /**
     * The machine ID for the terminal that sold the subscription, or null if unknown.
     *
     * Defaults to null.
     */
    open val machineId: Int?
        get() = null

    /**
     * A name (or description) of the subscription.
     *
     * eg: "Travel Ten", "Multi-trip", "City Pass"...
     */
    open val subscriptionName: String?
        get() = null

    /**
     * The number of passengers that the subscription is valid for. If a value greater than 1 is
     * supplied, then this will be displayed in the UI.
     */
    open val passengerCount: Int
        get() = 1

    open val subscriptionState: SubscriptionState
        get() = SubscriptionState.UNKNOWN

    /**
     * Where a subscription can be sold by a third party (such as a retailer), this is the name of
     * the retailer.
     *
     * By default, this returns null (and doesn't display any information).
     */
    open val saleAgencyName: String?
        get() = null

    /**
     * The timestamp that the subscription was purchased at, or null if not known.
     *
     * Returns null by default.
     */
    open val purchaseTimestamp: Calendar?
        get() = null

    /**
     * The timestamp that the subscription was last used at, or null if not known.
     *
     * Returns null by default.
     */
    open val lastUseTimestamp: Calendar?
        get() = null

    /**
     * The method by which this subscription was purchased.  If [PaymentMethod.UNKNOWN], then
     * nothing will be displayed.
     */
    open val paymentMethod: PaymentMethod
        get() = PaymentMethod.UNKNOWN

    /**
     * The total number of remaining trips in this subscription.
     *
     * If unknown or there is no limit to the number of trips, return null (default).
     */
    open val remainingTripCount: Int?
        get() = null

    /**
     * The total number of trips in this subscription.
     *
     * If unknown or there is no limit to the number of trips, return null (default).
     */
    open val totalTripCount: Int?
        get() = null

    /**
     * The total number of remaining days that this subscription can be used on.
     *
     * This is distinct to [.getValidTo] -- this is for subscriptions where it can be used
     * on distinct but non-sequential days.
     *
     * Returns null if this is unknown, or there is no restriction.
     */
    open val remainingDayCount: Int?
        get() = null

    /**
     * Where a subscription limits the number of trips in a day that may be taken, this value
     * indicates the number of trips remaining on the day of last use,
     * [.getLastUseTimestamp].
     *
     * For example, if a subscription states that it may be used for 2 trips per day, then this will
     * go 2, 1, 0... as it is used up.
     *
     * Returns null if this is unknown, or there is no restriction.
     */
    open val remainingTripsInDayCount: Int?
        get() = null

    /**
     * An array of zone numbers for which the subscription is valid.
     *
     * Returns null if there are no restrictions, or the restrictions are unknown (default).
     */
    open val zones: IntArray?
        get() = null

    /**
     * For networks that allow transfers (ie: multiple vehicles may be used as part of a single trip
     * and charged at a flat rate), this shows the latest time that transfers may be made.
     *
     * For example, a subscription may allow 2 trips per day, but allow a 2 hour transfer window. So
     * a passenger who boards their first vehicle at 10:00, then leaves the vehicle at 10:30, could
     * then board a second vehicle at 10:45 and still have it counted as the first "trip".
     *
     * Returns null if there is no such functionality, or the restrictions are unknown (default).
     */
    open val transferEndTimestamp: Calendar?
        get() = null

    /**
     * Allows [Subscription] implementors to show extra information that doesn't fit within the
     * standard bounds of the interface.  By default, this attempts to collect less common
     * attributes, and put them in here.
     *
     *
     * Note: in order to support obfuscation / hiding behaviour, if you implement this method, you
     * also need to use some other functionality:
     *
     *  * Check for [] whenever you show a card
     * number, or other mark (such as a name) that could be used to identify this card or its
     * holder.
     *
     *  * Pass [java.util.Calendar]/[java.util.Date] objects (timestamps) through
     * [au.id.micolous.metrodroid.util.TripObfuscator.maybeObfuscateTS].  This also
     * works on epoch timestamps (expressed as seconds since UTC).
     *
     *  * Pass all currency amounts through
     * [TransitCurrency.formatCurrencyString] and
     * [TransitCurrency.maybeObfuscateBalance].
     *
     */
    // TODO: i18n
    open val info: List<ListItem>?
        get() {
            val items = ArrayList<ListItem>()
            if (saleAgencyName != null) {
                items.add(ListItem(R.string.seller_agency, saleAgencyName))
            }

            if (machineId != null) {
                items.add(ListItem(R.string.machine_id,
                        Integer.toString(machineId!!)))
            }

            var purchaseTS = purchaseTimestamp
            if (purchaseTS != null) {
                purchaseTS = TripObfuscator.maybeObfuscateTS(purchaseTS)

                items.add(ListItem(R.string.purchase_date, if (purchaseTimestampHasTime())
                    Utils.dateTimeFormat(purchaseTS)
                else
                    Utils.longDateFormat(purchaseTS)))
            }

            var lastUseTS = lastUseTimestamp
            if (lastUseTS != null) {
                lastUseTS = TripObfuscator.maybeObfuscateTS(lastUseTS)

                items.add(ListItem(R.string.last_used_on, if (lastUseTimestampHasTime())
                    Utils.dateTimeFormat(lastUseTS)
                else
                    Utils.longDateFormat(lastUseTS)))
            }

            val cost = cost()
            if (cost != null) {
                items.add(ListItem(R.string.subscription_cost,
                        cost.maybeObfuscateFare().formatCurrencyString(true)))
            }

            val id = id
            if (id != null) {
                items.add(ListItem(R.string.subscription_id, Integer.toString(id)))
            }

            if (paymentMethod != PaymentMethod.UNKNOWN) {
                items.add(ListItem(R.string.payment_method, paymentMethod.description))
            }

            var transferEndTimestamp = transferEndTimestamp
            if (transferEndTimestamp != null && lastUseTS != null) {
                transferEndTimestamp = TripObfuscator.maybeObfuscateTS(transferEndTimestamp)

                items.add(ListItem(R.string.free_transfers_until,
                        Utils.dateTimeFormat(transferEndTimestamp)))
            }

            val counter = remainingTripsInDayCount
            if (counter != null && lastUseTS != null) {
                items.add(ListItem(R.string.remaining_trip_count, Utils.localizePlural(
                        R.plurals.remaining_trip_on_day, counter,
                        remainingTripsInDayCount, Utils.longDateFormat(lastUseTS))))
            }

            val zones = zones
            if (zones != null && zones.size > 0) {
                val zones_list = StringBuilder()
                for (z in zones) {
                    if (zones_list.length != 0)
                        zones_list.append(", ")
                    zones_list.append(Integer.toString(z))
                }

                items.add(ListItem(Utils.localizePlural(R.plurals.travel_zones,
                        zones.size), zones_list.toString()))
            }

            return if (!items.isEmpty()) items else null
        }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Show a time of day next to [.getValidTo].
     */
    open fun validToHasTime(): Boolean {
        return false
    }

    open fun getAgencyName(isShort: Boolean): String? {
        return null
    }

    enum class SubscriptionState private constructor(@param:StringRes @field:StringRes @get:StringRes
                                                     val description: Int) {
        /** No state is known, display no UI for the state.  */
        UNKNOWN(R.string.unknown),

        /**
         * The subscription is present on the card, but currently disabled.
         */
        INACTIVE(R.string.subscription_inactive),

        /**
         * The subscription has been purchased, but never used.
         *
         * In systems where a subscription has a fixed start date, this state should only appear
         * if the card was scanned before the start date/time.
         *
         * In systems where a subscription does not have a fixed start date, or does not *yet* have
         * a fixed start date, this means that there are no trips recorded on this subscription.
         */
        UNUSED(R.string.subscription_unused),

        /**
         * The subscription has been purchased, and has started.
         *
         * In systems where a subscription has a fixed start date, this state should appear if the
         * card was scanned after the start date/time.
         *
         * In systems where a subscription does not have a fixed start date, this state should
         * appear after the first trip has been taken.
         */
        STARTED(R.string.subscription_started),

        /**
         * The subscription has been "used up".
         *
         * This is the "final" state when there are a fixed number of trips associated with a
         * subscription, eg: the card holder bought 10 trips, and has taken 10 trips.
         */
        USED(R.string.subscription_used),

        /**
         * The subscription has expired.
         *
         * This is the "final" state when the card has been scanned after the end date/time of the
         * subscription.
         */
        EXPIRED(R.string.subscription_expired)
    }

    /**
     * If [.getPurchaseTimestamp] contains a valid time element, return true here.
     *
     * Otherwise, return false to hide the time component (default).
     */
    open fun purchaseTimestampHasTime(): Boolean {
        return false
    }

    /**
     * If [.getLastUseTimestamp] contains a valid time element, return true here.
     *
     * Otherwise, return false to hide the time component (default).
     */
    open fun lastUseTimestampHasTime(): Boolean {
        return false
    }

    /**
     * The cost of the subscription, or null if unknown (default).
     */
    open fun cost(): TransitCurrency? {
        return null
    }

    /**
     * Describes payment methods for a [Subscription].
     */
    enum class PaymentMethod private constructor(@param:StringRes @field:StringRes @get:StringRes
                                                 val description: Int) {
        UNKNOWN(R.string.unknown),
        CASH(R.string.payment_method_cash),
        CREDIT_CARD(R.string.payment_method_credit_card),
        DEBIT_CARD(R.string.payment_method_debit_card),
        CHEQUE(R.string.payment_method_cheque),
        /** The payment is made using stored balance on the transit card itself.  */
        TRANSIT_CARD(R.string.payment_method_transit_card),
        /** The subscription costs nothing (gratis)  */
        FREE(R.string.payment_method_free)
    }
}
