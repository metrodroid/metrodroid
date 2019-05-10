/*
 * ClassicKeys.kt
 *
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

package au.id.micolous.metrodroid.key

import au.id.micolous.metrodroid.util.ImmutableByteArray

interface ClassicKeys : CardKeys {
    fun isEmpty(): Boolean

    /**
     * Gets all keys for the card.
     *
     * @return All [ClassicSectorKey] for the card.
     */
    fun getAllKeys(tagId: ImmutableByteArray): List<ClassicSectorKey>

    /**
     * Gets the keys for a particular sector on the card.
     *
     * @param sectorNumber The sector number to retrieve the key for
     * @param preferences Sorted list of preferred card IDs
     * @return All candidate [ClassicSectorKey] for that sector, or an empty list if there is
     * no known key, or the sector is out of range.
     */
    fun getCandidates(sectorNumber: Int, tagId: ImmutableByteArray, preferences: List<String>): List<ClassicSectorKey>
}
