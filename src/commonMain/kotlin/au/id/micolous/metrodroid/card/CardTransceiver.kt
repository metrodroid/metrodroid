/*
 * CardTransceiver.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.card

import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Abstracts platform-specific interfaces to NFC cards.
 */
interface CardTransceiver {
    /**
     * Gets the UID of the card that is currently connected.
     *
     * For [Protocol.JIS_X_6319_4] cards, this is the IDm.
     *
     * Returns null if no card is presently connected.
     */
    val uid : ImmutableByteArray?

    /**
     * Sends a message to the card, and returns the response.
     *
     * @throws CardTransceiveException On card communication errors
     * @throws CardLostException If the card moves out of the field.
     */
    suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray
}