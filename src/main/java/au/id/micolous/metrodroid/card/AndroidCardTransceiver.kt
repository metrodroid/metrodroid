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
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcF
import au.id.micolous.metrodroid.card.CardTransceiver.Protocol
import au.id.micolous.metrodroid.card.CardTransceiver.Protocol.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.toImmutable
import java.io.IOException

fun <T>wrapAndroidExceptions(f: () -> T): T {
    try {
        return f()
    } catch (e: TagLostException) {
        throw CardLostException(Utils.getErrorMessage(e), e)
    } catch (e: IOException) {
        throw CardTransceiveException(Utils.getErrorMessage(e), e)
    }
}

/**
 * Wrapper for Android's [Tag] class, to implement the [CardTransceiver] interface.
 */
class AndroidCardTransceiver(private val tag: Tag) : CardTransceiver {
    private var protocol: Protocol? = null
    override var uid: ImmutableByteArray? = null
        private set

    private var isoDep: IsoDep? = null

    private var nfcA: NfcA? = null
    override var sak: Short? = null
        private set
    override var atqa: ImmutableByteArray? = null
        private set

    private var nfcF: NfcF? = null
    override var defaultSystemCode: Int? = null
        private set
    override var pmm: ImmutableByteArray? = null
        private set

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    override fun connect(protocol: Protocol) {
        close()

        when (protocol) {
            ISO_14443A -> {
                val isoDep = IsoDep.get(tag) ?:
                    throw CardProtocolUnsupportedException("iso14a not supported by this card")

                isoDep.connect()
                this.isoDep = isoDep
            }

            JIS_X_6319_4 -> {
                val nfcF = NfcF.get(tag) ?:
                    throw CardProtocolUnsupportedException("nfcF not supported by this card")

                nfcF.connect()
                this.nfcF = nfcF
                this.defaultSystemCode = nfcF.systemCode.toImmutable().byteArrayToInt()
                this.pmm = nfcF.manufacturer.toImmutable()
            }

            NFC_A -> {
                val nfcA = NfcA.get(tag) ?:
                    throw CardProtocolUnsupportedException("nfcA not supported by this card")

                nfcA.connect()
                this.sak = nfcA.sak
                this.atqa = nfcA.atqa.toImmutable()
                this.nfcA = nfcA
            }

            else -> {
                throw CardProtocolUnsupportedException("Protocol not supported on this platform")
            }
        }

        uid = tag.id.toImmutable()
        this.protocol = protocol
    }

    override fun close() {
        val isoDep = this.isoDep
        this.isoDep = null
        val nfcF = this.nfcF
        this.nfcF = null
        val nfcA = this.nfcA
        this.nfcA = null
        uid = null
        defaultSystemCode = null
        pmm = null

        // Android can declare IOExecption here but we really don't care.
        try {
            isoDep?.close()
        } catch (e: IOException) {
        }

        try {
            nfcA?.close()
        } catch (e: IOException) {
        }

        try {
            nfcF?.close()
        } catch (e: IOException) {
        }
    }

    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        val request = data.dataCopy
        val isoDep = this.isoDep
        val nfcA = this.nfcA
        val nfcF = this.nfcF

        return wrapAndroidExceptions {
            when {
                isoDep != null -> isoDep.transceive(request)
                nfcA != null -> nfcA.transceive(request)
                nfcF != null -> nfcF.transceive(request)
                else -> throw CardTransceiveException("Card not connected")
            }
        }.toImmutable()
    }
}
