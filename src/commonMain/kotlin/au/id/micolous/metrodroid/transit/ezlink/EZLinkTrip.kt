/*
 * EZLinkTrip.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip

@Parcelize
class EZLinkTrip (private val mTransaction: CEPASTransaction,
                  private val mCardName: String): Trip() {

    override val startTimestamp: Timestamp?
        get() = mTransaction.timestamp

    override val routeName: String?
        get() = getRouteName(mTransaction.type, mTransaction.userData)

    override val humanReadableRouteID: String?
        get() = mTransaction.userData

    override val fare: TransitCurrency?
        get() = if (mTransaction.type === CEPASTransaction.TransactionType.CREATION) null else TransitCurrency.SGD(-mTransaction.amount)

    override val startStation: Station?
        get() {
            if (mTransaction.type === CEPASTransaction.TransactionType.BUS && (
                            mTransaction.userData.startsWith("SVC")
                                    || mTransaction.userData.startsWith("BUS")))
                return null
            if (mTransaction.type === CEPASTransaction.TransactionType.CREATION)
                return Station.nameOnly(mTransaction.userData)
            if (mTransaction.userData[3] == '-' || mTransaction.userData[3] == ' ') {
                val startStationAbbr = mTransaction.userData.substring(0, 3)
                return EZLinkTransitData.getStation(startStationAbbr)
            }
            return Station.nameOnly(mTransaction.userData)
        }

    override val endStation: Station?
        get() {
            if (mTransaction.type === CEPASTransaction.TransactionType.CREATION)
                return null
            if (mTransaction.userData[3] == '-' || mTransaction.userData[3] == ' ') {
                val endStationAbbr = mTransaction.userData.substring(4, 7)
                return EZLinkTransitData.getStation(endStationAbbr)
            }
            return null
        }


    override val mode: Trip.Mode
        get() = getMode(mTransaction.type)

    override fun getAgencyName(isShort: Boolean): String? {
        return getAgencyName(mTransaction.type, mCardName, isShort)
    }

    companion object {
        fun getMode(type: CEPASTransaction.TransactionType) =
                when (type) {
                    CEPASTransaction.TransactionType.BUS, CEPASTransaction.TransactionType.BUS_REFUND -> Trip.Mode.BUS
                    CEPASTransaction.TransactionType.MRT -> Trip.Mode.METRO
                    CEPASTransaction.TransactionType.TOP_UP -> Trip.Mode.TICKET_MACHINE
                    CEPASTransaction.TransactionType.RETAIL, CEPASTransaction.TransactionType.SERVICE -> Trip.Mode.POS
                    else -> Trip.Mode.OTHER
                }

        fun getAgencyName(type: CEPASTransaction.TransactionType, cardName: String, isShort: Boolean): String =
                when (type) {
                    CEPASTransaction.TransactionType.BUS, CEPASTransaction.TransactionType.BUS_REFUND -> "BUS"
                    CEPASTransaction.TransactionType.CREATION,
                    CEPASTransaction.TransactionType.TOP_UP,
                    CEPASTransaction.TransactionType.SERVICE -> if (isShort && cardName == "EZ-Link") "EZ" else cardName
                    CEPASTransaction.TransactionType.RETAIL -> "POS"
                    else -> "SMRT"
                }

        fun getRouteName(type: CEPASTransaction.TransactionType, userData: String) =
                when (type) {
                    CEPASTransaction.TransactionType.BUS -> {
                        if (userData.startsWith("SVC") || userData.startsWith("BUS"))
                            Localizer.localizeString(R.string.ez_bus_number, userData.substring(3, 7).replace(" ", ""))
                        else
                            Localizer.localizeString(R.string.unknown_format, userData)
                    }
                    // FIXME: These aren't actually routes...
                    CEPASTransaction.TransactionType.BUS_REFUND -> Localizer.localizeString(R.string.ez_bus_refund)
                    CEPASTransaction.TransactionType.MRT -> Localizer.localizeString(R.string.ez_mrt)
                    CEPASTransaction.TransactionType.TOP_UP -> Localizer.localizeString(R.string.ez_topup)
                    CEPASTransaction.TransactionType.CREATION -> Localizer.localizeString(R.string.ez_first_use)
                    CEPASTransaction.TransactionType.RETAIL -> Localizer.localizeString(R.string.ez_retail_purchase)
                    CEPASTransaction.TransactionType.SERVICE -> Localizer.localizeString(R.string.ez_service_charge)
                    else -> Localizer.localizeString(R.string.unknown_format, type.toString())
                }
    }
}
