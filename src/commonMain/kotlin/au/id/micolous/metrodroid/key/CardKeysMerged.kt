/*
 * CardKeysMerged.kt
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

class CardKeysMerged (private val retrievers: List<CardKeysRetriever>) : CardKeysRetriever {
    /**
     * Retrieves all statically defined MIFARE Classic keys.
     * @return All [ClassicCardKeys], or null if not found
     */
    override fun forClassicStatic(): ClassicStaticKeys? {
        var merged = ClassicStaticKeys.fallback()
        for (retriever in retrievers) {
            merged += retriever.forClassicStatic() ?: continue
        }
        return merged
    }

    override fun forID(id: Int): CardKeys? {
        retrievers.forEach {
            val r = it.forID(id)
            if (r != null) return r
        }
        return null
    }
    /**
     * Retrieves a MIFARE Classic card keys from storage by its UID.
     * @param tagID The UID to look up (4 bytes)
     * @return Matching [ClassicCardKeys], or null if not found
     */
    override fun forTagID(tagID: ImmutableByteArray): CardKeys? {
        for (retriever in retrievers) {
            val ck = retriever.forTagID(tagID)
            if (ck is ClassicCardKeys)
                return ck
        }
        return null
    }
}