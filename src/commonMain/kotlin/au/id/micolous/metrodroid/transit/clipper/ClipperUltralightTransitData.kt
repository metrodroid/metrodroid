/*
 * ClipperUltralightTransitData.kt
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
package au.id.micolous.metrodroid.transit.clipper

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
class ClipperUltralightTransitData private constructor(private val mSerial: Long,
                                                       private val mBaseDate: Int,
                                                       override val trips: List<ClipperUltralightTrip>?,
                                                       private val mType: Int,
                                                       private val mSub: ClipperUltralightSubscription) : TransitData() {

    override val serialNumber: String?
        get() = mSerial.toString()

    override val cardName: String
        get() = NAME

    override val subscriptions: List<Subscription>?
        get() = listOf<Subscription>(mSub)

    override val info: List<ListItem>?
        get() = listOf(
                when (mType) {
                    0x04 -> ListItem(R.string.ticket_type, R.string.clipper_ticket_type_adult)
                    0x44 -> ListItem(R.string.ticket_type, R.string.clipper_ticket_type_senior)
                    0x84 -> ListItem(R.string.ticket_type, R.string.clipper_ticket_type_rtc)
                    0xc4 -> ListItem(R.string.ticket_type, R.string.clipper_ticket_type_youth)
                    else -> ListItem(R.string.ticket_type, mType.toString(16))
                }
        )

    override fun getRawFields(level: RawLevel): List<ListItem> = listOf(
        ListItem(R.string.clipper_base_date,
            ClipperTransitData.clipperTimestampToCalendar(mBaseDate * 1440L * 60L)
                ?.format())
    )

    companion object {
        private const val NAME = "Clipper Ultralight"

        private fun parse(card: UltralightCard): ClipperUltralightTransitData {
            val page0 = card.getPage(4).data
            val page1 = card.getPage(5).data
            val mBaseDate = page1.byteArrayToInt(2, 2)
            val rawTrips = intArrayOf(6, 11).map { offset -> card.readPages(offset, 5) }.
                    filter { !it.isAllZero() }. map { ClipperUltralightTrip(it, mBaseDate) }
            var trLast: ClipperUltralightTrip? = null
            for (tr in rawTrips) {
                if (trLast == null || tr.isSeqGreater(trLast))
                    trLast = tr
            }
            return ClipperUltralightTransitData(
                    mSub = ClipperUltralightSubscription(page1.byteArrayToInt(0, 2),
                            trLast?.tripsRemaining ?: -1,
                            trLast?.transferExpiry ?: 0, mBaseDate),
                    trips = rawTrips.filter {  ! it.isHidden },
                    mType = page0[1].toInt() and 0xff,
                    mSerial = getSerial(card),
                    mBaseDate = mBaseDate)
        }


        val FACTORY: UltralightCardTransitFactory = object : UltralightCardTransitFactory {
            override fun check(card: UltralightCard) = card.getPage(4).data[0].toInt() == 0x13

            override fun parseTransitData(card: UltralightCard) = parse(card)

            override fun parseTransitIdentity(card: UltralightCard) =
                    TransitIdentity(NAME, getSerial(card).toString())
        }

        private fun getSerial(card: UltralightCard): Long {
            val otp = card.getPage(3).data
            return otp.byteArrayToLong(0, 4)
        }
    }
}
