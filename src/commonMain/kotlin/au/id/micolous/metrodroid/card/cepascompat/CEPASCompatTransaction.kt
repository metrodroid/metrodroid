/*
 * CEPASCompatTransaction.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.cepascompat

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// This file is only for reading old dumps
@Parcelize
@Serializable
data class CEPASCompatTransaction(
        val type: Byte,
        val amount: Int,
        private val date: Long,
        private val date2: Long,
        @SerialName("user-data")
        val userData: String) : Parcelable {
    val unixDate: Long
        get() =
        // Compatibility for Metrodroid <= 2.9.34
        // Timestamps were stored as seconds since UNIX epoch.
            if (date != 0L)
                date
            else
                date2 / 1000
}
