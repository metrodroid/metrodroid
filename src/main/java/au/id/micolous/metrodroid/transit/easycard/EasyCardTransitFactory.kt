/*
 * EasyCardTransitFactory.kt
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://www.fuzzysecurity.com/tutorials/rfid/4.html
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
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
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.transit.*

class EasyCardTransitFactory : ClassicCardTransitFactory() {
    override fun check(card: ClassicCard): Boolean {
        val data: ByteArray? = try {
            (card.getSector(0))?.getBlock(1)?.data
        } catch (e: Exception) {
            null
        }

        val x = data != null && Arrays.equals(data, MAGIC)
        return x
    }

    override fun earlySectors(): Int {
        return 0
    }

    override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
        val uid = parseSerialNumber(card)
        return TransitIdentity(NAME, uid)
    }

    override fun parseTransitData(card: ClassicCard): TransitData {
        return EasyCardTransitData(
                parseSerialNumber(card),
                parseBalance(card),
                parseTrips(card),
                parseRefill(card))
    }

    private fun parseSerialNumber(card: ClassicCard): String {
        val data = (card.getSector(0))?.getBlock(0)?.data!!
        return Utils.getHexString(data, 0, 4)
    }

    private fun parseBalance(card: ClassicCard): Int {
        val data = (card.getSector(2))?.getBlock(0)?.data
        return Utils.byteArrayToIntReversed(data, 0, 4)
    }

    private fun parseRefill(card: ClassicCard): EasyCardTrip {
        val data = (card.getSector(2))?.getBlock(2)?.data!!

        val id = data[11].toInt()
        val date = Utils.byteArrayToLongReversed(data, 1, 4)
        val amount = Utils.byteArrayToIntReversed(data, 6, 2)

        return EasyCardTrip(date, -amount, id, true, false)
    }

    private fun parseTrips(card: ClassicCard): List<EasyCardTrip> {
        val blocks = (
                (card.getSector(3) as ClassicSector).blocks.subList(1, 3) +
                (card.getSector(4) as ClassicSector).blocks.subList(0, 3) +
                (card.getSector(5) as ClassicSector).blocks.subList(0, 3))
                .filter { !it.data.all { it == 0x0.toByte() } }

        var trips = blocks.map { block ->
            val data = block.data
            val timestamp = Utils.byteArrayToLongReversed(data, 1, 4)
            val fare = Utils.byteArrayToIntReversed(data, 6, 2)
            val transactionType = data[11].toInt()
            EasyCardTrip(timestamp, fare, transactionType, false,
                    data[5] == 0x11.toByte())
        }.distinctBy { it.timestamp }

        Collections.sort(trips, Trip.Comparator())
        Collections.reverse(trips)

        var mergedTrips = ArrayList<EasyCardTrip>()

        for (trip in trips) {
            if (mergedTrips.isEmpty()) {
                mergedTrips.add(trip)
                continue
            }
            var lastTrip = mergedTrips.get(mergedTrips.size - 1)
            if (lastTrip.shouldBeMerged(trip))
                lastTrip.merge(trip)
            else
                mergedTrips.add(trip)
        }

        return mergedTrips
    }

    companion object {
        private val TZ: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
        private const val NAME = "EasyCard"
        val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.easycard)
                .setName(NAME)
                .setLocation(R.string.location_taipei)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .setPreview()
                .build()

        private val MAGIC = byteArrayOf(
                0x0e, 0x14, 0x00, 0x01,
                0x07, 0x02, 0x08, 0x03,
                0x09, 0x04, 0x08, 0x10,
                0x00, 0x00, 0x00, 0x00)

        private const val EASYCARD_STR: String = "easycard"
        private const val POS = 1
        private const val BUS = 5
    }

    @Parcelize
    data class EasyCardTransitData(
            private val serialNumber: String,
            private val balance: Int,
            private val trips: List<EasyCardTransitFactory.EasyCardTrip>,
            private val refill: EasyCardTransitFactory.EasyCardTrip
    ) : TransitData() {
        override fun getBalance(): TransitCurrency = TransitCurrency.TWD(balance)

        override fun getCardName(): String = NAME

        override fun getSerialNumber(): String? = serialNumber

        override fun getTrips(): Array<out Trip> {
            val ret: ArrayList<Trip> = ArrayList()
            ret.addAll(trips)
            ret.add(refill)
            return ret.toArray(arrayOf())
        }
    }

    @Parcelize
    data class EasyCardTrip(
            internal val timestamp: Long,
            private var fare: Int,
            private val location: Int,
            private val isTopup: Boolean,
            private val isTapOff: Boolean,
            private var exitTimestamp: Long? = null,
            private var exitLocation: Int? = null
    ) : Trip() {
        override fun getFare(): TransitCurrency? = TransitCurrency.TWD(fare)

        override fun getStartTimestamp(): Calendar {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = timestamp * 1000
            return g
        }

        override fun getEndTimestamp(): Calendar? {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = (exitTimestamp ?: return null) * 1000
            return g
        }

        override fun getStartStation(): Station? = when (location) {
            BUS -> null
            POS -> null
            else -> StationTableReader.getStation(EASYCARD_STR, location)
        }

        override fun getEndStation(): Station? {
            return StationTableReader.getStation(EASYCARD_STR, exitLocation ?: return null)
        }

        override fun getMode(): Mode {
            if (isTopup)
                return Mode.TICKET_MACHINE
            return when (location) {
                BUS -> Mode.BUS
                POS -> Mode.POS
                else -> Mode.METRO
            }
        }

        fun shouldBeMerged(trip: EasyCardTrip): Boolean {
            if (location == POS || location == BUS
                    || trip.location == POS || trip.location == BUS
            || trip.exitTimestamp != null || exitTimestamp != null) {
                return false
            }
            return (!isTapOff && trip.isTapOff)
        }

        fun merge(trip: EasyCardTrip) {
            fare += trip.fare
            exitLocation = trip.location
            exitTimestamp = trip.timestamp
        }

        override fun getRouteName(): String?  = when (mode) {
            Mode.METRO -> super.getRouteName()
            // Ticket machines would otherwise inherit a line from the Station.
            else -> null
        }
    }
}
