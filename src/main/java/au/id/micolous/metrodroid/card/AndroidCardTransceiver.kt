/*
 * AndroidCardTransceiver.kt
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

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcF
import au.id.micolous.metrodroid.card.CardTransceiver.Protocol
import au.id.micolous.metrodroid.card.CardTransceiver.Protocol.*
import au.id.micolous.metrodroid.card.CardTransceiver.UnsupportedProtocolException
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Utils
import java.io.IOException

/**
 * Wrapper for Android's [Tag] class, to implement the [CardTransceiver] interface.
 */
class AndroidCardTransceiver(private val tag: Tag) : CardTransceiver {
    private var protocol: Protocol? = null
    override var uid: ImmutableByteArray? = null
        private set
    private var isoDep: IsoDep? = null
    private var nfcF: NfcF? = null
    override var defaultSystemCode: Int? = null
        private set
    override var pmm: ImmutableByteArray? = null
        private set

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    override fun connect(protocol: Protocol) {
        disconnect()

        when (protocol) {
            ISO_14443A -> {
                val isoDep = IsoDep.get(tag) ?: throw UnsupportedProtocolException(
                        "iso14a not supported by this tag")

                isoDep.connect()
                this.isoDep = isoDep
            }

            JIS_X_6319_4 -> {
                val nfcF = NfcF.get(tag) ?: throw UnsupportedProtocolException(
                        "nfcF not supported by this tag")

                nfcF.connect()
                this.nfcF = nfcF
                this.defaultSystemCode = Utils.byteArrayToInt(nfcF.systemCode)
                this.pmm = ImmutableByteArray.fromByteArray(nfcF.manufacturer)
            }

            else -> {
                throw UnsupportedProtocolException("Protocol not supported on this platform")
            }
        }

        uid = ImmutableByteArray.fromByteArray(tag.id)
        this.protocol = protocol
    }

    override fun disconnect() {
        val isoDep = this.isoDep
        this.isoDep = null
        val nfcF = this.nfcF
        this.nfcF = null
        uid = null
        defaultSystemCode = null
        pmm = null

        try {
            isoDep?.close()
        } catch (e: IOException) {
        }

        try {
            nfcF?.close()
        } catch (e: IOException) {
        }
    }

    @Throws(IOException::class)
    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        val request = data.dataCopy
        val response: ByteArray
        val isoDep = this.isoDep
        val nfcF = this.nfcF

        response = when {
            isoDep != null -> isoDep.transceive(request)
            nfcF != null -> nfcF.transceive(request)
            else -> throw IOException("Card protocol not connected")
        }

        return ImmutableByteArray.fromByteArray(response)
    }
}
