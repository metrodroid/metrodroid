/*
 * OvcUltralightTransitData.kt
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
package au.id.micolous.metrodroid.transit.venezia

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

private const val NAME = "Venezia Ultralight"

@Parcelize
data class VeneziaUltralightSubscription(override val parsed: En1545Parsed,
                                         override val validTo: Timestamp?,
                                         private val mOtp: Int) : En1545Subscription() {
    constructor(head: ImmutableByteArray, validTo: Timestamp?, mOtp: Int) : this(
            En1545Parser.parse(head,
                    En1545Container(
                            En1545FixedHex(CONTRACT_UNKNOWN_A, 32),
                            En1545FixedInteger(TRIP_FORMAT, 4),
                            En1545FixedInteger(CONTRACT_TARIFF, 16),
                            En1545FixedHex(CONTRACT_UNKNOWN_B, 44),
                            En1545FixedInteger(CONTRACT_AUTHENTICATOR, 32)
                    )
            ),
            validTo,
            mOtp
    )

    override val lookup: En1545Lookup
        get() = VeneziaLookup

    override val subscriptionState: SubscriptionState
        get() = if (mOtp == 0) SubscriptionState.INACTIVE else SubscriptionState.STARTED

    companion object {
        const val TRIP_FORMAT = "TripFormat"
    }
}

@Parcelize
private data class VeneziaUltralightTransaction(override val parsed: En1545Parsed) : VeneziaTransaction() {
    companion object {
        fun parse(raw: ImmutableByteArray, tripFormat: Int): VeneziaUltralightTransaction? {
            if (raw.sliceOffLen(1, 11).isAllZero())
                return null
            val fields = when (tripFormat) {
                1 -> En1545Container(
                        En1545FixedInteger("A", 11),
                        En1545FixedInteger.timePacked11Local(EVENT),
                        En1545FixedInteger(Y_VALUE, 14),
                        En1545FixedInteger("B", 2),
                        En1545FixedInteger.datePacked(EVENT),
                        En1545FixedInteger.timePacked11Local(EVENT_FIRST_STAMP),
                        En1545FixedInteger("Z", 16),
                        En1545FixedInteger("C", 17),
                        En1545FixedInteger(EVENT_AUTHENTICATOR, 32)
                )
                else -> En1545Container( // so far only 2
                        En1545FixedInteger("A", 8),
                        En1545FixedInteger.timePacked11Local(EVENT),
                        En1545FixedInteger(Y_VALUE, 14),
                        En1545FixedInteger("B", 2),
                        En1545FixedInteger.datePacked(EVENT),
                        En1545FixedInteger.timePacked11Local(EVENT_FIRST_STAMP),
                        En1545FixedInteger("D", 2),
                        En1545FixedInteger.datePacked(En1545Subscription.CONTRACT_END),
                        En1545FixedInteger.timePacked11Local(En1545Subscription.CONTRACT_END),
                        En1545FixedInteger(TRANSPORT_TYPE, 4),
                        En1545FixedInteger("F", 5),
                        En1545FixedInteger(EVENT_AUTHENTICATOR, 32)
                )
            }
            return VeneziaUltralightTransaction(En1545Parser.parse(raw, fields))
        }
    }

    val expiryTimestamp: Timestamp?
        get() = parsed.getTimeStamp(En1545Subscription.CONTRACT_END, VeneziaLookup.TZ)
}

@Parcelize
data class VeneziaUltralightTransitData(private val head: ImmutableByteArray,
                                        private val mSerial: Long,
                                        internal val mSub: VeneziaUltralightSubscription,
                                        override val trips: List<TransactionTripAbstract>) : TransitData() {
    override val serialNumber: String? get() = mSerial.toString()

    override val cardName get() = NAME

    override val subscriptions: List<Subscription>?
        get() = listOfNotNull(mSub)
}

private fun getSerial(card: UltralightCard) =
        (card.getPage(0).data.sliceOffLen(0, 3)
                + card.getPage(1).data).byteArrayToLongReversed()

private fun parse(card: UltralightCard): VeneziaUltralightTransitData {
    val head = card.readPages(4, 4)
    val blocks = listOf(
            card.readPages(8, 4),
            card.readPages(12, 4)
    )

    val tripFormat = head.getBitsFromBuffer(32, 4)

    val transactions = blocks.mapNotNull { VeneziaUltralightTransaction.parse(it, tripFormat) }

    val lastTransaction = transactions.maxWithOrNull (nullsFirst(compareBy { it.timestamp as? TimestampFull }))

    val sub = VeneziaUltralightSubscription(
            head,
            lastTransaction?.expiryTimestamp,
            mOtp = card.getPage(3).data.byteArrayToInt(2, 2)
    )

    return VeneziaUltralightTransitData(
            head = head,
            trips = TransactionTrip.merge(transactions),
            mSub = sub,
            mSerial = getSerial(card)
    )
}

class VeneziaUltralightTransitFactory : UltralightCardTransitFactory {
    override fun check(card: UltralightCard) =
            (card.getPage(0).data.byteArrayToInt(0, 1) == 0x05 && card.getPage(1).data.byteArrayToInt(2, 2) == 0x64e9)

    override fun parseTransitData(card: UltralightCard) = parse(card)

    override fun parseTransitIdentity(card: UltralightCard) = TransitIdentity(NAME, getSerial(card).toString())
}
