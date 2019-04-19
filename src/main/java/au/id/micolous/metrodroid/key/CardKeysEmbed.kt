/*
 * CardKeys.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import android.content.Context
import android.content.res.AssetManager
import android.database.Cursor
import android.database.MatrixCursor
import android.util.Log
import au.id.micolous.metrodroid.provider.KeysTableColumns
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import org.apache.commons.io.IOUtils
import org.json.JSONObject

class CardKeysEmbed (private val baseDir: String): CardKeysRetriever {
    override fun forID(context: Context, id: Int) =
            keyForId(context, listOf("classic", "static"), id)

    override fun makeCursor(context: Context): Cursor? =
            makeCursor(context, listOf("classic", "static"))

    override fun makeStaticClassicCursor(context: Context) = makeCursor(context, listOf("static"))

    override fun forTagID(context: Context, tagID: ImmutableByteArray): CardKeys? =
            fromEmbed(context, "classic", tagID.toHexString())

    private fun fromEmbed(context: Context, dir: String, name: String): CardKeys? = try {
        val inputStream = context.assets
                .open("$baseDir/$dir/$name.json", AssetManager.ACCESS_RANDOM)
        val b = IOUtils.toByteArray(inputStream)
        val k = JSONObject(String(b, Utils.getASCII()))
        CardKeys.fromJSON(k, "$dir/$name")
    } catch (e: Exception) {
        null
    }

    private fun keyForId(context: Context, dirs: List<String>, targetId: Int): CardKeys? {
        var ctr = START_COUNTER
        for (dir in dirs) {
            try {
                for (file in context.assets.list("$baseDir/$dir").orEmpty()) {
                    if (!file.endsWith(".json"))
                        continue
                    ctr--
                    if (ctr == targetId) {
                        val inputStream = context.assets
                                .open("$baseDir/$dir/$file", AssetManager.ACCESS_RANDOM)
                        val b = IOUtils.toByteArray(inputStream)
                        val k = JSONObject(String(b, Utils.getASCII()))
                        return CardKeys.fromJSON(k, "$dir/$file")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun makeCursor(context: Context, dirs: List<String>): Cursor {
        var ctr = START_COUNTER
        val cur = MatrixCursor(arrayOf(KeysTableColumns._ID,
                KeysTableColumns.CARD_ID, KeysTableColumns.CARD_TYPE,
                KeysTableColumns.KEY_DATA))
        for (dir in dirs) {
            try {
                context.assets.list("$baseDir/$dir").orEmpty().forEach lam@{
                    if (!it.endsWith(".json"))
                        return@lam
                    ctr--
                    try {
                        val inputStream = context.assets
                                .open("$baseDir/$dir/$it", AssetManager.ACCESS_RANDOM)
                        val b = IOUtils.toByteArray(inputStream)
                        val k = JSONObject(String(b, Utils.getASCII()))
                        val type = k.getString(CardKeys.JSON_KEY_TYPE_KEY)
                        val tagId = when (type) {
                            CardKeys.TYPE_MFC_STATIC -> CardKeys.CLASSIC_STATIC_TAG_ID
                            else -> k.getString(CardKeys.JSON_TAG_ID_KEY)
                        }
                        cur.addRow(arrayOf(ctr, tagId, type, k))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Log.d("CardKeysEmbed", "loaded ${START_COUNTER - ctr} keys for dirs ${dirs.joinToString(", ")}")
        return cur
    }

    companion object {
        private const val START_COUNTER = -10000
    }
}
