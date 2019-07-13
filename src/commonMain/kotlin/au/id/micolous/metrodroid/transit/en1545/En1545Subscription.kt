/*
 * En1545Subscription.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.en1545

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

abstract class En1545Subscription : Subscription() {
    protected abstract val parsed: En1545Parsed
    protected abstract val lookup: En1545Lookup

    override val zones: IntArray?
        get() {
            val zonecode = parsed.getInt(CONTRACT_ZONES) ?: return null

            val zones = mutableListOf<Int>()
            var zone = 0
            while (zonecode shr zone > 0) {
                if (zonecode and (1 shl zone) != 0) {
                    zones.add(zone + 1)
                }
                zone++
            }

            return zones.toIntArray()
        }

    override val purchaseTimestamp: Timestamp?
        get() = parsed.getTimeStamp(CONTRACT_SALE, lookup.timeZone)

    override val paymentMethod: Subscription.PaymentMethod
        get() {
            if (cost == null) {
                return super.paymentMethod
            }

            return when (parsed.getIntOrZero(CONTRACT_PAY_METHOD)) {
                0x90 -> Subscription.PaymentMethod.CASH
                0xb3 -> Subscription.PaymentMethod.CREDIT_CARD

                0 -> Subscription.PaymentMethod.UNKNOWN
                else -> Subscription.PaymentMethod.UNKNOWN
            }
        }

    override val lastUseTimestamp: Timestamp?
        get() = parsed.getTimeStamp(CONTRACT_LAST_USE, lookup.timeZone)

    override val subscriptionState: Subscription.SubscriptionState
        get() {
            val status = parsed.getInt(CONTRACT_STATUS) ?: return super.subscriptionState

            when (status) {
                0 -> return Subscription.SubscriptionState.UNUSED
                1 -> return Subscription.SubscriptionState.STARTED
                0xFF -> return Subscription.SubscriptionState.EXPIRED
            }
            Log.d(TAG, "Unknown subscription state: " + NumberUtils.intToHex(status))
            return Subscription.SubscriptionState.UNKNOWN
        }

    override val saleAgencyName: FormattedString?
        get() {
            val agency = parsed.getInt(CONTRACT_SALE_AGENT) ?: return null

            return lookup.getAgencyName(agency, false)
        }

    override val passengerCount: Int
        get() {
            return parsed.getInt(CONTRACT_PASSENGER_TOTAL) ?: return super.passengerCount
        }

    override val validFrom: Timestamp?
        get() = parsed.getTimeStamp(CONTRACT_START, lookup.timeZone)

    override val validTo: Timestamp?
        get() = parsed.getTimeStamp(CONTRACT_END, lookup.timeZone)

    protected val contractTariff: Int?
        get() = parsed.getInt(CONTRACT_TARIFF)

    protected val contractProvider: Int?
        get() = parsed.getInt(CONTRACT_PROVIDER)

    override val subscriptionName: String?
        get() = lookup.getSubscriptionName(parsed.getInt(CONTRACT_PROVIDER),
                contractTariff)

    override val machineId: Int?
        get() = parsed.getInt(CONTRACT_SALE_DEVICE)?.let { if (it == 0) null else it }

    override val id: Int?
        get() = parsed.getInt(CONTRACT_SERIAL_NUMBER)

    open val balance: TransitBalance?
        get() = null

    override val info: List<ListItem>?
        get() {
            val li = mutableListOf<ListItem>()
            val clas = parsed.getInt(CONTRACT_PASSENGER_CLASS)
            if (clas != null)
                li.add(ListItem(R.string.passenger_class, clas.toString()))
            val receipt = parsed.getInt(CONTRACT_RECEIPT_DELIVERED)
            if (receipt != null && receipt != 0)
                li.add(ListItem(Localizer.localizeString(R.string.with_receipt)))
            if (receipt != null && receipt == 0)
                li.add(ListItem(Localizer.localizeString(R.string.without_receipt)))
            if (parsed.contains(CONTRACT_ORIGIN_1) || parsed.contains(CONTRACT_DESTINATION_1)) {
                if (parsed.contains(CONTRACT_VIA_1))
                li.add(ListItem(Localizer.localizeFormatted(R.string.valid_origin_destination_via,
                        getStationName(CONTRACT_ORIGIN_1),
                        getStationName(CONTRACT_DESTINATION_1),
                        getStationName(CONTRACT_VIA_1))))
                else
                    li.add(ListItem(Localizer.localizeFormatted(R.string.valid_origin_destination,
                            getStationName(CONTRACT_ORIGIN_1),
                            getStationName(CONTRACT_DESTINATION_1))))

            }
            if (parsed.contains(CONTRACT_ORIGIN_2) || parsed.contains(CONTRACT_DESTINATION_2)) {
                li.add(ListItem(Localizer.localizeFormatted(R.string.valid_origin_destination,
                        getStationName(CONTRACT_ORIGIN_2),
                        getStationName(CONTRACT_DESTINATION_2))))
            }
            return super.info.orEmpty() + li
        }

    private fun getStationName(prop: String): FormattedString? {
        return lookup.getStation(parsed.getInt(prop) ?: return null, contractProvider, null)?.stationName
    }

    override fun getRawFields(level: TransitData.RawLevel): List<ListItem>? =
            parsed.getInfo(
                    when (level) {
                        TransitData.RawLevel.UNKNOWN_ONLY -> setOf(
                                CONTRACT_AUTHENTICATOR, CONTRACT_TARIFF,
                                En1545FixedInteger.datePackedName(CONTRACT_SALE),
                                En1545FixedInteger.dateName(CONTRACT_SALE),
                                En1545FixedInteger.timePacked11LocalName(CONTRACT_SALE),
                                En1545FixedInteger.timeLocalName(CONTRACT_SALE),
                                CONTRACT_PROVIDER,
                                En1545FixedInteger.dateName(CONTRACT_START),
                                En1545FixedInteger.dateName(CONTRACT_END),
                                CONTRACT_STATUS,
                                En1545FixedInteger.dateName(CONTRACT_LAST_USE),
                                CONTRACT_ZONES,
                                CONTRACT_ORIGIN_1,
                                CONTRACT_DESTINATION_1,
                                CONTRACT_VIA_1,
                                CONTRACT_ORIGIN_2,
                                CONTRACT_DESTINATION_2,
                                CONTRACT_SERIAL_NUMBER,
                                CONTRACT_SALE_AGENT,
                                CONTRACT_SALE_DEVICE,
                                CONTRACT_PRICE_AMOUNT,
                                CONTRACT_PAY_METHOD
                        )
                        else -> setOf()
                    })

    override val cost: TransitCurrency?
        get() {
            val cost = parsed.getIntOrZero(CONTRACT_PRICE_AMOUNT)
            return if (cost == 0) {
                null
            } else lookup.parseCurrency(cost)
        }

    override fun getAgencyName(isShort: Boolean) =
        lookup.getAgencyName(contractProvider, false)

    companion object {
        private const val TAG = "En1545Subscription"
        const val CONTRACT_ZONES = "ContractZones"
        const val CONTRACT_SALE = "ContractSale"
        const val CONTRACT_PRICE_AMOUNT = "ContractPriceAmount"
        const val CONTRACT_PAY_METHOD = "ContractPayMethod"
        const val CONTRACT_LAST_USE = "ContractLastUse"
        const val CONTRACT_STATUS = "ContractStatus"
        const val CONTRACT_SALE_AGENT = "ContractSaleAgent"
        const val CONTRACT_PASSENGER_TOTAL = "ContractPassengerTotal"
        const val CONTRACT_START = "ContractStart"
        const val CONTRACT_END = "ContractEnd"
        const val CONTRACT_PROVIDER = "ContractProvider"
        const val CONTRACT_TARIFF = "ContractTariff"
        const val CONTRACT_SALE_DEVICE = "ContractSaleDevice"
        const val CONTRACT_SERIAL_NUMBER = "ContractSerialNumber"
        const val CONTRACT_UNKNOWN_A = "ContractUnknownA"
        const val CONTRACT_UNKNOWN_B = "ContractUnknownB"
        const val CONTRACT_UNKNOWN_C = "ContractUnknownC"
        const val CONTRACT_UNKNOWN_D = "ContractUnknownD"
        const val CONTRACT_UNKNOWN_E = "ContractUnknownE"
        const val CONTRACT_UNKNOWN_F = "ContractUnknownF"
        const val CONTRACT_NETWORK_ID = "ContractNetworkId"
        const val CONTRACT_PASSENGER_CLASS = "ContractPassengerClass"
        const val CONTRACT_AUTHENTICATOR = "ContractAuthenticator"
        const val CONTRACT_SOLD = "ContractSold"
        const val CONTRACT_DEBIT_SOLD = "ContractDebitSold"
        const val CONTRACT_JOURNEYS = "ContractJourneys"
        const val CONTRACT_RECEIPT_DELIVERED = "ContractReceiptDelivered"
        const val CONTRACT_ORIGIN_1 = "ContractOrigin1"
        const val CONTRACT_VIA_1 = "ContractVia1"
        const val CONTRACT_DESTINATION_1 = "ContractDestination1"
        const val CONTRACT_ORIGIN_2 = "ContractOrigin2"
        const val CONTRACT_DESTINATION_2 = "ContractDestination2"
        const val CONTRACT_VEHICULE_CLASS_ALLOWED = "ContractVehiculeClassAllowed"
        const val CONTRACT_DURATION = "ContractDuration"
        const val CONTRACT_INTERCHANGE = "ContractInterchange"
        const val LINKED_CONTRACT = "LinkedContract"
        const val CONTRACT_RESTRICT_CODE = "ContractRestrictCode"
    }
}
