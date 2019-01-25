/*
 * XmlUtils.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.serializers

import kotlinx.serialization.toUtf8Bytes

object XmlUtils {
    private val CARDS_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cards>\n".toUtf8Bytes()
    private val CARDS_FOOTER = "</cards>\n".toUtf8Bytes()
    private val CARDS_SEPARATOR = byteArrayOf(10) //  \n

    fun concatCardsFromString(cards: Iterator<String>): String {
        val os = StringBuilder()
        os.append(CARDS_HEADER)

        while (cards.hasNext()) {
            val s = cards.next()
            os.append(cutXmlDef(s))
            os.append(CARDS_SEPARATOR)
        }

        os.append(CARDS_FOOTER)

        return os.toString()
    }

    fun cutXmlDef(data: String): String {
        return if (!data.startsWith("<?")) data else data.substring(data.indexOf("?>") + 2)
    }
}
