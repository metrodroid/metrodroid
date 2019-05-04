/*
 * DesfireFileSettings.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.desfire.settings

import au.id.micolous.metrodroid.util.ImmutableByteArray

interface DesfireFileSettings {
    val subtitle: String

    companion object {
        /* DesfireFile Types */
        const val STANDARD_DATA_FILE = 0x00.toByte()
        const val BACKUP_DATA_FILE = 0x01.toByte()
        const val VALUE_FILE = 0x02.toByte()
        const val LINEAR_RECORD_FILE = 0x03.toByte()
        const val CYCLIC_RECORD_FILE = 0x04.toByte()

        fun create(data: ImmutableByteArray): DesfireFileSettings = when (data[0]) {
            STANDARD_DATA_FILE, BACKUP_DATA_FILE -> StandardDesfireFileSettings(data)
            LINEAR_RECORD_FILE, CYCLIC_RECORD_FILE -> RecordDesfireFileSettings(data)
            VALUE_FILE -> ValueDesfireFileSettings(data)
            else -> throw Exception("Unknown file type: " + data[0].toInt().toString(16))
        }
    }
}

