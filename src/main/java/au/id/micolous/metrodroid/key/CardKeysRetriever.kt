package au.id.micolous.metrodroid.key

import android.content.Context
import android.database.Cursor
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import org.json.JSONException

interface CardKeysRetriever {
    @Throws(JSONException::class)
    fun forTagID(context: Context, tagID: ImmutableByteArray): CardKeys?

    fun makeStaticClassicCursor(context: Context): Cursor?

    fun makeCursor(context: Context): Cursor?

    @Throws(JSONException::class)
    fun forID(context: Context, id: Int): CardKeys?
}