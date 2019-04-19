/*
 * ErgRecord.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.erg.record

import au.id.micolous.metrodroid.util.ImmutableByteArray
import java.util.*

/**
 * Represents a record inside of an ERG MIFARE Classic based card.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
open class ErgRecord internal constructor() {
    @FunctionalInterface
    abstract class Factory {
        abstract fun recordFromBytes(block: ImmutableByteArray): ErgRecord?
    }

    override fun toString(): String {
        return String.format(Locale.ENGLISH, "[%s]", javaClass.simpleName)
    }
}
