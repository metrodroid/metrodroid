/*
 * Itso7816TransitData.kt
 *
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
package au.id.micolous.metrodroid.transit.itso

import au.id.micolous.metrodroid.card.itso.ItsoApplication
import au.id.micolous.metrodroid.transit.CardTransitFactory
import kotlinx.android.parcel.Parcelize

/**
 * Implements ITSO application on "Generic Microprocessor" (ie: ISO7816)
 *
 * Reference: ITSO TS Part 10 Section 3
 * https://www.itso.org.uk/services/specification-resources/the-itso-specification/itso-technical-specification/
 *
 * FIXME: This is untested.
 */
@Parcelize
class Itso7816TransitData(private val byteArray : ByteArray) : ItsoTransitData(byteArray) {
    override fun getCardName() = NAME

    companion object {
        private const val NAME = "ITSO (ISO 7816)"

        val FACTORY: CardTransitFactory<ItsoApplication> = object : ItsoTransitData.ItsoTransitFactory<ItsoApplication> {
            override fun check(card: ItsoApplication): Boolean = true

            override fun getShell(card: ItsoApplication): ByteArray? =
                    card.getFile(ItsoApplication.File.SHELL.selector)?.binaryData

            override fun parseTransitData(card: ItsoApplication) : ItsoTransitData? {
                val shell = getShell(card) ?: return null
                return Itso7816TransitData(shell)
            }
        }
    }
}