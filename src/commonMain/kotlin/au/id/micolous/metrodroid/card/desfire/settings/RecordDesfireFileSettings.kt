/*
 * RecordDesfireFileSettings.kt
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

package au.id.micolous.metrodroid.card.desfire.settings

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.ImmutableByteArray

class RecordDesfireFileSettings (settings: ImmutableByteArray): DesfireFileSettingsImpl(settings) {
    val recordSize = settings.byteArrayToIntReversed(4, 3)
    private val maxRecords = settings.byteArrayToIntReversed(7, 3)
    val curRecords = settings.byteArrayToIntReversed(10, 3)

    override val subtitle: String
        get() = Localizer.localizePlural(R.plurals.desfire_record_format,
                curRecords,
                Localizer.localizeString(fileTypeString),
                curRecords,
                maxRecords,
                recordSize)
}
