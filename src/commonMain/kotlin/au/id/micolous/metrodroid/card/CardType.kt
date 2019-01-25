/*
 * CardType.java
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015, 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card

enum class CardType constructor(private val mValue: Int) {
    MifareClassic(0),
    MifareUltralight(1),
    MifareDesfire(2),
    CEPAS(3),
    FeliCa(4),
    ISO7816(5),
    MultiProtocol(7),
    Unknown(65535);

    fun toInteger() = mValue

    override fun toString() = when (mValue) {
        0 -> "MIFARE Classic"
        1 -> "MIFARE Ultralight"
        2 -> "MIFARE DESFire"
        3 -> "CEPAS"
        4 -> "FeliCa"
        5 -> "ISO7816"
        6 -> "Calypso"
        7 -> "Multi-protocol"
        65535 -> "Unknown"
        else -> "Unknown"
    }

    companion object {
        fun parseValue(value: String): CardType {
            return values().find { it.mValue == value.toInt() }!!
        }
    }
}
