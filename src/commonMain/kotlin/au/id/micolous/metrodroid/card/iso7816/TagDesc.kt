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
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.*
import kotlinx.datetime.Month
import kotlinx.datetime.number

data class TagDesc(val name: StringResource,
                   val contents: TagContents,
                   val hiding: TagHiding = TagHiding.NONE
) {
    private val isHidden get() = when {
        hiding == TagHiding.CARD_NUMBER && Preferences.hideCardNumbers -> true
        // TODO: implement this properly
        hiding == TagHiding.DATE && Preferences.obfuscateTripDates -> true
        contents == TagContents.DUMP_LONG && Preferences.hideCardNumbers -> true
        contents == TagContents.DUMP_UNKNOWN && (Preferences.hideCardNumbers || Preferences.obfuscateTripDates) -> true
        else -> false
    }
    fun interpretTag(data: ImmutableByteArray): ListItemInterface? =
        if (isHidden)
            null
        else
            contents.interpretTag(name, data)
    fun interpretTagString(data: ImmutableByteArray): String =
            if (isHidden)
                ""
            else
                contents.interpretTagString(data)
}

interface TagContentsInterface {
    fun interpretTagString(data: ImmutableByteArray): String
    fun interpretTag(name: StringResource, data: ImmutableByteArray): ListItemInterface? =
            if (data.isEmpty())
                null
            else
                ListItem(name, interpretTagString(data))
}

enum class TagContents : TagContentsInterface {
    DUMP_SHORT {
        override fun interpretTagString(data: ImmutableByteArray): String = data.toHexString()
    },
    DUMP_LONG {
        override fun interpretTagString(data: ImmutableByteArray): String = data.toHexString()
        override fun interpretTag(name: StringResource, data: ImmutableByteArray): ListItemInterface? =
                if (data.isEmpty()) null else ListItem(name, data.toHexDump())
    },
    ASCII {
        override fun interpretTagString(data: ImmutableByteArray): String = data.readASCII()
    },
    DUMP_UNKNOWN {
        override fun interpretTagString(data: ImmutableByteArray): String = data.toHexString()
        override fun interpretTag(name: StringResource, data: ImmutableByteArray): ListItemInterface? =
                if (data.isEmpty()) null else ListItem(name, data.toHexDump())
    },
    HIDE {
        override fun interpretTagString(data: ImmutableByteArray): String = ""
        override fun interpretTag(name: StringResource, data: ImmutableByteArray): ListItemInterface? = null
    },
    CURRENCY {
        override fun interpretTagString(data: ImmutableByteArray): String {
            val n = NumberUtils.convertBCDtoInteger(data.byteArrayToInt())
            return currencyNameByCode(n) ?: n.toString()
        }
    },
    COUNTRY_BCD {
        override fun interpretTagString(data: ImmutableByteArray): String = countryCodeToName(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
    },
    COUNTRY_ASCIINUM {
        override fun interpretTagString(data: ImmutableByteArray): String = countryCodeToName(data.readASCII().toInt())
    },
    LANGUAGE_LIST {
        override fun interpretTagString(data: ImmutableByteArray): String = data.readASCII().chunked(2) {
            languageCodeToName(it.toString()) ?: it.toString() }.joinToString(", ")
    },
    FDDA {
        private fun subList(data: ImmutableByteArray): List<ListItem> {
            val sl = mutableListOf(
                    ListItem(R.string.emv_fdda_version_no, data.byteArrayToInt(0, 1).toString()),
                    ListItem(R.string.emv_fdda_unpredictable_number, data.getHexString(1, 4)),
                    ListItem(R.string.emv_fdda_transaction_qualifiers, data.getHexString(5, 2)),
                    ListItem(R.string.emv_fdda_rfu, data.getHexString(7, 1)),
            )
            if (data.size > 8)
                sl.add(ListItem(R.string.emv_fdda_tail, data.sliceOffLen(8, data.size - 8).toHexDump()))
            return sl
        }
        override fun interpretTagString(data: ImmutableByteArray): String =
                if (data.size < 8)
                    data.toHexString()
                else
                    subList(data).map { it.text1?.unformatted.orEmpty() + ": " + it.text2?.unformatted.orEmpty() }.joinToString(", ")
        override fun interpretTag(name: StringResource, data: ImmutableByteArray): ListItemInterface {
            if (data.size < 8)
                return ListItem(name, data.getHexString())
            return ListItemRecursive(name, null, subList(data))
        }
    },
    CONTENTS_DATE {
        private fun adjustYear(yy: Int): Int {
            if (yy < 80)
                return 2000 + yy
            if (yy in 81..99)
                return 1900 + yy
            return yy
        }
        override fun interpretTagString(data: ImmutableByteArray): String = when(data.size) {
            3 -> Daystamp(
                    adjustYear(data.convertBCDtoInteger(0, 1)),
                    data.convertBCDtoInteger(1, 1) - 1,
                    data.convertBCDtoInteger(2, 1)).format().unformatted
            2 -> TripObfuscator.maybeObfuscateTS(Daystamp(
                    adjustYear(data.convertBCDtoInteger(0, 1)),
                    data.convertBCDtoInteger(1, 1) - 1,
                    1)).let { "${it.month.number}/${it.year}" }
            else -> if (Preferences.obfuscateTripDates) "" else data.toHexString()
        }
    }
}

enum class TagHiding {
    NONE,
    CARD_NUMBER,
    DATE
}

val HIDDEN_TAG = TagDesc(R.string.unknown, TagContents.HIDE)
val UNKNOWN_TAG = TagDesc(R.string.unknown, TagContents.DUMP_UNKNOWN)
