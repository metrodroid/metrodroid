/*
 * HafilatMetrocardTransitData.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.hafilat

import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.en1545.En1545Parser
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData
import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class HafilatTransitData (
        override val trips: List<TransactionTripAbstract>,
        override val subscriptions: List<HafilatSubscription>?,
        private val purse: HafilatSubscription?,
        private val serial: Long,
        private val parsed : En1545Parsed
): En1545TransitData(parsed) {
    override val lookup: HafilatLookup
        get() = HafilatLookup

    override val serialNumber: String?
        get() = formatSerial(serial)

    override val cardName: String
        get() = Localizer.localizeString(NAME)

    override val info: List<ListItem>?
        get() {
            val items = mutableListOf<ListItem>()
            if (purse != null) {
                items.add(ListItem(R.string.ticket_type, purse.subscriptionName))

                if (purse.machineId != null) {
                    items.add(ListItem(R.string.machine_id,
                            purse.machineId.toString()))
                }

                val purchaseTS = purse.purchaseTimestamp
                if (purchaseTS != null) {
                    items.add(ListItem(R.string.issue_date, purchaseTS.format()))
                }

                val purseId = purse.id
                if (purseId != null)
                    items.add(ListItem(R.string.purse_serial_number, purseId.toString(16)))
            }
            return super.info.orEmpty() + items
        }

    companion object {
        private fun parse(card: DesfireCard): HafilatTransitData {
            val app = card.getApplication(APP_ID)

            // This is basically mapped from Intercode
            // 0 = TICKETING_ENVIRONMENT
            val parsed = En1545Parser.parse(app!!.getFile(0)!!.data,
                    IntercodeTransitData.TICKET_ENV_FIELDS)

            // 1 is 0-record file on all cards we've seen so far

            // 2 = TICKETING_CONTRACT_LIST, not really useful to use

            val transactionList = mutableListOf<Transaction>()

            // 3-6: TICKETING_LOG
            // 8 is "ABU DHABI" and some indexing
            // 9, 0xd: zero-filled
            // a:???
            for (fileId in intArrayOf(3, 4, 5, 6)) {
                val data = app.getFile(fileId)?.data ?: continue
                if (data.getBitsFromBuffer(0, 14) == 0)
                    continue
                transactionList.add(HafilatTransaction(data))
            }

            // c-f: locked counters
            val subs = mutableListOf<HafilatSubscription>()
            var purse: HafilatSubscription? = null
            // 10-13: contracts
            for (fileId in intArrayOf(0x10, 0x11, 0x12, 0x13)) {
                val data = app.getFile(fileId)?.data ?: continue
                if (data.getBitsFromBuffer(0, 7) == 0)
                    continue
                val sub = HafilatSubscription(data)
                if (sub.isPurse)
                    purse = sub
                else
                    subs.add(sub)
            }

            // 14-17: zero-filled
            // 18: ??
            // 19: zero-filled
            // 1b-1c: locked
            // 1d: empty record file
            // 1e: const

            return HafilatTransitData(purse = purse,
                    serial = getSerial(card.tagId),
                    subscriptions = if (subs.isNotEmpty()) subs else null,
                    trips = TransactionTrip.merge(transactionList),
                    parsed = parsed)
        }

        private const val APP_ID = 0x107f2
        private val NAME = R.string.card_name_hafilat

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_abu_dhabi,
                imageId = R.drawable.hafilat,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.MifareDesfire,
                resourceExtraNote = R.string.card_note_adelaide,
                preview = true)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(getSerial(card.tagId)))

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)
        }

        private fun formatSerial(serial: Long) = "01-" + NumberUtils.zeroPad(serial, 15)

        private fun getSerial(tagId: ImmutableByteArray) =
                tagId.byteArrayToLongReversed(1, 6)
    }
}
