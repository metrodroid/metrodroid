/*
 * DesfireAuthLog.kt
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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.util.NumberUtils

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class DesfireAuthLog(
        @XMLId("key-id")
        private val keyId: Int,
        private val challenge: ImmutableByteArray,
        private val response: ImmutableByteArray,
        private val confirm: ImmutableByteArray) {
    @Transient
    val rawData: ListItem
        get() =
            ListItemRecursive(R.string.desfire_keyex, Localizer.localizeString(R.string.desfire_key_number,
                    NumberUtils.intToHex(keyId)), listOf(
                    ListItemRecursive.collapsedValue(R.string.desfire_challenge, challenge.toHexDump()),
                    ListItemRecursive.collapsedValue(R.string.desfire_response, response.toHexDump()),
                    ListItemRecursive.collapsedValue(R.string.desfire_confirmation, confirm.toHexDump())))
}
