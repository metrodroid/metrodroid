/*
 * DesfireFile.kt
 *
 * Copyright (C) 2011 Eric Butler
 * Copyright (C) 2019 Google
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 * Vladimir Serbinenko
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

import au.id.micolous.metrodroid.card.desfire.DesfireProtocol
import au.id.micolous.metrodroid.card.desfire.settings.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

abstract class DesfireFile {
    abstract val fileSettings: DesfireFileSettings?
    abstract val raw: RawDesfireFile
    open val data: ImmutableByteArray
        get() = raw.data!!

    open fun getRawData(id: Int): ListItem {
        return ListItemRecursive(Localizer.localizeString(R.string.file_title_format,
                id.hexString),
                fileSettings?.subtitle,
                listOf(ListItem(null, data.toHexDump())))
    }

    companion object {
        fun create(raw: RawDesfireFile): DesfireFile {
            if (raw.settings == null) {
                return when (raw.readCommand) {
                    DesfireProtocol.READ_DATA -> StandardDesfireFile(null, raw)
                    DesfireProtocol.GET_VALUE -> ValueDesfireFile(null, raw)
                    // FIXME: Record files heavily rely on settings. Fortunately so far
                    // we didn't need to parse them on cards with locked directory listing
                    //DesfireProtocol.READ_RECORD -> RecordDesfireFile(null, raw)
                    else -> InvalidDesfireFile(fileSettings = InvalidDesfireFileSettings(), raw = raw)
                }
            }
            val fileSettings = DesfireFileSettings.create(raw.settings)
            if (raw.error != null || raw.data == null) {
                if (raw.isUnauthorized)
                    return UnauthorizedDesfireFile(fileSettings = fileSettings, raw = raw)
                return InvalidDesfireFile(fileSettings = fileSettings, raw = raw)
            }
            return when (fileSettings) {
                is RecordDesfireFileSettings -> RecordDesfireFile(fileSettings, raw)
                is ValueDesfireFileSettings -> ValueDesfireFile(fileSettings, raw)
                is StandardDesfireFileSettings -> StandardDesfireFile(fileSettings, raw)
                else -> throw IllegalArgumentException("Unknown file settings")
            }
        }

        fun create(settings: ImmutableByteArray, data: ImmutableByteArray) = create(RawDesfireFile(
                settings = settings,
                data = data))
    }
}
