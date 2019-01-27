/*
 * FallbackFactory.kt
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData
import au.id.micolous.metrodroid.util.Preferences

internal actual class FallbackFactory : ClassicCardTransitFactory {
    val fallback
        get () = Preferences.mfcFallbackReader

    override fun check(card: ClassicCard) =
            fallback in SMARTRIDER

    override fun parseTransitIdentity(card: ClassicCard) = when (fallback) {
        // This has a proper check now, but is included for legacy reasons.
        //
        // Before the introduction of key-based detection for these cards, Metrodroid did
        // not record the key inside the ClassicCard XML structure.
        in SMARTRIDER -> SmartRiderTransitData.FACTORY.parseTransitIdentity(card)
        else -> null
    }

    override fun parseTransitData(card: ClassicCard) = when (fallback) {
        // This has a proper check now, but is included for legacy reasons.
        //
        // Before the introduction of key-based detection for these cards, Metrodroid did
        // not record the key inside the ClassicCard XML structure.
        in SMARTRIDER -> SmartRiderTransitData.FACTORY.parseTransitData(card)
        else -> null
    }

    companion object {
        private val SMARTRIDER = listOf("myway", "smartrider")
    }
}
