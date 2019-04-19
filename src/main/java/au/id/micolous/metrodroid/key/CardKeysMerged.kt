package au.id.micolous.metrodroid.key

import android.content.Context
import android.database.Cursor
import android.database.MergeCursor
import au.id.micolous.metrodroid.util.ImmutableByteArray

class CardKeysMerged (private val retrievers: List<CardKeysRetriever>): CardKeysRetriever {
    override fun forID(context: Context, id: Int): CardKeys? {
        retrievers.forEach {
            val r = it.forID(context, id)
            if (r != null) return r
        }
        return null
    }

    override fun makeCursor(context: Context): Cursor? = mergeCursors(
            retrievers.map { it.makeCursor(context) })

    /**
     * Retrieves a MIFARE Classic card keys from storage by its UID.
     * @param tagID The UID to look up (4 bytes)
     * @return Matching [ClassicCardKeys], or null if not found
     */
    override fun forTagID(context: Context, tagID: ImmutableByteArray): ClassicCardKeys? {
        for (retriever in retrievers) {
            val ck = retriever.forTagID(context, tagID)
            if (ck is ClassicCardKeys)
                return ck
        }
        return null
    }

    override fun makeStaticClassicCursor(context: Context): Cursor? = mergeCursors(
            retrievers.map { it.makeStaticClassicCursor(context) })

    companion object {
        private fun mergeCursors(input: List<Cursor?>): Cursor? {
            val f = input.filterNotNull()
            when (f.size) {
                0 -> return null
                1 -> return f[0]
                else -> return MergeCursor(f.toTypedArray())
            }
        }
    }
}