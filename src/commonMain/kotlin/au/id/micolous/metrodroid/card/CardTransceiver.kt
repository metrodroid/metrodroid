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
import kotlinx.io.core.Closeable

/**
 * Abstracts platform-specific interfaces to NFC cards.
 */
interface CardTransceiver : Closeable {
    /**
     * The physical-layer protocol which is used to connect to a card.
     */
    enum class Protocol {
        /** ISO/IEC 144443 Type A */
        ISO_14443A,
        /** NFC-F (JIS X 6319-4) */
        JIS_X_6319_4,
        /** NFC-A */
        NFC_A,
        // TODO: Support other protocols
    }

    /**
     * Connects to the card with the given physical-layer protocol.
     *
     * If the card is already connected on any protocol, this will be disconnected first.
     *
     * Calling this method while connected, with the same protocol parameter, resets connectivity
     * with the card.
     *
     * @param protocol Physical-layer protocol to use
     * @throws CardProtocolUnsupportedException If the protocol is not supported by the card or
     * platform.
     */
    fun connect(protocol: Protocol)

    /**
     * Disconnects from the card.
     *
     * If not implemented, this does nothing.
     *
     * This must not throw an error -- if the tag is _already_ disconnected, fine, good, lets
     * move on, no need to ask for permission.
     */
    override fun close() { }

    /**
     * Gets the UID of the card that is currently connected.
     *
     * For [Protocol.JIS_X_6319_4] cards, this is the IDm.
     *
     * Returns null if no card is presently connected.
     */
    val uid : ImmutableByteArray?

    /**
     * Gets the default system code associated with the card.
     *
     * Only valid after [connect] has been called, and for [Protocol.JIS_X_6319_4] cards.
     *
     * Returns null otherwise.
     */
    val defaultSystemCode : Int? get() = null

    /**
     * Gets the Manufacture Parameters (PMm) of the card that is currently selected.
     *
     * Only valid after [connect] has been called, and for [Protocol.JIS_X_6319_4] cards.
     *
     * Returns null otherwise.
     */
    val pmm : ImmutableByteArray? get() = null

    val atqa : ImmutableByteArray? get() = null
    val sak : Short? get() = null

    /**
     * Sends a message to the card, and returns the response.
     *
     * @throws CardTransceiveException On card communication errors
     * @throws CardLostException If the card moves out of the field.
     */
    suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray
}
