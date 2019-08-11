/*
 * TransitData.kt
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

package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.NativeThrows
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator

abstract class TransitData : Parcelable {

    /**
     * Balance of the card's "purse".
     *
     * Cards with a single "purse" balance should implement this method, and
     * [TransitData.balances] will convert single-purse cards.  Most transit cards have
     * only one purse so should use this.
     *
     * Cards with multiple "purse" balances (generally: hybrid transit cards that can act as
     * multiple transit cards) must implement [TransitData.balances] instead.
     *
     * If the balance is not known, this does not need to be implemented.
     *
     * UI code must call [TransitData.balances] to get the balances of purses in the
     * card, even in the case there is only one purse.
     *
     * @see TransitData.balances
     * @return The balance of the card, or null if it is not known.
     */
    protected open val balance: TransitBalance?
        get() = null

    /**
     * Balances of the card's "purses".
     *
     * Cards with multiple "purse" balances (generally: hybrid transit cards that can act as
     * multiple transit cards) must implement this method, and must not implement
     * [TransitData.balance].
     *
     * Cards with a single "purse" balance should implement [TransitData.balance]
     * instead.
     *
     * @return The balance of the card, or null if it is not known.
     */
    open val balances: List<TransitBalance>?
        get() {
            val b = balance ?: return null
            return listOf(b)
        }

    abstract val serialNumber: String?

    /**
     * Lists all trips on the card.  May return null if the trip information cannot be read.
     *
     * @return List of Trip[], or null if not supported.
     */
    open val trips: List<Trip>?
        get() = null

    open val subscriptions: List<Subscription>?
        get() = null

    /**
     * Allows [TransitData] implementors to show extra information that doesn't fit within the
     * standard bounds of the interface.  By default, this returns null, so the "Info" tab will not
     * be displayed.
     *
     *
     * Note: in order to support obfuscation / hiding behaviour, if you implement this method, you
     * also need to use some other functionality:
     *
     *  * Check for [au.id.micolous.metrodroid.util.Preferences.hideCardNumbers] whenever you show a card
     * number, or other mark (such as a name) that could be used to identify this card or its
     * holder.
     *
     *  * Pass [au.id.micolous.metrodroid.time.Timestamp] objects (timestamps) through
     * [au.id.micolous.metrodroid.util.TripObfuscator.maybeObfuscateTS].  This also
     * works on epoch timestamps (expressed as seconds since UTC).
     *
     *  * Pass all currency amounts through
     * [TransitCurrency.formatCurrencyString] and
     * [TransitCurrency.maybeObfuscateBalance].
     *
     */
    open val info: List<ListItem>?
        get() = null

    abstract val cardName: String

    /**
     * You can optionally add a link to an FAQ page for the card.  This will be shown in the ...
     * drop down menu for cards that are supported, and on the main page for subclasses of
     * [au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData].
     *
     * @return Uri pointing to an FAQ page, or null if no page is to be supplied.
     */
    open val moreInfoPage: String?
        get() = null

    /**
     * You may optionally link to a page which allows you to view the online services for the card.
     *
     * @return Uri pointing to online services page, or null if no page is to be supplied.
     */
    open val onlineServicesPage: String?
        get() = null

    open val warning: String?
        get() = null

    /**
     * If a [TransitData] provider doesn't know some of the stops / stations on a user's card,
     * then it may raise a signal to the user to submit the unknown stations to our web service.
     *
     * @return false if all stations are known (default), true if there are unknown stations
     */
    open val hasUnknownStations: Boolean
        get() = false

    /**
     * Finds the timestamp of the latest trip taken on the card.
     * @return Latest timestamp of a trip, or null if there are no trips or no trips with
     * timestamps.
     */
    fun getLastUseDaystamp(): Daystamp? {
        // Find the last trip taken on the card.
        return trips?.mapNotNull { t -> t.endTimestamp ?: t.startTimestamp }?.map { it.toDaystamp() }
                ?.maxBy { it.daysSinceEpoch }
    }

    enum class RawLevel {
        NONE,
        UNKNOWN_ONLY,
        ALL;

        companion object {
            fun fromString(v: String): RawLevel? = values().find { it.toString() == v }
        }
    }

    // In Swift properties can't throw. Obfuscated trips never throw as they
    // copy all fields. So if safe is true, always pass trips through
    // obfuscation even if no real data changes
    @NativeThrows
    fun prepareTrips(safe: Boolean = false): List<Trip>? {
        val maybeObfuscatedTrips: List<Trip>
        if (Preferences.obfuscateTripDates ||
            Preferences.obfuscateTripTimes ||
            Preferences.obfuscateTripFares || safe) {
            maybeObfuscatedTrips = TripObfuscator.obfuscateTrips(trips ?: return null,
                Preferences.obfuscateTripDates,
                Preferences.obfuscateTripTimes,
                Preferences.obfuscateTripFares)
        } else
            maybeObfuscatedTrips = trips ?: return null
        // Explicitly sort these events
        return maybeObfuscatedTrips.sortedWith(Trip.Comparator())
    }

    open fun getRawFields(level: RawLevel): List<ListItem>? = null

    companion object {
        fun mergeInfo(transitData: TransitData): List<ListItem>? {
            val rawLevel = Preferences.rawLevel
            val inf = transitData.info
            if (rawLevel == RawLevel.NONE)
                return inf
            val rawInf = transitData.getRawFields(rawLevel) ?: return inf
            return inf.orEmpty() + listOf(HeaderListItem(Localizer.localizeString(R.string.raw_header))) + rawInf
        }

        fun hasInfo(transitData: TransitData): Boolean {
            if (transitData.info != null)
                return true
            val rawLevel = Preferences.rawLevel
            if (rawLevel == RawLevel.NONE)
                return false
            return transitData.getRawFields(rawLevel) != null
        }
    }
}
