package au.id.micolous.metrodroid.key

import android.content.Context
import android.database.Cursor
import android.net.Uri
import au.id.micolous.metrodroid.provider.CardKeyProvider
import au.id.micolous.metrodroid.provider.KeysTableColumns
import au.id.micolous.metrodroid.util.ImmutableByteArray
import org.json.JSONException
import org.json.JSONObject

object CardKeysDB : CardKeysRetriever {
    override fun forTagID(context: Context, tagID: ImmutableByteArray): CardKeys? =
            fromCursor(fromUri(context = context, uri =
            Uri.withAppendedPath(CardKeyProvider.CONTENT_BY_UID_URI, tagID.toHexString())))

    override fun makeStaticClassicCursor(context: Context): Cursor? =
            fromUri(context, Uri.withAppendedPath(CardKeyProvider.CONTENT_BY_UID_URI,
                    CardKeys.CLASSIC_STATIC_TAG_ID))

    /**
     * Retrieves a key by its internal ID.
     * @return Matching [ClassicCardKeys], or null if not found.
     */
    @Throws(JSONException::class)
    override fun forID(context: Context, id: Int) = if (id >= 0)
        fromCursor(fromUri(context, Uri.withAppendedPath(CardKeyProvider.CONTENT_URI,
                Integer.toString(id))))
    else
        null

    @Throws(JSONException::class)
    private fun fromUri(context: Context, uri: Uri): Cursor? =
            context.contentResolver.query(uri, null,
                null, null, null)

    @Throws(JSONException::class)
    private fun fromCursor(cursor: Cursor?): CardKeys? {
        if (cursor == null)
            return null
        if (!cursor.moveToFirst())
            return null
        return CardKeys.fromJSON(
                JSONObject(cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA))),
                cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE)))
    }

    override fun makeCursor(context: Context): Cursor? =
            context.contentResolver.query(CardKeyProvider.CONTENT_URI, null, null,
        null,KeysTableColumns.CREATED_AT + " DESC")
}