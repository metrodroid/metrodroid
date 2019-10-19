/*
 * FelicaSystem.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2019 Google
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
package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.hexString
import kotlinx.serialization.Serializable

@Serializable
data class FelicaSystem(
    /** Service codes that are present in this System */
    @XMLListIdx("code") val services: Map<Int, FelicaService> = emptyMap(),
    /** When reading, did we skip trying to read the contents of this system? */
    val skipped: Boolean = false) {
    fun getService(serviceCode: Int) = services[serviceCode]

    /** Shows raw data for all Services within this System */
    fun rawData(systemCode: Int): List<ListItem> {
        val emptyServices = services.filterValues { it.blocks.isEmpty() }
        val skippedServiceIds = emptyServices.filterValues { it.skipped }.keys
        val emptyServiceIds = emptyServices.filterValues { !it.skipped }.keys

        return listOfNotNull(
            if (emptyServiceIds.isEmpty()) { null } else {
                ListItem(Localizer.localizePlural(R.plurals.felica_empty_service_codes,
                    emptyServiceIds.size, emptyServiceIds.size),
                    emptyServiceIds.joinToString { it.hexString })
            },
            if (skippedServiceIds.isEmpty()) { null } else {
                ListItem(Localizer.localizePlural(R.plurals.felica_skipped_service_codes,
                    skippedServiceIds.size, skippedServiceIds.size),
                    skippedServiceIds.joinToString { it.hexString })
            }
        ) + services.mapNotNull { (serviceCode, service) ->
            if (service.blocks.isEmpty()) {
                null
            } else {
                ListItemRecursive(
                        Localizer.localizeString(R.string.felica_service_title_format,
                                serviceCode.hexString,
                                Localizer.localizeString(
                                        FelicaUtils.getFriendlyServiceName(systemCode,
                                                serviceCode))),
                        Localizer.localizePlural(R.plurals.block_count,
                                service.blocks.size, service.blocks.size), service.rawData)
            }
        }
    }
}
