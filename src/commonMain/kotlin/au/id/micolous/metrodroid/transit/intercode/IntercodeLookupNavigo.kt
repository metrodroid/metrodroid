/*
 * IntercodeLookupNavigo.kt
 *
 * Copyright 2009-2013 by 'L1L1'
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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.util.StationTableReader

private const val NAVIGO_STR = "navigo"

internal object IntercodeLookupNavigo : IntercodeLookupSTR(NAVIGO_STR) {
    override fun cardInfo(env: () -> En1545Parsed): CardInfo =
            if (env().getIntOrZero(En1545TransitData.HOLDER_CARD_TYPE) == 1) NAVIGO_DECOUVERTE_CARD_INFO else NAVIGO_CARD_INFO

    override val allCards: List<CardInfo>
        get() = listOf(NAVIGO_CARD_INFO)

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (station == 0)
            return null
        var mdstStationId = station or ((agency ?: 0) shl 16) or ((transport ?: 0) shl 24)
        val sectorId = station shr 9
        val stationId = station shr 4 and 0x1F
        var humanReadableId = station.toString()
        var fallBackName = station.toString()
        if (transport == En1545Transaction.TRANSPORT_TRAIN && (agency == RATP || agency == SNCF)) {
            mdstStationId = mdstStationId and -0xff0010 or 0x30000
        }
        if ((agency == RATP || agency == SNCF) && (transport == En1545Transaction.TRANSPORT_METRO || transport == En1545Transaction.TRANSPORT_TRAM)) {
            mdstStationId = mdstStationId and 0x0000fff0 or 0x3020000
            fallBackName = if (SECTOR_NAMES[sectorId] != null)
                Localizer.localizeString(R.string.navigo_sector_station_id,
                        SECTOR_NAMES[sectorId], stationId)
            else
                Localizer.localizeString(R.string.navigo_sector_id_station_id,
                        sectorId, stationId)
            humanReadableId = "$sectorId/$stationId"
        }

        return StationTableReader.getStationNoFallback(NAVIGO_STR, mdstStationId, humanReadableId)
                ?: Station.unknown(fallBackName)
    }


    override val subscriptionMap: Map<Int, StringResource> = mapOf(
            0 to R.string.navigo_forfait,
            3 to R.string.navigo_forfait_jour
    )

    private val SECTOR_NAMES = mapOf(
            // TODO: Move this to MdSt
            1 to "Cité",
            2 to "Rennes",
            3 to "Villette",
            4 to "Montparnasse",
            5 to "Nation",
            6 to "Saint-Lazare",
            7 to "Auteuil",
            8 to "République",
            9 to "Austerlitz",
            10 to "Invalides",
            11 to "Sentier",
            12 to "Île Saint-Louis",
            13 to "Daumesnil",
            14 to "Italie",
            15 to "Denfert",
            16 to "Félix Faure",
            17 to "Passy",
            18 to "Étoile",
            19 to "Clichy - Saint Ouen",
            20 to "Montmartre",
            21 to "Lafayette",
            22 to "Buttes Chaumont",
            23 to "Belleville",
            24 to "Père Lachaise",
            25 to "Charenton",
            26 to "Ivry - Villejuif",
            27 to "Vanves",
            28 to "Issy",
            29 to "Levallois",
            30 to "Péreire",
            31 to "Pigalle"
    )

    private const val RATP = 3
    private const val SNCF = 2

    private val NAVIGO_CARD_INFO = CardInfo(
            name = "Navigo",  // personalised card
            imageId = R.drawable.navigo,
            imageAlphaId = R.drawable.iso7810_id1_alpha,
            locationId = R.string.location_paris,
            region = TransitRegion.FRANCE,
            cardType = CardType.ISO7816)

    private val NAVIGO_DECOUVERTE_CARD_INFO = CardInfo(
            name = "Navigo découverte",  // anonymous card (lit: discovery)
            imageId = R.drawable.navigo,
            imageAlphaId = R.drawable.iso7810_id1_alpha,
            locationId = R.string.location_paris,
            region = TransitRegion.FRANCE,
            cardType = CardType.ISO7816)
}
