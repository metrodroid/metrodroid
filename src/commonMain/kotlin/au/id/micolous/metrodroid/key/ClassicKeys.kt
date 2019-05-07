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
