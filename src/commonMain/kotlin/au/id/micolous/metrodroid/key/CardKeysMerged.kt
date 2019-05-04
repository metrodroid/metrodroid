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