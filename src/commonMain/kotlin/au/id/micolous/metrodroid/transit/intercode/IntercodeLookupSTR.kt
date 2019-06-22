/*
 * IntercodeLookupSTR.kt
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

package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR

internal abstract class IntercodeLookupSTR(str: String) : En1545LookupSTR(str), IntercodeLookup {
    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        return if (contractTariff == null) null else Localizer.localizeString(R.string.unknown_format, contractTariff)
    }
}
