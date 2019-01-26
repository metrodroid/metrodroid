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
import java.lang.Exception

class CardKeysFileReaderEmbed(private val context: Context,
                              private val baseDir: String) : CardKeysFileReader {
    override fun readFile(fileName: String): String? = try {
        context.assets.open("$baseDir/$fileName",
                AssetManager.ACCESS_RANDOM)?.reader(Charsets.UTF_8)?.readText()
    } catch (e: Exception) {
        null
    }

    override fun listFiles(dir: String) = context.assets.list("$baseDir/$dir")?.toList()
}

fun CardKeysEmbed(context: Context, baseDir: String) = CardKeysFromFiles(CardKeysFileReaderEmbed(context, baseDir))

