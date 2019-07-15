/*
 * DesfireApplication.kt
 *
 * Copyright (C) 2011 Eric Butler
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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.card.desfire.files.DesfireFile
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.ui.ListItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class DesfireApplication(
        @XMLListIdx("id")
        private val files: Map<Int, RawDesfireFile>,
        @XMLId("auth-log")
        private val authLog: List<DesfireAuthLog> = emptyList(),
        private val dirListLocked: Boolean = false) {
    @Transient
    val interpretedFiles: Map<Int, DesfireFile> = files.mapValues { (_, v) -> DesfireFile.create(v) }
    @Transient
    val rawData: List<ListItem>
        get() = interpretedFiles.map { (k, v) -> v.getRawData(k) } + authLog.map { it.rawData }

    fun getFile(fileId: Int) = interpretedFiles[fileId]

    companion object {
        /**
         * Converts the MIFARE DESFire application into a MIFARE Classic Application Directory AID,
         * according to the process described in NXP AN10787.
         * @return A Pair of (aid, sequence) if the App ID is encoded per AN10787, or null otherwise.
         */
        fun getMifareAID(appid: Int): Pair<Int, Int>? {
            if ((appid and 0xf0) != 0xf0) {
                // Not a MIFARE AID
                return null
            }
            // 0x1234f6 == 0x6341 seq 0x2
            val aid = ((appid and 0xf00000) shr 20) or
                    ((appid and 0xff00) shr 4) or
                    ((appid and 0xf) shl 12)
            val seq = (appid and 0x0f0000) shr 16
            return Pair(aid, seq)
        }
    }
}


