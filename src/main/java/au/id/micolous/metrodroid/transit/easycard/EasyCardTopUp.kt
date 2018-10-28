/*
 * EasyCardTransitData.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
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

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class EasyCardTopUp(
        internal val timestamp: Long,
        private val amount: Int,
        private val location: Int
) : Trip() {
    override fun getFare(): TransitCurrency? = TransitCurrency.TWD(-amount)

    override fun getStartTimestamp(): Calendar {
        val g = GregorianCalendar(EasyCardTransitData.TZ)
        g.timeInMillis = timestamp * 1000
        return g
    }

    override fun getStartStation(): Station? =
            StationTableReader.getStation(EasyCardTransitData.EASYCARD_STR, location)

    override fun getMode(): Mode = Mode.TICKET_MACHINE

    override fun getRouteName(): String? = null

    companion object {
        fun parse(card: ClassicCard): EasyCardTopUp {
            val data = (card.getSector(2))?.getBlock(2)?.data!!

            val id = data[11].toInt()
            val date = Utils.byteArrayToLongReversed(data, 1, 4)
            val amount = Utils.byteArrayToIntReversed(data, 6, 2)

            return EasyCardTopUp(date, amount, id)
        }
    }
}