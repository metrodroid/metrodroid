/*
 * NFCVPage.kt
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
package au.id.micolous.metrodroid.card.nfcv

import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents a page of data on a NFC Vicinity card
 */
@Serializable
data class NFCVPage(
        @SerialName("data")
        @Optional
        val dataRaw: ImmutableByteArray = ImmutableByteArray.empty(),
        @Optional
        @XMLId("unauthorized")
        val isUnauthorized: Boolean = false) {
    @Transient
    val data: ImmutableByteArray
        get() {
            if (isUnauthorized)
                throw UnauthorizedException()
            return dataRaw
        }

    companion object {
        fun unauthorized() = NFCVPage(ImmutableByteArray.empty(), true)
    }
}
