/*
 * RecordDesfireFile.kt
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.card.desfire.files

import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

class RecordDesfireFile (override val fileSettings: RecordDesfireFileSettings,
                         override val raw: RawDesfireFile): DesfireFile() {
    val records: List<ImmutableByteArray>
        get() =
            (0 until fileSettings.curRecords).map {
                data.sliceOffLen(fileSettings.recordSize * it, fileSettings.recordSize)
            }

    private val recordsSafe: List<ImmutableByteArray?>?
        get() {
            val recSize = fileSettings.recordSize
            if (recSize == 0)
                return null

            val numRecs = (data.size + recSize - 1) / recSize

            return (0 until numRecs).map {
                data.sliceOffLenSafe(recSize * it, fileSettings.recordSize)
            }
        }

    override fun getRawData(id: Int): ListItem {
        val recs = recordsSafe ?: return super.getRawData(id)

        val sub = recs.mapIndexed { idx, value ->
            ListItemRecursive.collapsedValue(Localizer.localizeString(R.string.record_title_format, idx), null,
                    value?.toHexDump() ?: FormattedString("invalid"))
        }
        return ListItemRecursive(Localizer.localizeString(R.string.file_title_format,
                id.hexString), fileSettings.subtitle, sub)
    }
}
