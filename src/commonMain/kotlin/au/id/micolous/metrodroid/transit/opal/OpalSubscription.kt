/*
 * OpalSubscription.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.opal

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp

/**
 * Class describing auto-topup on Opal.
 *
 *
 * Opal has no concept of subscriptions, but when auto-topup is enabled, you no longer need to
 * manually refill the card with credit.
 *
 *
 * Dates given are not valid.
 */
@Suppress("PLUGIN_WARNING")
@Parcelize
internal class OpalSubscription private constructor() : Subscription() {
    // Start of Opal trial
    override val validFrom: Timestamp?
        get() = Daystamp(2012, 12, 7)

    // Maximum possible date representable on the card
    override val validTo: Timestamp?
        get() = Daystamp(2159, 6, 6)

    override val subscriptionName: String?
        get() = Localizer.localizeString(R.string.opal_automatic_top_up)

    override val paymentMethod: Subscription.PaymentMethod
        get() = Subscription.PaymentMethod.CREDIT_CARD

    override fun getAgencyName(isShort: Boolean): String? {
        return Localizer.localizeString(R.string.opal_agency_tfnsw)
    }

    companion object {
        val instance = OpalSubscription()
    }
}
