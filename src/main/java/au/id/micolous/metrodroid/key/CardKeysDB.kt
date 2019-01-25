package au.id.micolous.metrodroid.key

import android.content.Context
import android.database.Cursor
import android.net.Uri
import au.id.micolous.metrodroid.provider.CardKeyProvider
import au.id.micolous.metrodroid.provider.KeysTableColumns
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.json.JsonTreeParser

class CardKeysDB (private val context: Context): CardKeysRetriever {
    override fun forClassicStatic(): ClassicStaticKeys? {
        val cursor = fromUri(Uri.withAppendedPath(CardKeyProvider.CONTENT_BY_UID_URI,
                CardKeys.CLASSIC_STATIC_TAG_ID)) ?: return ClassicStaticKeys.fallback()
        var keys: ClassicStaticKeys? = null

        // Static key requests should give all of the static keys.
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE)) != CardKeys.TYPE_MFC_STATIC)
                continue
            try {
                val id = cursor.getInt(cursor.getColumnIndex(KeysTableColumns._ID))
                val json = JsonTreeParser.parse(cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA)))
                val nk = ClassicStaticKeys.fromJSON(json, "cursor/$id") ?: continue
                if (keys == null)
                    keys = nk
                else
                    keys += nk
            } catch (ignored: Exception) {
            }
        }
        return keys
    }

    override fun forTagID(tagID: ImmutableByteArray): CardKeys? =
            fromCursor(fromUri(uri =
            Uri.withAppendedPath(CardKeyProvider.CONTENT_BY_UID_URI, tagID.toHexString())))

    /**
     * Retrieves a key by its internal ID.
     * @return Matching [ClassicCardKeys], or null if not found.
     */
    override fun forID(id: Int) = if (id >= 0)
        fromCursor(fromUri(Uri.withAppendedPath(CardKeyProvider.CONTENT_URI,
                Integer.toString(id))))
    else
        null

    private fun fromUri(uri: Uri): Cursor? =
            context.contentResolver.query(uri, null,
                null, null, null)

    private fun fromCursor(cursor: Cursor?): CardKeys? {
        if (cursor == null)
            return null
        if (!cursor.moveToFirst())
            return null
        return CardKeys.fromJSON(
                JsonTreeParser.parse(cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA))),
                cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE)))
    }
}