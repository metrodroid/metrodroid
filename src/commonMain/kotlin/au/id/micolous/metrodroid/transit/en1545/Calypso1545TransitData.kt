/*
 * Calypso1545TransitData.kt
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

import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransactionTripAbstract
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

typealias SubCreator = (data: ImmutableByteArray, counter: Int?,
                        contractList: En1545Parsed?,
                        listNum: Int?) -> En1545Subscription?

typealias TripCreator = (data: ImmutableByteArray) -> En1545Transaction?

@Parcelize
data class Calypso1545TransitDataCapsule(
        val ticketEnv: En1545Parsed,
        val trips: List<TransactionTripAbstract>?,
        val subscriptions: List<En1545Subscription>?,
        val balances: List<TransitBalance>?,
        val serial: String?,
        val contractList: En1545Parsed?
): Parcelable

abstract class Calypso1545TransitData constructor (
        ticketEnv: En1545Parsed,
        override val trips: List<TransactionTripAbstract>?,
        override val subscriptions: List<En1545Subscription>?,
        override val balances: List<TransitBalance>?,
        override val serialNumber: String?,
        private val contractList: En1545Parsed?)
    : En1545TransitData(ticketEnv) {

    constructor(capsule: Calypso1545TransitDataCapsule): this(
            ticketEnv = capsule.ticketEnv,
            trips = capsule.trips,
            subscriptions = capsule.subscriptions,
            balances = capsule.balances,
            serialNumber = capsule.serial,
            contractList = capsule.contractList
    )

    val networkId
        get() = mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID)

    override fun getRawFields(level: RawLevel): List<ListItem>? {
        if (contractList == null)
            return super.getRawFields(level)
        return super.getRawFields(level).orEmpty() + listOf(HeaderListItem("Contract List")) +
                contractList.getInfo(setOf())
    }

    companion object {
        private fun getContracts(card: CalypsoApplication) =
                (listOf(CalypsoApplication.File.TICKETING_CONTRACTS_1,
                    CalypsoApplication.File.TICKETING_CONTRACTS_2)).mapNotNull {
                        card.getFile(it)?.recordList
            }.flatten()

        private fun insertSub(bals : MutableList<TransitBalance>,
                              subs: MutableList<En1545Subscription>, sub: En1545Subscription?) {
            if (sub == null)
                return
            val bal = sub.balance
            if (bal != null)
                bals.add(bal)
            else
                subs.add(sub)
        }

        private fun insertSub(card: CalypsoApplication,
                              bals : MutableList<TransitBalance>,
                              subs: MutableList<En1545Subscription>,
                              createSubscription: SubCreator,
                              data: ImmutableByteArray,
                              contractList: En1545Parsed?,
                              listNum: Int?,
                              recordNum: Int) {
            insertSub(bals, subs, createSubscription(data, getCounter(card, recordNum),
                    contractList, listNum))
        }

        fun parseTrips(card: CalypsoApplication,
                       createTrip: TripCreator,
                       createSpecialEvent: TripCreator?): List<TransactionTripAbstract> {
            val transactions = card.getFile(CalypsoApplication.File.TICKETING_LOG)
                    ?.recordList.orEmpty().filter { !it.isAllZero() }
                    .mapNotNull { createTrip(it) }

            val specialEvents =
                    if (createSpecialEvent != null)
                        card.getFile(CalypsoApplication.File.TICKETING_SPECIAL_EVENTS)
                                ?.recordList.orEmpty()
                                .filter { !it.isAllZero() }
                                .mapNotNull { createSpecialEvent(it) }
                    else
                        emptyList()
            return TransactionTrip.merge(transactions + specialEvents)
        }

        private fun parseContracts(card: CalypsoApplication,
                                   contractListFields: En1545Field?,
                                   createSubscription: SubCreator,
                                   contracts: List<ImmutableByteArray>): Triple<List<En1545Subscription>,
                List<TransitBalance>, En1545Parsed?> {
            val subscriptions = mutableListOf<En1545Subscription>()
            val balances = mutableListOf<TransitBalance>()

            val parsed = mutableSetOf<Int>()
            val contractList: En1545Parsed?

            if (contractListFields != null) {
                contractList = En1545Parser.parse(card.getFile(CalypsoApplication.File.TICKETING_CONTRACT_LIST)
                        ?.getRecord(1) ?: ImmutableByteArray.empty(), contractListFields)
                for (i in 0..15) {
                    val ptr = contractList.getInt(En1545TransitData.CONTRACTS_POINTER, i)
                            ?: continue
                    if (ptr == 0)
                        continue
                    parsed.add(ptr)
                    if (ptr > contracts.size)
                        continue
                    val recordData = contracts[ptr - 1]
                    insertSub(card, balances, subscriptions, createSubscription,
                            recordData, contractList, i, ptr)
                }
            } else
                contractList = null

            for ((idx, record) in contracts.withIndex()) {
                if (record.isAllZero())
                    continue
                if (parsed.contains(idx))
                    continue
                insertSub(card, balances, subscriptions, createSubscription,
                        record, null, null, idx+1)
            }

            return Triple(subscriptions, balances, contractList)
        }

        fun parseTicketEnv(card: CalypsoApplication,
                           ticketEnvHolderFields: En1545Container): En1545Parsed {
            val ticketEnv = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                    ?.recordList?.fold(ImmutableByteArray.empty()) { acc, data -> acc + data }
                    ?: ImmutableByteArray.empty()
            return En1545Parser.parse(ticketEnv, ticketEnvHolderFields)
        }

        fun parseWithEnv(card: CalypsoApplication,
                         ticketEnvParsed: En1545Parsed,
                         contractListFields: En1545Field?,
                         serial: String?,
                         createSubscription: SubCreator,
                         createTrip: TripCreator,
                         createSpecialEvent: TripCreator? = null,
                         contracts: List<ImmutableByteArray> = getContracts(card)):
                Calypso1545TransitDataCapsule {
            val (subscriptions, balances, contractList) = parseContracts(card,
                    contractListFields,
                    createSubscription, contracts = contracts)

            return Calypso1545TransitDataCapsule(balances = if (balances.isNotEmpty()) balances else null,
                    subscriptions = subscriptions,
                    trips = parseTrips(card, createTrip, createSpecialEvent = createSpecialEvent),
                    ticketEnv = ticketEnvParsed, serial = serial,
                    contractList = contractList)
        }

        fun parse(card: CalypsoApplication,
                  ticketEnvHolderFields: En1545Container,
                  contractListFields: En1545Field?,
                  serial: String?,
                  createSubscription: SubCreator,
                  createTrip: TripCreator,
                  createSpecialEvent: TripCreator? = null,
                  contracts: List<ImmutableByteArray> = getContracts(card)): Calypso1545TransitDataCapsule =
                parseWithEnv(card, parseTicketEnv(card, ticketEnvHolderFields),
                        contractListFields, serial, createSubscription, createTrip,
                        createSpecialEvent = createSpecialEvent, contracts = contracts)

        private val COUNTERS = arrayOf(CalypsoApplication.File.TICKETING_COUNTERS_1, CalypsoApplication.File.TICKETING_COUNTERS_2, CalypsoApplication.File.TICKETING_COUNTERS_3, CalypsoApplication.File.TICKETING_COUNTERS_4)

        private fun getCounter(card: CalypsoApplication, recordNum: Int, trySfi: Boolean): Int? {
            val commonCtr = card.getFile(CalypsoApplication.File.TICKETING_COUNTERS_9, trySfi)
            commonCtr?.getRecord(1)?.byteArrayToInt(3 * (recordNum - 1), 3)?.
                    let { return it }
            val ownCtr = card.getFile(COUNTERS[recordNum - 1], trySfi)
            return ownCtr?.getRecord(1)?.byteArrayToInt(0, 3)
        }

        fun getCounter(card: CalypsoApplication, recordNum: Int): Int? {
            if (recordNum > 4)
                return null
            val cnt = getCounter(card, recordNum, false)
            return cnt ?: getCounter(card, recordNum, true)
        }
    }
}
