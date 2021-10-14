/*
 * TransitData.kt
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.TripObfuscator

abstract class TransitData : Parcelable {

    /**
     * The (currency) balance of the card's _purse_. Most cards have only one purse.
     *
     * By default, this returns `null`, so no balance information will be displayed (and it is presumed unknown).
     *
     * If a card **only** stores season passes or a number of credited rides (instead of currency), return `null`.
     *
     * Cards with more than one purse must implement [balances] instead.
     *
     * Cards with a single purse _should_ implement this method, and [TransitData.balance] will automatically handle
     * the conversion.
     *
     * Note: This method is protected -- callers must use [balances], even in the case there is only one purse.
     *
     * @see balances
     * @return The balance of the card, or `null` if it is not known.
     */
    protected open val balance: TransitBalance?
        get() = null

    /**
     * The (currency) balances of all of the card's _purses_.
     *
     * Cards with multiple "purse" balances (generally: hybrid transit cards that can act as multiple transit cards)
     * must implement this method, and must not implement [balance].
     *
     * Cards with a single "purse" balance should implement [balance] instead -- the default implementation in
     * [TransitData] will automatically up-convert it.
     *
     * @return The purse balance(s) of the card, or `null` if it is not known.
     */
    open val balances: List<TransitBalance>?
        get() {
            val b = balance ?: return null
            return listOf(b)
        }

    /**
     * The serial number of the card. Generally printed on the card itself, or shown on receipts from ticket vending
     * machines.
     *
     * If a card has more than one serial number, then show the second serial number in [info].
     */
    abstract val serialNumber: String?

    /**
     * Lists all trips on the card.
     *
     * If the transit card does not store trip information, or the [TransitData] implementation does not support
     * reading trips yet, return `null`.
     *
     * If the [TransitData] implementation supports reading trip data, but no trips have been taken on the card, return
     * an [emptyList].
     *
     * Note: UI elements must use [prepareTrips] instead, which implements obfuscation.
     *
     * @return A [List] of [Trip]s, or `null` if not supported.
     * @see [prepareTrips]
     */
    open val trips: List<Trip>?
        get() = null

    open val subscriptions: List<Subscription>?
        get() = null

    /**
     * Allows [TransitData] implementors to show extra information that doesn't fit within the standard bounds of the
     * interface. By default, this returns `null`, which hides the "Info" tab.
     *
     * Implementors **must** implement obfuscation through this method:
     *
     * * Check for [Preferences.hideCardNumbers] whenever you show a card number, or other mark (such as name, height,
     *   weight, birthday or gender) that could be used to identify this card or its holder.
     *
     * * Pass dates and times ([Timestamp] and [Daystamp]) through [TripObfuscator.maybeObfuscateTS].
     *
     * * Pass all currency amounts through [TransitCurrency.formatCurrencyString] and
     *   [TransitCurrency.maybeObfuscateBalance].
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
     * By default, this returns `null`, which causes the menu item to be removed.
     *
     * @return Uri to card's online services page.
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
     * Finds the [Daystamp] of the latest [Trip] taken on the card. This value is **not** obfuscated.
     *
     * This is helpful for cards that define their [balance] validity period as "_n_ years since last use".
     *
     * @return Latest timestamp of a trip, or `null` if there are no trips or no trips with timestamps.
     */
    fun getLastUseDaystamp(): Daystamp? {
        // Find the last trip taken on the card.
        return trips?.mapNotNull { t -> t.endTimestamp ?: t.startTimestamp }?.map { it.toDaystamp() }
                ?.maxByOrNull { it.daysSinceEpoch }
    }

    /**
     * Declares levels of raw information to display, useful for development and debugging.
     */
    enum class RawLevel {
        /** Display no extra information (default). */
        NONE,
        /** Display only unknown fields, or fields that are not displayed in other contexts. */
        UNKNOWN_ONLY,
        /** Display all fields, even ones that are decoded in other contexts. */
        ALL;

        companion object {
            fun fromString(v: String): RawLevel? = values().find { it.toString() == v }
        }
    }

    /**
     * Prepares a list of trips for display in the UI.
     *
     * This will obfuscate trip details if the user has enabled one of the trip obfuscation [Preferences].
     *
     * @param safe When `false` (default), the exact [trips] will be returned verbatim if no obfuscation [Preferences]
     * have been enabled.
     *
     * When `true`, [trips] is passed through [TripObfuscator] regardless of whether the user has enabled one
     * of the trip obfuscation [Preferences]. If no obfuscation flags are enabled, [TripObfuscator] will simply copy
     * all fields verbatim.
     *
     * This is required for Swift interop, as properties can't throw exceptions in Swift.
     *
     * @return A list of trips for display purposes. If [trips] is null, this also returns null.
     *
     * @see [trips], [TripObfuscator.obfuscateTrips]
     */
    @Throws(Throwable::class)
    fun prepareTrips(safe: Boolean = false): List<Trip>?  = logAndSwiftWrap ("TransitData", "prepareTrips failed") lam@{
        val trips = this.trips ?: return@lam null

        val maybeObfuscatedTrips = if (safe ||
                Preferences.obfuscateTripDates ||
                Preferences.obfuscateTripTimes ||
                Preferences.obfuscateTripFares) {
            TripObfuscator.obfuscateTrips(trips,
                    Preferences.obfuscateTripDates,
                    Preferences.obfuscateTripTimes,
                    Preferences.obfuscateTripFares)
        } else {
            trips
        }

        // Explicitly sort these events
        return@lam maybeObfuscatedTrips.sortedWith(Trip.Comparator())
    }

    /**
     * Raw field information from the card, which is only useful for development and debugging. By default, this
     * returns `null`.
     *
     * This has the same semantics as [info], except obfuscation is never used on these fields.
     *
     * This is only called when [Preferences.rawLevel] is not [RawLevel.NONE].
     *
     * @param level Level of detail requested.
     * @see [info]
     */
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
