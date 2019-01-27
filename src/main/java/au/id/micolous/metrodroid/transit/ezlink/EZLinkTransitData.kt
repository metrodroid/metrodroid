/*
 * EZLinkTransitData.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
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

package au.id.micolous.metrodroid.transit.ezlink

import android.os.Parcel
import android.os.Parcelable

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.cepas.CEPASApplication
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray

import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar
import java.util.Collections
import java.util.GregorianCalendar
import java.util.TimeZone

actual class EZLinkTransitData : TransitData {

    override val serialNumber: String?
    private val mBalance: Int
    private val mTrips: List<EZLinkTrip>

    override val cardName: String
        get() = getCardIssuer(serialNumber!!)

    public override// This is stored in cents of SGD
    val balance: TransitCurrency?
        get() = TransitCurrency.SGD(mBalance)

    override val trips: List<EZLinkTrip>?
        get() = mTrips

    private constructor(parcel: Parcel) {
        serialNumber = parcel.readString()
        mBalance = parcel.readInt()

        mTrips = ArrayList()
        parcel.readTypedList(mTrips, EZLinkTrip.CREATOR)
    }

    actual constructor(cepasCard: CEPASApplication) {
        val purse = CEPASPurse(cepasCard.getPurse(3))
        serialNumber = Utils.getHexString(purse.can, "<Error>")
        mBalance = purse.purseBalance
        mTrips = parseTrips(cepasCard)
    }

    private fun parseTrips(card: CEPASApplication): List<EZLinkTrip> {
        val history = CEPASHistory(card.getHistory(3))
        val transactions = history.transactions
        if (transactions != null) {
            val trips = ArrayList<EZLinkTrip>()

            for (transaction in transactions)
                trips.add(EZLinkTrip(transaction, cardName))

            return trips
        }
        return emptyList()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serialNumber)
        parcel.writeInt(mBalance)

        parcel.writeTypedList(mTrips)
    }

    override fun describeContents(): Int = 0

    actual companion object {
        @JvmStatic
        val CREATOR: Parcelable.Creator<EZLinkTransitData> = object : Parcelable.Creator<EZLinkTransitData> {
            override fun createFromParcel(parcel: Parcel): EZLinkTransitData {
                return EZLinkTransitData(parcel)
            }

            override fun newArray(size: Int): Array<EZLinkTransitData?> {
                return arrayOfNulls(size)
            }
        }
        private val EZLINK_STR = "ezlink"

        actual val EZ_LINK_CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.ezlink_card)
                .setName("EZ-Link")
                .setLocation(R.string.location_singapore)
                .setCardType(CardType.CEPAS)
                .build()

        private val NETS_FLASHPAY_CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.nets_card)
                .setName("NETS FlashPay")
                .setLocation(R.string.location_singapore)
                .setCardType(CardType.CEPAS)
                .build()
        val ALL_CARD_INFOS = Collections.unmodifiableList(Arrays.asList(
                EZLinkTransitData.EZ_LINK_CARD_INFO,
                EZLinkTransitData.NETS_FLASHPAY_CARD_INFO
        ))

        val TZ = TimeZone.getTimeZone("Asia/Singapore")
        private val EPOCH: Long

        init {
            val epoch = GregorianCalendar(TZ)
            epoch.set(1995, Calendar.JANUARY, 1, 0, 0, 0)

            EPOCH = epoch.timeInMillis
        }

        fun timestampToCalendar(timestamp: Long): Calendar {
            val c = GregorianCalendar(TZ)
            c.timeInMillis = EPOCH
            c.add(Calendar.SECOND, timestamp.toInt())
            return c
        }

        internal fun daysToCalendar(days: Int): Calendar {
            val c = GregorianCalendar(TZ)
            c.timeInMillis = EPOCH
            c.add(Calendar.DATE, days)
            return c
        }

        fun getCardIssuer(canNo: String): String {
            val issuerId = Integer.parseInt(canNo.substring(0, 3))
            when (issuerId) {
                100 -> return "EZ-Link"
                111 -> return "NETS"
                else -> return "CEPAS"
            }
        }

        fun getStation(code: String): Station {
            return if (code.length != 3) Station.unknown(code) else StationTableReader.getStation(EZLINK_STR,
                    ImmutableByteArray.fromASCII(code).byteArrayToInt(), code)
        }

        actual fun check(cepasCard: CEPASApplication): Boolean {
            return cepasCard.getHistory(3) != null && cepasCard.getPurse(3) != null
        }

        actual fun parseTransitIdentity(card: CEPASApplication): TransitIdentity {
            val purse = CEPASPurse(card.getPurse(3))
            val canNo = Utils.getHexString(purse.can, "<Error>")
            return TransitIdentity(getCardIssuer(canNo), canNo)
        }

        val notice: String?
            get() = StationTableReader.getNotice(EZLINK_STR)
    }
}
