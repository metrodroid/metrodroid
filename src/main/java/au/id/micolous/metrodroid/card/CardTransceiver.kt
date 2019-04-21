/*
 * CardTransceiver.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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

import java.io.IOException

import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Abstracts platform-specific interfaces to NFC tags.
 */
interface CardTransceiver {
    /**
     * The physical-layer protocol which is used to connect to a tag.
     */
    enum class Protocol {
        /** ISO/IEC 144443 Type A */
        ISO_14443A,
        /** NFC-F (JIS X 6319-4) */
        JIS_X_6319_4,
    }

    /**
     * Connects to the tag with the given physical-layer protocol.
     *
     * If the tag is already connected on any protocol, this will be disconnected first.
     *
     * Calling this method while connected, with the same protocol parameter, resets connectivity
     * with the tag.
     *
     * @param protocol Physical-layer protocol to use
     * @throws UnsupportedProtocolException If the protocol is not supported by the tag or platform.
     */
    @Throws(UnsupportedProtocolException::class)
    fun connect(protocol: Protocol)

    /**
     * Disconnects from the tag.
     *
     * If not implemented, this does nothing.
     */
    fun disconnect() { }

    /**
     * Gets the UID of the tag that is currently connected.
     *
     * For [Protocol.JIS_X_6319_4] cards, this is the IDm.
     *
     * Returns null if no tag is presently connected.
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

    /**
     * Sends a message to the tag, and returns the response.
     *
     * In the event of no response, this must throw an [IOException].
     *
     * @throws IOException on communication errors or tag loss
     */
    @Throws(IOException::class)
    fun transceive(data: ImmutableByteArray): ImmutableByteArray

    /**
     * Thrown when trying to connect to tag with a protocol it, or the platform, does not support.
     */
    class UnsupportedProtocolException(message: String?) : IOException(message)
}
