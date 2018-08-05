/*
 * EasyCardTransitInfo.kt
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

import android.os.Parcel
import android.os.Parcelable
import android.text.Spanned
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Utils
import java.util.*

data class EasyCardTransitData(
        private val serialNumber: String,
        private val manufacturingDate: Date,
        private val balance: Int,
        private val trips: List<EasyCardTransitFactory.EasyCardTrip>,
        private val refill: EasyCardTransitFactory.EasyCardRefill
) : TransitData() {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            Date(parcel.readLong()),
            parcel.readInt(),
            parcel.createTypedArrayList(EasyCardTransitFactory.EasyCardTrip.CREATOR),
            parcel.readParcelable(EasyCardTransitFactory.EasyCardRefill::class.java.classLoader)) {
    }

    override fun getBalance(): TransitCurrency = TransitCurrency(balance, "TWD")

    override fun getCardName(): String = "EasyCard"

    override fun getSerialNumber(): String? = serialNumber

    override fun getTrips(): Array<out Trip> {
        var ret: ArrayList<Trip>
        ret = ArrayList()
        ret.addAll(trips)
        ret.add(refill)
        return ret.toArray(arrayOf())
    }

    override fun getSubscriptions(): Array<out Subscription>? = arrayOf()

    override fun getInfo(): MutableList<ListItem> =
        mutableListOf(ListItem(R.string.easycard_manufactoring_date, "" + manufacturingDate))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serialNumber)
        parcel.writeLong(manufacturingDate.time)
        parcel.writeInt(balance)
        parcel.writeTypedList(trips)
        parcel.writeParcelable(refill, flags)
    }

    companion object CREATOR : Parcelable.Creator<EasyCardTransitData> {
        override fun createFromParcel(parcel: Parcel): EasyCardTransitData {
            return EasyCardTransitData(parcel)
        }

        override fun newArray(size: Int): Array<EasyCardTransitData?> {
            return arrayOfNulls(size)
        }
    }
}
