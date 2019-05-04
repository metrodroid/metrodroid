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

package au.id.micolous.metrodroid.transit.ezlinkcompat

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList
import java.util.Collections

import au.id.micolous.metrodroid.card.cepascompat.CEPASCard
import au.id.micolous.metrodroid.card.cepascompat.CEPASCompatPurse
import au.id.micolous.metrodroid.card.cepascompat.CEPASCompatTransaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData
import au.id.micolous.metrodroid.util.Utils

// This is only to read old dumps
actual class EZLinkCompatTransitData : TransitData {

    override val serialNumber: String?
    private val mBalance: Int
    private val mTrips: List<EZLinkCompatTrip>

    override val cardName: String
        get() = if (serialNumber != null) EZLinkTransitData.getCardIssuer(serialNumber) else "EZ"

    public override// This is stored in cents of SGD
    val balance: TransitCurrency?
        get() = TransitCurrency.SGD(mBalance)

    override val trips: List<EZLinkCompatTrip>?
        get() = mTrips

    private constructor(parcel: Parcel) {
        serialNumber = parcel.readString()
        mBalance = parcel.readInt()

        mTrips = ArrayList()
        parcel.readTypedList(mTrips, EZLinkCompatTrip.CREATOR)
    }

    actual constructor(cepasCard: CEPASCard) {
        serialNumber = Utils.getHexString(cepasCard.getPurse(3)?.can!!, "<Error>")
        mBalance = cepasCard.getPurse(3)!!.purseBalance
        mTrips = parseTrips(cepasCard)
    }

    private fun parseTrips(card: CEPASCard): List<EZLinkCompatTrip> {
        val transactions = card.getHistory(3)?.transactions
        if (transactions != null) {
            val trips = ArrayList<EZLinkCompatTrip>()

            for (transaction in transactions)
                trips.add(EZLinkCompatTrip(transaction, cardName))

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
        val CREATOR: Parcelable.Creator<EZLinkCompatTransitData> = object : Parcelable.Creator<EZLinkCompatTransitData> {
            override fun createFromParcel(parcel: Parcel): EZLinkCompatTransitData {
                return EZLinkCompatTransitData(parcel)
            }

            override fun newArray(size: Int): Array<EZLinkCompatTransitData?> {
                return arrayOfNulls(size)
            }
        }

        actual fun parseTransitIdentity(card: CEPASCard): TransitIdentity {
            val canNo = Utils.getHexString(
                    card.getPurse(3)!!.can!!, "<Error>")
            return TransitIdentity(EZLinkTransitData.getCardIssuer(canNo), canNo)
        }
    }

}
