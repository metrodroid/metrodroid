/*
 * InvalidDesfireFile.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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

import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import kotlinx.serialization.Transient

class InvalidDesfireFile (override val fileSettings: DesfireFileSettings,
                          override val raw: RawDesfireFile): DesfireFile() {
    private val errorMessage: String
        get () = raw.error ?: "Data is null"

    @Transient
    override val data: ImmutableByteArray
        get() = throw IllegalStateException("Invalid file: $errorMessage")

    override fun getRawData(id: Int): ListItem {
        return ListItem(Localizer.localizeString(R.string.invalid_file_title_format,
                NumberUtils.intToHex(id),
                errorMessage))
    }
}
