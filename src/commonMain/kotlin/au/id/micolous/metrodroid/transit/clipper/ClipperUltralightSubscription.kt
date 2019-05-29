/*
 * ClipperUltralightSubscription.kt
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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription

@Parcelize
internal class ClipperUltralightSubscription (private val mProduct: Int,
                                              private val mTripsRemaining: Int,
                                              private val mTransferExpiry: Int,
                                              private val mBaseDate: Int): Subscription() {

    override val subscriptionName: String?
        get() {
            when (mProduct and 0xf) {
                0x3 -> return Localizer.localizeString(R.string.clipper_single,
                        Localizer.localizeString(R.string.clipper_ticket_type_adult))
                0x4 -> return Localizer.localizeString(R.string.clipper_return,
                        Localizer.localizeString(R.string.clipper_ticket_type_adult))
                0x5 -> return Localizer.localizeString(R.string.clipper_single,
                        Localizer.localizeString(R.string.clipper_ticket_type_senior))
                0x6 -> return Localizer.localizeString(R.string.clipper_return,
                        Localizer.localizeString(R.string.clipper_ticket_type_senior))
                0x7 -> return Localizer.localizeString(R.string.clipper_single,
                        Localizer.localizeString(R.string.clipper_ticket_type_rtc))
                0x8 -> return Localizer.localizeString(R.string.clipper_return,
                        Localizer.localizeString(R.string.clipper_ticket_type_rtc))
                0x9 -> return Localizer.localizeString(R.string.clipper_single,
                        Localizer.localizeString(R.string.clipper_ticket_type_youth))
                0xa -> return Localizer.localizeString(R.string.clipper_return,
                        Localizer.localizeString(R.string.clipper_ticket_type_youth))
                else -> return mProduct.toString(16)
            }
        }

    override val remainingTripCount: Int?
        get() = if (mTripsRemaining == -1) null else mTripsRemaining

    override val subscriptionState: Subscription.SubscriptionState
        get() = when {
            mTripsRemaining == -1 -> Subscription.SubscriptionState.UNUSED
            mTripsRemaining == 0 -> Subscription.SubscriptionState.USED
            mTripsRemaining > 0 -> Subscription.SubscriptionState.STARTED
            else -> Subscription.SubscriptionState.UNKNOWN
        }

    override val transferEndTimestamp: Timestamp?
        get() = ClipperTransitData.clipperTimestampToCalendar(mTransferExpiry * 60L)

    override val validTo: Timestamp?
        get() = ClipperTransitData.clipperTimestampToCalendar(mBaseDate * 86400L)

    override val purchaseTimestamp: Timestamp?
        get() = ClipperTransitData.clipperTimestampToCalendar((mBaseDate - 89) * 86400L)

    override fun getAgencyName(isShort: Boolean): String? {
        return if (mProduct shr 4 == 0x21) ClipperData.getAgencyName(ClipperData.AGENCY_MUNI, isShort) else ClipperData.getAgencyName(mProduct shr 4, isShort)
    }
}
