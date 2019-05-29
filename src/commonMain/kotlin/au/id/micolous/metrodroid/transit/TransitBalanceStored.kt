/*
 * TransitBalanceStored.kt
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

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.Timestamp

@Parcelize
class TransitBalanceStored constructor(override val balance: TransitCurrency,
                                       override val name: String?,
                                       override val validFrom: Timestamp?,
                                       override val validTo: Timestamp?) : TransitBalance {
    constructor(bal: TransitCurrency, name: String?) : this(bal, name, null, null)
    constructor(bal: TransitCurrency, expiry: Timestamp?) : this(bal, null, null, expiry)
    constructor(bal: TransitCurrency, name: String?, expiry: Timestamp?) : this(bal, name, null, expiry)
}
