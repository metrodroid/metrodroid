/*
 * UltralightPage.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a page of data on a MIFARE Ultralight (4 bytes)
 */
@Serializable
data class UltralightPage(
        @SerialName("data")
        val dataRaw: ImmutableByteArray = ImmutableByteArray.empty(),
        @XMLId("unauthorized")
        val isUnauthorized: Boolean = false) {
    val data: ImmutableByteArray
        get() {
            if (isUnauthorized)
                throw UnauthorizedException()
            return dataRaw
        }

    companion object {
        fun unauthorized() = UltralightPage(ImmutableByteArray.empty(), true)
    }
}
