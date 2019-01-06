/*
 * EasyCardTopUp.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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
package au.id.micolous.metrodroid.transit.easycard

import android.support.annotation.VisibleForTesting
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EasyCardTopUp(
        internal val timestamp: Long,
        private val amount: Int,
        private val location: Int,
        private val machineId: Long
) : Trip() {
    @VisibleForTesting
    constructor(data: ByteArray) : this(
            Utils.byteArrayToLongReversed(data, 1, 4),
            Utils.byteArrayToIntReversed(data, 6, 2),
            data[11].toInt(),
            Utils.byteArrayToLongReversed(data, 12, 4)
    )

    override fun getFare() = TransitCurrency.TWD(-amount)

    override fun getStartTimestamp() = EasyCardTransitData.parseTimestamp(timestamp)

    override fun getStartStation(): Station? =
            StationTableReader.getStation(EasyCardTransitData.EASYCARD_STR, location)

    override fun getMode() = Mode.TICKET_MACHINE

    override fun getRouteName(): String? = null

    override fun getHumanReadableRouteID(): String? = null

    override fun getMachineID() = "0x${machineId.toString(16)}"

    companion object {
        fun parse(card: ClassicCard) = EasyCardTopUp(card[2, 2].data)
    }
}