/*
 * MobibTransitData.kt
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

package au.id.micolous.metrodroid.transit.mobib

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.sum

/*
 * Reference:
 * - https://github.com/zoobab/mobib-extractor
 */
@Parcelize
class MobibTransitData(
    private val extHolderParsed: En1545Parsed?,
    private val purchase: Int,
    private val totalTrips: Int,
    private val ticketEnv: En1545Parsed,
    override val trips: List<TransactionTripAbstract>?,
    override val subscriptions: List<En1545Subscription>?,
    override val balances: List<TransitBalance>?,
    override val serialNumber: String?
) : En1545TransitData(ticketEnv) {
    val networkId
        get() = mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID)

    override val info: List<ListItem>?
        get() {
            val li = mutableListOf<ListItem>()
            En1545FixedInteger.parseDate(purchase, TZ)?.let {
                    li.add(ListItem(R.string.purchase_date, TimestampFormatter.longDateFormat(it)))
            }
            li.add(ListItem(R.string.transaction_counter, totalTrips.toString()))
            if (extHolderParsed != null) {
                val gender = extHolderParsed.getIntOrZero(EXT_HOLDER_GENDER)
                if (gender == 0) {
                    li.add(ListItem(R.string.card_type, R.string.card_type_anonymous))
                } else {
                    li.add(ListItem(R.string.card_type, R.string.card_type_personal))
                }
                if (gender != 0 && !Preferences.hideCardNumbers &&
                        !Preferences.obfuscateTripDates) {
                    li.add(ListItem(R.string.card_holders_name,
                            extHolderParsed.getString(EXT_HOLDER_NAME)))
                    when (gender) {
                        1 -> li.add(ListItem(R.string.gender,
                                R.string.gender_male))
                        2 -> li.add(ListItem(R.string.gender,
                                R.string.gender_female))
                        else -> li.add(ListItem(R.string.gender,
                                gender.toString(16)))
                    }
                }
            }
            li.addAll(super.info.orEmpty())
            return li
        }

    override val cardName: String
        get() = NAME

    override val lookup get() = MobibLookup

    companion object {
        // 56 = Belgium
        private const val MOBIB_NETWORK_ID = 0x56001
        const val NAME = "Mobib"
        private const val EXT_HOLDER_NAME = "ExtHolderName"

        private val CARD_INFO = CardInfo(
                name = NAME,
                cardType = CardType.ISO7816,
                imageId = R.drawable.mobib_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.BELGIUM,
                locationId = R.string.location_brussels)

        val TZ = MetroTimeZone.BRUSSELS

        private fun ticketEnvFields(version: Int) = when {
                version <= 2 -> En1545Container(
                        En1545FixedInteger(ENV_VERSION_NUMBER, 6),
                        En1545FixedInteger(ENV_UNKNOWN_A, 7),
                        En1545FixedInteger(ENV_NETWORK_ID, 24),
                        En1545FixedInteger(ENV_UNKNOWN_B, 9),
                        En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
                        En1545FixedInteger(ENV_UNKNOWN_C, 6),
                        En1545FixedInteger.dateBCD(HOLDER_BIRTH_DATE),
                        En1545FixedHex(ENV_CARD_SERIAL, 76),
                        En1545FixedInteger(ENV_UNKNOWN_D, 5),
                        En1545FixedInteger(HOLDER_INT_POSTAL_CODE, 14),
                        En1545FixedHex(ENV_UNKNOWN_E, 34)
                )
                else -> En1545Container(
                        En1545FixedInteger(ENV_VERSION_NUMBER, 6),
                        En1545FixedInteger(ENV_UNKNOWN_A, 7),
                        En1545FixedInteger(ENV_NETWORK_ID, 24),
                        En1545FixedInteger(ENV_UNKNOWN_B, 5),
                        En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
                        En1545FixedInteger(ENV_UNKNOWN_C, 10),
                        En1545FixedInteger.dateBCD(HOLDER_BIRTH_DATE),
                        En1545FixedHex(ENV_CARD_SERIAL, 76),
                        En1545FixedInteger(ENV_UNKNOWN_D, 5),
                        En1545FixedInteger(HOLDER_INT_POSTAL_CODE, 14),
                        En1545FixedHex(ENV_UNKNOWN_E, 34)
                )
        }
        private const val EXT_HOLDER_GENDER = "ExtHolderGender"
        private const val EXT_HOLDER_DATE_OF_BIRTH = "ExtHolderDateOfBirth"
        private const val EXT_HOLDER_CARD_SERIAL = "ExtHolderCardSerial"
        private const val EXT_HOLDER_UNKNOWN_A = "ExtHolderUnknownA"
        private const val EXT_HOLDER_UNKNOWN_B = "ExtHolderUnknownB"
        private const val EXT_HOLDER_UNKNOWN_C = "ExtHolderUnknownC"
        private const val EXT_HOLDER_UNKNOWN_D = "ExtHolderUnknownD"

        private val extHolderFields = En1545Container(
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_A, 18),
                En1545FixedHex(EXT_HOLDER_CARD_SERIAL, 76),
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_B, 16),
                En1545FixedHex(EXT_HOLDER_UNKNOWN_C, 58),
                En1545FixedInteger(EXT_HOLDER_DATE_OF_BIRTH, 32),
                En1545FixedInteger(EXT_HOLDER_GENDER, 2),
                En1545FixedInteger(EXT_HOLDER_UNKNOWN_D, 3),
                En1545FixedString(EXT_HOLDER_NAME, 259)
        )

        private fun getSerial(card: CalypsoApplication): String? {
            val holder = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)?.getRecord(1) ?: return null
            return NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(18 + 80, 24)), 6) + " / " +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(42 + 80, 24)), 6) +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(66 + 80, 16)), 4) +
                    NumberUtils.zeroPad(NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(82 + 80, 8)), 2) + " / " +
                    NumberUtils.convertBCDtoInteger(holder.getBitsFromBuffer(90 + 80, 4)).toString()
        }

        private fun parse(card: CalypsoApplication): MobibTransitData {
            val rawTicketEnv = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                    ?.recordList?.sum()
            val ticketEnv = if (rawTicketEnv == null) En1545Parsed() else
                        En1545Parser.parse(rawTicketEnv, ticketEnvFields(rawTicketEnv.getBitsFromBuffer(0, 6)))
            val contracts = card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1)?.recordList?.subList(
                    0, 7
            ).orEmpty()
            val subscriptions = mutableListOf<En1545Subscription>()
            val balances = mutableListOf<TransitBalance>()

            for ((idx, record) in contracts.withIndex()) {
                val sub = MobibSubscription.parse(record, Calypso1545TransitData.getCounter(card, idx + 1)) ?: continue

                val bal = sub.balance
                if (bal != null)
                    balances.add(bal)
                else
                    subscriptions.add(sub)
            }

            val ticketLog = card.getFile(CalypsoApplication.File.TICKETING_LOG,
                    trySfi = false) ?: card.getSfiFile(0x17)
            val transactions = ticketLog
                    ?.recordList.orEmpty()
                    .mapNotNull { MobibTransaction.parse(it) }

            val trips = TransactionTrip.merge(transactions)

            val holderFile = card.getFile(CalypsoApplication.File.HOLDER_EXTENDED)
            val holder = holderFile?.let { (it.getRecord(1) ?: ImmutableByteArray.empty()) +
                    (it.getRecord(2) ?: ImmutableByteArray.empty()) }
            val extHolderParsed = holder?.let { En1545Parser.parse(it, extHolderFields) }
            val purchase = card.getFile(CalypsoApplication.File.EP_LOAD_LOG, trySfi = false)
                    ?.getRecord(1)?.getBitsFromBuffer(2, 14) ?: 0
            val totalTrips = transactions.map { it.transactionNumber }.maxOrNull() ?: 0
            return MobibTransitData(balances = if (balances.isNotEmpty()) balances else null,
                    subscriptions = subscriptions,
                    trips = trips,
                    ticketEnv = ticketEnv, serialNumber = getSerial(card),
                    totalTrips = totalTrips,
                    purchase = purchase, extHolderParsed = extHolderParsed)
        }

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: CalypsoApplication) = TransitIdentity(NAME, getSerial(card))

            override fun check(tenv: ImmutableByteArray) = try {
                    MOBIB_NETWORK_ID == tenv.getBitsFromBuffer(13, 24)
                } catch (e: Exception) {
                    false
                }

            override fun getCardInfo(tenv: ImmutableByteArray) = CARD_INFO

            override fun parseTransitData(card: CalypsoApplication) = parse(card)
        }
    }
}
