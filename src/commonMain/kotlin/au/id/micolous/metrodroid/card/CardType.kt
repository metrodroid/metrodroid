/*
 * CardType.kt
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource

enum class CardType(
    val value: Int,
    val label: StringResource) {

    MifareClassic(0, R.string.card_media_mfc),
    MifareUltralight(1, R.string.card_media_mfu),
    MifareDesfire(2, R.string.card_media_mfd),
    CEPAS(3, R.string.card_media_cepas),
    FeliCa(4, R.string.card_media_felica),
    ISO7816(5, R.string.card_media_iso7816),
    MultiProtocol(7, R.string.card_media_multi_protocol),
    Vicinity(8, R.string.card_media_vicinity),
    MifarePlus(9, R.string.card_media_mfp),
    Unknown(65535, R.string.unknown_card);

    companion object {
        fun parseValue(value: Int) = values().find { it.value == value } ?: Unknown
    }
}
