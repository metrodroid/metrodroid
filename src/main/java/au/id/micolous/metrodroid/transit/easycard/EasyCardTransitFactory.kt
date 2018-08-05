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

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import au.id.micolous.farebot.R
import au.id.micolous.farebot.R.string.data
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.Utils
import java.util.*

class EasyCardTransitFactory(private val context: Context) {

    fun check(card: ClassicCard): Boolean {
        var data: ByteArray?
        try {
            data = (card.getSector(0))?.getBlock(1)?.data
        } catch (e: Exception) {
            data = null
        }
        return data != null && Utils.byteArrayToInt(data, 0, 4) == 0x0e140001
    }

    fun parseIdentity(card: ClassicCard): TransitIdentity {
        val uid = parseSerialNumber(card)
        return TransitIdentity("EasyCard", uid)
    }

    fun parseInfo(card: ClassicCard): EasyCardTransitData {
        return EasyCardTransitData(
                parseSerialNumber(card),
                parseManufacturingDate(card),
                parseBalance(card),
                parseTrips(card),
                parseRefill(card))
    }

    private fun parseSerialNumber(card: ClassicCard): String {
        val data = (card.getSector(0))?.getBlock(0)?.data!!
        return Utils.getHexString(Utils.byteArraySlice(data, 0, 4))
    }

    private fun parseBalance(card: ClassicCard): Int {
        val data = (card.getSector(2))?.getBlock(0)?.data
        return Utils.byteArrayToInt(data, 0, 1)
    }

    private fun parseRefill(card: ClassicCard): EasyCardRefill {
        val data = (card.getSector(2))?.getBlock(2)?.data!!

        val location = EasyCardStations[data[11].toInt()] ?: "EasyCard Unknown"
        val date = Utils.byteArrayToLong(Utils.reverseBuffer(Utils.byteArraySlice(data, 1, 5)))
        val amount = data[6].toInt()

        return EasyCardRefill(date, location, amount)
    }

    private fun parseTrips(card: ClassicCard): List<EasyCardTrip> {
        val blocks = (
                (card.getSector(3) as ClassicSector).blocks.subList(1, 3) +
                (card.getSector(4) as ClassicSector).blocks.subList(0, 3) +
                (card.getSector(5) as ClassicSector).blocks.subList(0, 3))
                .filter { !it.data.all { it == 0x0.toByte() } }

        return blocks.map { block ->
            val data = block.data
            val timestamp = Utils.byteArrayToLong(data.copyOfRange(1, 5).reversedArray())
            val fare = data[6].toInt()
            val balance = data[8].toLong()
            val transactionType = data[11].toInt()
            EasyCardTrip(timestamp, fare, balance, transactionType)
        }.distinctBy { it.timestamp }
    }

    private fun parseManufacturingDate(card: ClassicCard): Date {
        val data = card.getSector(0)?.getBlock(0)?.data
        return Date(Utils.byteArrayToLong(Utils.reverseBuffer(Utils.byteArraySlice(data, 5, 9))) * 1000L)
    }

    companion object {
        private val TZ: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
        val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.easycard)
                .setName("EasyCard")
                .setLocation(R.string.location_taipei)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .setPreview()
                .setExtraNote(R.string.easycard_card_note)
                .build()
    }

    public data class EasyCardRefill(
        private val timestamp: Long,
        private val location: String,
        private val amount: Int
    ) : Trip() {
        override fun describeContents(): Int = 0

        override fun hasTime(): Boolean = true

        override fun getMode(): Mode = Mode.TICKET_MACHINE

        constructor(parcel: Parcel) : this(
                parcel.readLong(),
                parcel.readString(),
                parcel.readInt()) {
        }

        override fun getStartTimestamp(): Calendar {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = timestamp * 1000
            return g
        }

        override fun getAgencyName(isShort : Boolean): String = location

        override fun getFare(): TransitCurrency = TransitCurrency(amount, "TWD")
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(timestamp)
            parcel.writeString(location)
            parcel.writeInt(amount)
        }

        companion object CREATOR : Parcelable.Creator<EasyCardRefill> {
            override fun createFromParcel(parcel: Parcel): EasyCardRefill {
                return EasyCardRefill(parcel)
            }

            override fun newArray(size: Int): Array<EasyCardRefill?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class EasyCardTrip(
            internal val timestamp: Long,
            private val fare: Int,
            private val balance: Long,
            private val transactionType: Int
    ) : Trip() {
        override fun getFare(): TransitCurrency? = TransitCurrency(fare,"TWD")

        constructor(parcel: Parcel) : this(
                parcel.readLong(),
                parcel.readInt(),
                parcel.readLong(),
                parcel.readInt()) {
        }

        override fun getStartTimestamp(): Calendar {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = timestamp * 1000
            return g
        }

        override fun getRouteName(): String? = EasyCardStations[transactionType]

        override fun getAgencyName(isShort : Boolean): String? = null

        override fun getStartStation(): Station? = null

        override fun getEndStation(): Station? = null

        override fun getMode(): Mode? = when (transactionType) {
            0x05 -> Mode.BUS
            0x01 -> Mode.POS
            else -> Mode.TRAIN
        }

        override fun hasTime(): Boolean = true
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(timestamp)
            parcel.writeInt(fare)
            parcel.writeLong(balance)
            parcel.writeInt(transactionType)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<EasyCardTrip> {
            override fun createFromParcel(parcel: Parcel): EasyCardTrip {
                return EasyCardTrip(parcel)
            }

            override fun newArray(size: Int): Array<EasyCardTrip?> {
                return arrayOfNulls(size)
            }
        }
    }
}
