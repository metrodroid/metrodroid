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
import au.id.micolous.metrodroid.multi.NativeThrows
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
data class TransitDataStored internal constructor(
    override val balances: List<TransitBalance>?,
    override val serialNumber: String?,
    override val trips: List<Trip>?,
    override val subscriptions: List<Subscription>?,
    override val info: List<ListItem>?,
    override val cardName: String,
    override val moreInfoPage: String?,
    override val onlineServicesPage: String?,
    override val warning: String?,
    override val hasUnknownStations: Boolean,
    val rawFieldsAll: List<ListItem>?,
    val rawFieldsUnknown: List<ListItem>?
): TransitData() {

    override fun getRawFields(level: RawLevel): List<ListItem>? = when(level) {
        RawLevel.NONE -> null
        RawLevel.UNKNOWN_ONLY -> rawFieldsUnknown
        RawLevel.ALL -> rawFieldsAll
    }

    object Storer {
        // Nullable Throwable is a pain for kotlin-swift interop, hence have
        // this wrapper to create a non-throwing TransitData with the same
        // data as original except for trips being already prepared
        @NativeThrows
        fun store(original: TransitData): TransitDataStored = TransitDataStored(
            balances = original.balances,
            serialNumber = original.serialNumber,
            trips = original.prepareTrips(safe=true),
            subscriptions = original.subscriptions,
            info = original.info,
            cardName = original.cardName,
            moreInfoPage = original.moreInfoPage,
            onlineServicesPage = original.onlineServicesPage,
            warning = original.warning,
            hasUnknownStations = original.hasUnknownStations,
            rawFieldsAll = original.getRawFields(RawLevel.ALL),
            rawFieldsUnknown = original.getRawFields(RawLevel.UNKNOWN_ONLY))

        @NativeThrows
        fun parse(card: Card): TransitDataStored? = card.parseTransitData()?.let { store(it) }
    }
}
