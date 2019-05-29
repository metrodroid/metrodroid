/*
 * CEPASCompatPurse.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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

import au.id.micolous.metrodroid.serializers.XMLHex
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

// This file is only for reading old dumps
@Serializable
data class CEPASCompatPurse(
        @Optional
        @XMLHex
        val can: ImmutableByteArray? = null,
        internal val id: Int = 0,
        @XMLId("purse-balance")
        val purseBalance: Int = 0)
