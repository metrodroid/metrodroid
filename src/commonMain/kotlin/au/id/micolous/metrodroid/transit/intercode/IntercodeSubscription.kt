/*
 * IntercodeSubscription.kt
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
package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*

import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class IntercodeSubscription(override val parsed: En1545Parsed, private val ctr: Int?,
                                 private val networkId: Int) : En1545Subscription() {

    override val lookup: En1545Lookup
        get() = IntercodeTransitData.getLookup(networkId)

    override val remainingTripCount: Int?
        get() {
            if (parsed.getIntOrZero(CONTRACT_DEBIT_SOLD) != 0 && parsed.getIntOrZero(CONTRACT_SOLD) != 0) {
                return ctr!! / parsed.getIntOrZero(CONTRACT_DEBIT_SOLD)
            }
            return if (parsed.getIntOrZero(CONTRACT_JOURNEYS) != 0) {
                ctr
            } else null
        }

    override val totalTripCount: Int?
        get() {
            if (parsed.getIntOrZero(CONTRACT_DEBIT_SOLD) != 0 && parsed.getIntOrZero(CONTRACT_SOLD) != 0) {
                return parsed.getIntOrZero(CONTRACT_SOLD) / parsed.getIntOrZero(CONTRACT_DEBIT_SOLD)
            }
            return if (parsed.getIntOrZero(CONTRACT_JOURNEYS) != 0) {
                parsed.getIntOrZero(CONTRACT_JOURNEYS)
            } else null
        }

    override val info: List<ListItem>?
        get() = super.info.orEmpty() + parsed.getInfo(setOf(
                    CONTRACT_TARIFF,
                    CONTRACT_PRICE_AMOUNT,
                    CONTRACT_PAY_METHOD,
                    CONTRACT_SALE_DEVICE,
                    CONTRACT_SALE_AGENT,
                    En1545FixedInteger.dateName(CONTRACT_SALE),
                    En1545FixedInteger.dateName(CONTRACT_START),
                    En1545FixedInteger.dateName(CONTRACT_END),
                    CONTRACT_STATUS,
                    CONTRACT_PROVIDER,
                    CONTRACT_RECEIPT_DELIVERED,
                    CONTRACT_PASSENGER_CLASS,
                    CONTRACT_ZONES,
                    CONTRACT_AUTHENTICATOR,
                    CONTRACT_ORIGIN_1,
                    CONTRACT_DESTINATION_1,
                    CONTRACT_VIA_1,
                    CONTRACT_ORIGIN_2,
                    CONTRACT_DESTINATION_2,
                    CONTRACT_SERIAL_NUMBER
        ))

    companion object {
        fun parse(data: ImmutableByteArray, type: Int, networkId: Int, ctr: Int?):IntercodeSubscription {
            val parsed = En1545Parser.parse(data, getFields(type))
            val nid = parsed.getInt(CONTRACT_NETWORK_ID)
            return IntercodeSubscription(parsed = parsed, ctr = ctr, networkId = nid ?: networkId)
        }

        private val subFieldsTypeFF = En1545Bitmap(
                En1545FixedInteger(CONTRACT_NETWORK_ID, 24),
                En1545FixedInteger(CONTRACT_PROVIDER, 8),
                En1545FixedInteger(CONTRACT_TARIFF, 16),
                En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                En1545Bitmap(
                        En1545FixedInteger("ContractCustomerProfile", 6),
                        En1545FixedInteger("ContractCustomerNumber", 32)
                ),
                En1545Bitmap(
                        En1545FixedInteger(CONTRACT_PASSENGER_CLASS, 8),
                        En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 8)
                ),
                En1545FixedInteger(CONTRACT_VEHICULE_CLASS_ALLOWED, 6),
                En1545FixedInteger("ContractPaymentPointer", 32),
                En1545FixedInteger(CONTRACT_PAY_METHOD, 11),
                En1545FixedInteger("ContractServices", 16),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 16),
                En1545FixedInteger("ContractPriceUnit", 16),
                En1545Bitmap(
                        En1545FixedInteger.timeLocal("ContractRestrictStart"),
                        En1545FixedInteger.timeLocal("ContractRestrictEnd"),
                        En1545FixedInteger("ContractRestrictDay", 8),
                        En1545FixedInteger("ContractRestrictTimeCode", 8),
                        En1545FixedInteger(CONTRACT_RESTRICT_CODE, 8),
                        En1545FixedInteger("ContractRestrictProduct", 16),
                        En1545FixedInteger("ContractRestrictLocation", 16)
                ),
                En1545Bitmap(
                        En1545FixedInteger.date(CONTRACT_START),
                        En1545FixedInteger.timeLocal(CONTRACT_START),
                        En1545FixedInteger.date(CONTRACT_END),
                        En1545FixedInteger.timeLocal(CONTRACT_END),
                        En1545FixedInteger(CONTRACT_DURATION, 8),
                        En1545FixedInteger.date("ContractLimit"),
                        En1545FixedInteger(CONTRACT_ZONES, 8),
                        En1545FixedInteger(CONTRACT_JOURNEYS, 16),
                        En1545FixedInteger("ContractPeriodJourneys", 16)
                ),
                En1545Bitmap(
                        En1545FixedInteger(CONTRACT_ORIGIN_1, 16),
                        En1545FixedInteger(CONTRACT_DESTINATION_1, 16),
                        En1545FixedInteger("ContractRouteNumbers", 16),
                        En1545FixedInteger("ContractRouteVariants", 8),
                        En1545FixedInteger("ContractRun", 16),
                        En1545FixedInteger(CONTRACT_VIA_1, 16),
                        En1545FixedInteger("ContractDistance", 16),
                        En1545FixedInteger(CONTRACT_INTERCHANGE, 8)
                ),
                En1545Bitmap(
                        En1545FixedInteger.date(CONTRACT_SALE),
                        En1545FixedInteger.timeLocal(CONTRACT_SALE),
                        En1545FixedInteger(CONTRACT_SALE_AGENT, 8),
                        En1545FixedInteger(CONTRACT_SALE_DEVICE, 16)
                ),
                En1545FixedInteger(CONTRACT_STATUS, 8),
                En1545FixedInteger("ContractLoyaltyPoints", 16),
                En1545FixedInteger(CONTRACT_AUTHENTICATOR, 16),
                En1545FixedInteger("ContractExtra", 0)
        )

        fun commonFormat(extra: En1545Field): En1545Field {
            return En1545Bitmap(
                    En1545FixedInteger(CONTRACT_PROVIDER, 8),
                    En1545FixedInteger(CONTRACT_TARIFF, 16),
                    En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                    En1545FixedInteger(CONTRACT_PASSENGER_CLASS, 8),
                    En1545Bitmap(
                            En1545FixedInteger.date(CONTRACT_START),
                            En1545FixedInteger.date(CONTRACT_END)
                    ),
                    En1545FixedInteger(CONTRACT_STATUS, 8),
                    extra
            )
        }

        private val subFieldsTypeOther = commonFormat(En1545FixedInteger("ContractData", 0))
        private val SALE_CONTAINER = En1545Container(
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 16),
                En1545FixedInteger(CONTRACT_SALE_AGENT, 8)
        )
        private val PAY_CONTAINER = En1545Container(
                En1545FixedInteger(CONTRACT_PAY_METHOD, 11),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 16),
                En1545FixedInteger(CONTRACT_RECEIPT_DELIVERED, 1)
        )
        private val SOLD_CONTAINER = En1545Container(
                En1545FixedInteger(CONTRACT_SOLD, 8),
                En1545FixedInteger(CONTRACT_DEBIT_SOLD, 5)
        )
        private val PERIOD_CONTAINER = En1545Container(
                En1545FixedInteger("ContractEndPeriod", 14),
                En1545FixedInteger("ContractSoldPeriod", 6)
        )
        private val PASSENGER_COUNTER = En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6)

        private val ZONE_MASK = En1545FixedInteger(CONTRACT_ZONES, 16)
        private val OVD1_CONTAINER = En1545Container(
                En1545FixedInteger(CONTRACT_ORIGIN_1, 16),
                En1545FixedInteger(CONTRACT_VIA_1, 16),
                En1545FixedInteger(CONTRACT_DESTINATION_1, 16)
        )
        private val OD2_CONTAINER = En1545Container(
                En1545FixedInteger(CONTRACT_ORIGIN_2, 16),
                En1545FixedInteger(CONTRACT_DESTINATION_2, 16)
        )
        private val subFieldsType20 = commonFormat(
                En1545Bitmap(
                        OVD1_CONTAINER,
                        OD2_CONTAINER,
                        ZONE_MASK,
                        SALE_CONTAINER,
                        PAY_CONTAINER,
                        PASSENGER_COUNTER,
                        PERIOD_CONTAINER,
                        SOLD_CONTAINER,
                        En1545FixedInteger(CONTRACT_VEHICULE_CLASS_ALLOWED, 4),
                        En1545FixedInteger(LINKED_CONTRACT, 5)
                )
        )
        val subFieldsType46 = commonFormat(
                En1545Bitmap(
                        OVD1_CONTAINER,
                        OD2_CONTAINER,
                        ZONE_MASK,
                        SALE_CONTAINER,
                        PAY_CONTAINER,
                        PASSENGER_COUNTER,
                        PERIOD_CONTAINER,
                        SOLD_CONTAINER,
                        En1545FixedInteger(CONTRACT_VEHICULE_CLASS_ALLOWED, 4),
                        En1545FixedInteger(LINKED_CONTRACT, 5),
                        En1545FixedInteger.timeLocal(CONTRACT_START),
                        En1545FixedInteger.timeLocal(CONTRACT_END),
                        En1545FixedInteger.date("ContractDataEndInhibition"),
                        En1545FixedInteger.date("ContractDataValidityLimit"),
                        En1545FixedInteger("ContractDataGeoLine", 28),
                        En1545FixedInteger(CONTRACT_JOURNEYS, 16),
                        En1545FixedInteger("ContractDataSaleSecureDevice", 32)
                )
        )

        private fun getFields(type: Int): En1545Field {
            if (type == 0xff)
                return subFieldsTypeFF

            if (type == 0x20)
                return subFieldsType20

            return if (type == 0x46) subFieldsType46 else subFieldsTypeOther

        }
    }
}
