/*
 * KROCAPData.kt
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
 */
package au.id.micolous.metrodroid.card.ksx6924

import au.id.micolous.metrodroid.card.iso7816.TagContents.DUMP_SHORT
import au.id.micolous.metrodroid.card.iso7816.TagContents.HIDE
import au.id.micolous.metrodroid.card.iso7816.TagDesc

object KROCAPData {
    // TODO: i18n
    val TAGMAP = mapOf(
            "50" to TagDesc("Payment terms", DUMP_SHORT),
            "47" to TagDesc("Supported protocols", DUMP_SHORT),
            "43" to TagDesc("ID center", DUMP_SHORT),
            "11" to TagDesc("Balance command", DUMP_SHORT),
            "4f" to TagDesc("ADF AID", DUMP_SHORT),
            "9f10" to TagDesc("Additional file references", DUMP_SHORT),
            "45" to TagDesc("Usercode", DUMP_SHORT),
            "5f24" to TagDesc("Expiry", DUMP_SHORT),
            "12" to TagDesc("Card serial number", HIDE),
            "13" to TagDesc("Operator serial number", DUMP_SHORT),
            "bf0c" to TagDesc("Discretionary data", DUMP_SHORT)
    )
}