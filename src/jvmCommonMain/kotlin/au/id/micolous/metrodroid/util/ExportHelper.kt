@file:JvmName("ExportHelperJvm")
/*
 * ExportHelper.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.time.TimestampFull


/**
 * Dynamically generates a filename for a card dump.
 *
 * @param tagId The tag's UID
 * @param scannedAt A [TimestampFull] of when the [Card] dump was created
 * @param format The format of the dump, used as a filename extension
 * @param gen Used for handling duplicate filenames in a ZIP
 */
fun makeFilename(tagId: String, scannedAt: TimestampFull, format: String, gen: Int = 0): String {
    val dt = scannedAt.isoDateTimeFilenameFormat()
    return if (gen != 0) "Metrodroid-$tagId-$dt-$gen.$format" else "Metrodroid-$tagId-$dt.$format"
}

/**
 * Dynamically generates a filename for a [Card] dump.
 *
 * @param card The [Card] dump to generate a filename for.
 */
fun makeFilename(card: Card): String
    = makeFilename(card.tagId.toHexString(), card.scannedAt, "json")
