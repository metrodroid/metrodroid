/*
 * ValueDesfireFileSettings.kt
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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

/**
 * Contains FileSettings for Value file types.
 * See GetFileSettings for schemadata.
 */
class ValueDesfireFileSettings (buf: ImmutableByteArray) : DesfireFileSettingsImpl(buf) {
    private val lowerLimit = buf.byteArrayToIntReversed(4, 4)
    private val upperLimit = buf.byteArrayToIntReversed(8, 4)
    private var limitedCreditValue = buf.byteArrayToIntReversed(12, 4)
    private var limitedCreditEnabled = buf[16].toInt() != 0x00

    override val subtitle: String
        get() = Localizer.localizeString(R.string.desfire_value_format,
                Localizer.localizeString(fileTypeString),
                lowerLimit,
                upperLimit,
                limitedCreditValue,
                Localizer.localizeString(if (limitedCreditEnabled) R.string.enabled else R.string.disabled))
}
