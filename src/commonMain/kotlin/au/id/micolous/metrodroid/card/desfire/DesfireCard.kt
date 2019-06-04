/*
 * DesfireCard.kt
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.card.CardProtocol
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLDesfireManufacturingData
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class DesfireCard constructor(
        @XMLId("manufacturing-data")
        @XMLDesfireManufacturingData
        val manufacturingData: ImmutableByteArray,
        @XMLListIdx("id")
        val applications: Map<Int, DesfireApplication>,
        @Optional
        override val isPartialRead: Boolean = false,
        @Optional
        val appListLocked: Boolean = false) : CardProtocol() {

    @Transient
    override val manufacturingInfo: List<ListItem>
        get() = DesfireManufacturingData(manufacturingData).info

    @Transient
    override val rawData: List<ListItem>
        get() = applications.map { (id,app) ->
                ListItemRecursive(makeName(id),
                        null, app.rawData)
            }

    private fun makeName(id: Int): String {
        val mifareAID = DesfireApplication.getMifareAID(id)
        if (mifareAID != null) {
            return Localizer.localizeString(R.string.mfc_aid_title_format,
                    mifareAID.first.hexString, mifareAID.second)
        }

        return Localizer.localizeString(R.string.application_title_format,
                id.hexString)
    }

    override fun parseTransitIdentity(): TransitIdentity? {
        for (f in DesfireCardTransitRegistry.allFactories)
            if (f.check(this))
                return f.parseTransitIdentity(this)

        return null
    }

    override fun parseTransitData(): TransitData? {
        for (f in DesfireCardTransitRegistry.allFactories)
            if (f.check(this))
                return f.parseTransitData(this)
        return null
    }

    fun getApplication(appId: Int): DesfireApplication? = applications[appId]
}
