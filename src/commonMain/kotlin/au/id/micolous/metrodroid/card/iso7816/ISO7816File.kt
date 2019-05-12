/*
 * ISO7816File.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.serializers.XMLIgnore
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents a file on a Calypso card.
 */
@Serializable
@XMLIgnore("selector")
data class ISO7816File internal constructor(
        @XMLId("data")
        @Optional
        val binaryData: ImmutableByteArray? = null,
        @XMLListIdx("index")
        @Optional
        private val records: Map<Int, ImmutableByteArray> = emptyMap(),
        @Optional
        val fci: ImmutableByteArray? = null) {
    @Transient
    val recordList get() = records.entries.sortedBy { it.key }.map { it.value }.toList()

    /**
     * Gets a record for a given index.
     * @param index Record index to retrieve.
     * @return ISO7816Record with that index, or null if not present.
     */
    fun getRecord(index: Int): ImmutableByteArray? = records[index]

    fun showRawData(selectorStr: String): ListItem {
        val recList = mutableListOf<ListItem>()
        if (binaryData != null)
            recList.add(ListItemRecursive.collapsedValue(Localizer.localizeString(R.string.binary_title_format),
                    binaryData.toHexDump()))
        if (fci != null)
            recList.add(ListItemRecursive(Localizer.localizeString(R.string.file_fci), null,
                    ISO7816TLV.infoWithRaw(fci)))
        for ((idx, data) in records)
            recList.add(ListItemRecursive.collapsedValue(Localizer.localizeString(R.string.record_title_format, idx),
                    data.toHexDump()))
        return ListItemRecursive(Localizer.localizeString(R.string.file_title_format, selectorStr),
                Localizer.localizePlural(R.plurals.record_count, records.size, records.size),
                recList)
    }

    override fun toString() = "<$TAG: data=$binaryData, records=$records>"

    companion object {
        private const val TAG = "ISO7816File"
    }
}
