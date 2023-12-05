/*
 * IntercodeLookupPassPass.kt
 *
 * Copyright 2023 by 'Altonss'
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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.util.StationTableReader

internal object IntercodeLookupPassPass : IntercodeLookupSTR("passpass"), IntercodeLookupSingle {

    override val cardInfo: CardInfo
        get() = CardInfo(
                name = "Pass Pass",
                locationId = R.string.location_hauts_de_france,
                imageId = R.drawable.passpass,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816,
                region = TransitRegion.FRANCE,
                preview = true)

    override val subscriptionMap: Map<Int, StringResource> = mapOf(
            24577 to R.string.ilevia_trajet_unitaire,
            24578 to R.string.ilevia_trajet_unitaire_x10,
            25738 to R.string.ilevia_mensuel,
            25743 to R.string.ilevia_10mois
    )

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?,
                              agency: Int?, transport: Int?): FormattedString? {
        if (agency == ILEVIA && routeNumber != null)
            return StationTableReader.getLineName("passpass", routeNumber)
        return super.getRouteName(routeNumber, routeNumber, agency, transport)
    }

    private const val ILEVIA = 23
}
