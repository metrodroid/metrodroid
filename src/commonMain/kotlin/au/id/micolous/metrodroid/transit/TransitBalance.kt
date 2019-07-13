/*
 * TransitBalance.kt
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

package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp

interface TransitBalance : Parcelable {
    val balance: TransitCurrency

    val validFrom: Timestamp?
        get() = null

    val validTo: Timestamp?
        get() = null

    val name: String?
        get() = null

    companion object {
        fun formatValidity(balance: TransitBalance): FormattedString? {
            val validFrom = balance.validFrom?.format()
            val validTo = balance.validTo?.format()

            return when {
                validFrom != null && validTo != null -> Localizer.localizeFormatted(R.string.valid_format, validFrom, validTo)
                validTo != null -> Localizer.localizeFormatted(R.string.valid_to_format, validTo)
                else -> null
            }
        }

    }
}
