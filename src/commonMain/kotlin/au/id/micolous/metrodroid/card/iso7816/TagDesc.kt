/*
 * TagDesc.kt
 *
 * Copyright 2019 Google
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
package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.util.*

data class TagDesc(val name: StringResource,
                   val contents: TagContents,
                   val hiding: TagHiding = TagHiding.NONE) {
    fun interpretTag(data: ImmutableByteArray) : String {
        when {
            hiding == TagHiding.CARD_NUMBER && Preferences.hideCardNumbers -> return ""
            // TODO: implement this properly
            hiding == TagHiding.DATE && Preferences.obfuscateTripDates -> return ""
        }

        return when (contents) {
            TagContents.ASCII -> data.readASCII()
            TagContents.DUMP_SHORT -> data.toHexString()
            TagContents.DUMP_LONG -> data.toHexString()
            TagContents.CURRENCY -> {
                val n = NumberUtils.convertBCDtoInteger(data.byteArrayToInt())
                currencyNameByCode(n) ?: n.toString()
            }
            TagContents.COUNTRY -> countryCodeToName(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
            else -> data.toHexString()
        }
    }
}

enum class TagContents {
    DUMP_SHORT,
    DUMP_LONG,
    ASCII,
    DUMP_UNKNOWN,
    HIDE,
    CURRENCY,
    COUNTRY
}

enum class TagHiding {
    NONE,
    CARD_NUMBER,
    DATE
}

val HIDDEN_TAG = TagDesc(R.string.unknown, TagContents.HIDE)
