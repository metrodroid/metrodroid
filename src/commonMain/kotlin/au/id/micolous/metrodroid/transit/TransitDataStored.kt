/*
 * StoredTransitData.kt
 *
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.logAndSwiftWrap
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.ObfuscatedTrip
import kotlinx.serialization.Serializable

@Parcelize
@Suppress("unused") // Used from Swift and tests
@Serializable
data class TransitDataStored internal constructor(
    override val balances: List<TransitBalance>? = null,
    override val serialNumber: String? = null,
    override val trips: List<ObfuscatedTrip>? = null,
    override val subscriptions: List<StoredSubscription>? = null,
    override val info: List<ListItemInterface>? = null,
    override val cardName: String,
    override val moreInfoPage: String? = null,
    override val onlineServicesPage: String? = null,
    override val warning: String? = null,
    override val hasUnknownStations: Boolean,
    val rawFieldsAll: List<ListItemInterface>? = null,
    val rawFieldsUnknown: List<ListItemInterface>? = null
): TransitData() {

    override fun getRawFields(level: RawLevel): List<ListItemInterface>? = when(level) {
        RawLevel.NONE -> null
        RawLevel.UNKNOWN_ONLY -> rawFieldsUnknown
        RawLevel.ALL -> rawFieldsAll
    }
    
    @Serializable
    @Parcelize
    class StoredSubscription(
        override val id: Int? = null,
        override val validFrom: Timestamp? = null,
        override val validTo: Timestamp? = null,
        override val machineId: Int? = null,
        override val subscriptionName: String? = null,
        override val passengerCount: Int,
        override val subscriptionState: SubscriptionState,
        override val saleAgencyName: FormattedString? = null,
        override val purchaseTimestamp: Timestamp? = null,
        override val lastUseTimestamp: Timestamp? = null,
        override val paymentMethod: PaymentMethod,
        override val remainingTripCount: Int? = null,
        override val totalTripCount: Int? = null,
        override val remainingDayCount: Int? = null,
        override val remainingTripsInDayCount: Int? = null,
        override val zones: IntArray? = null,
        override val transferEndTimestamp: Timestamp? = null,
        val rawFieldsUnknown: List<ListItemInterface>? = null,
        val rawFieldsFull: List<ListItemInterface>? = null,
        override val info: List<ListItemInterface>? = null,
        val agencyName: FormattedString? = null,
        val agencyNameShort: FormattedString? = null,
        override val cost: TransitCurrency? = null
    ): Subscription() {
        constructor(base: Subscription) : this(
            id = base.id,
            validFrom = base.validFrom,
            validTo = base.validTo,
            machineId = base.machineId,
            subscriptionName = base.subscriptionName,
            passengerCount = base.passengerCount,
            subscriptionState = base.subscriptionState,
            saleAgencyName = base.saleAgencyName,
            purchaseTimestamp = base.purchaseTimestamp,
            lastUseTimestamp = base.lastUseTimestamp,
            paymentMethod = base.paymentMethod,
            remainingTripCount = base.remainingTripCount,
            totalTripCount = base.totalTripCount,
            remainingDayCount = base.remainingDayCount,
            remainingTripsInDayCount = base.remainingTripsInDayCount,
            zones = base.zones,
            transferEndTimestamp = base.transferEndTimestamp,
            rawFieldsUnknown = base.getRawFields(RawLevel.UNKNOWN_ONLY),
            rawFieldsFull = base.getRawFields(RawLevel.ALL),
            info = base.info,
            agencyName = base.getAgencyName(false),
            agencyNameShort = base.getAgencyName(true),
            cost = base.cost
        )

    }

    object Storer {
        // Nullable Throwable is a pain for kotlin-swift interop, hence have
        // this wrapper to create a non-throwing TransitData with the same
        // data as original except for trips being already prepared
        @Throws(Throwable::class)
        fun store(original: TransitData): TransitDataStored = TransitDataStored(
            balances = original.balances,
            serialNumber = original.serialNumber,
            trips = original.prepareTripsSafe(),
            subscriptions = original.subscriptions?.map { StoredSubscription(it) },
            info = original.info,
            cardName = original.cardName,
            moreInfoPage = original.moreInfoPage,
            onlineServicesPage = original.onlineServicesPage,
            warning = original.warning,
            hasUnknownStations = original.hasUnknownStations,
            rawFieldsAll = original.getRawFields(RawLevel.ALL),
            rawFieldsUnknown = original.getRawFields(RawLevel.UNKNOWN_ONLY))

        @Throws(Throwable::class)
        fun parse(card: Card): TransitDataStored? = logAndSwiftWrap ("TransitDataStore", "Failed to parse") {
            card.parseTransitData()?.let { store(it) }
        }
    }
}
