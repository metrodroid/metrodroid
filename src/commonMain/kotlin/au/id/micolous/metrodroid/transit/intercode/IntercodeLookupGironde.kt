/*
 * IntercodeTrip.kt
 *
 * Copyright 2009 by 'L1L1'
 * Copyright 2013-2014 by 'kalon33'
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it &&/or modify
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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo

internal object IntercodeLookupGironde : IntercodeLookupSTR("gironde"), IntercodeLookupSingle {

    override val cardInfo: CardInfo
        get() = CardInfo(
            name = "TransGironde",
            locationId = R.string.location_gironde,
            imageId = R.drawable.transgironde,
            imageAlphaId = R.drawable.iso7810_id1_alpha,
            cardType = CardType.ISO7816,
            preview = true)

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?,
                              agency: Int?, transport: Int?): FormattedString? {
        if (routeNumber == null)
            return null
        if (agency == TRANSGIRONDE)
            return FormattedString.language("Ligne $routeNumber", "fr-FR")
        return super.getRouteName(routeNumber, routeNumber, agency, transport)
    }

    private const val TRANSGIRONDE = 16
}
