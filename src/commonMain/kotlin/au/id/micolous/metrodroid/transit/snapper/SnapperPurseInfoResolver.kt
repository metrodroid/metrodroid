/*
 * SnapperPurseInfoResolver.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
 *
 * Reference: https://github.com/micolous/metrodroid/wiki/Snapper
 */

package au.id.micolous.metrodroid.transit.snapper

import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfoResolver
import au.id.micolous.metrodroid.multi.R

object SnapperPurseInfoResolver : KSX6924PurseInfoResolver() {
    // TODO: Handle Snapper codes better.

    override val issuers = mapOf(
            0x02 to R.string.snapper_issuer_snapper
    )
}