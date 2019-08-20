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
import android.nfc.tech.NfcV
import au.id.micolous.metrodroid.card.felica.FelicaTransceiver
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.toImmutable
import kotlinx.io.core.Closeable
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
class AndroidFelicaTransceiver(private val tag: Tag) : FelicaTransceiver, Closeable {
    override var uid: ImmutableByteArray? = null
        private set

    private var nfcF: NfcF? = null
    override var defaultSystemCode: Int? = null
        private set
    override var pmm: ImmutableByteArray? = null
        private set

    fun connect() {
        close()

        val nfcF = NfcF.get(tag) ?: throw CardProtocolUnsupportedException("FeliCa")

        nfcF.connect()
        this.nfcF = nfcF
        this.defaultSystemCode = nfcF.systemCode.toImmutable().byteArrayToInt()
        this.pmm = nfcF.manufacturer.toImmutable()
        this.uid = tag.id.toImmutable()
    }

    override fun close() {
        val nfcF = this.nfcF
        this.nfcF = null
        try {
            nfcF?.close()
        } catch (e: IOException) {
        }
    }

    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray = wrapAndroidExceptions {
            nfcF!!.transceive(data.dataCopy)
        }.toImmutable()
}

/**
 * Wrapper for Android's [Tag] class, to implement the [CardTransceiver] interface.
 */
class AndroidNfcVTransceiver(private val tag: Tag) : CardTransceiver, Closeable {
    override var uid: ImmutableByteArray? = null
        private set

    private var nfcV: NfcV? = null

    fun connect() {
        close()

        val nfcV = NfcV.get(tag) ?: throw CardProtocolUnsupportedException("ISO15693")

        nfcV.connect()
        this.nfcV = nfcV
        uid = tag.id.toImmutable()
    }

    override fun close() {
        val nfcV = this.nfcV
        this.nfcV = null
        uid = null

        try {
            nfcV?.close()
        } catch (e: IOException) {
        }
    }

    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray =
            wrapAndroidExceptions { nfcV!!.transceive(data.dataCopy) }.toImmutable()
}

class AndroidIsoTransceiver(private val tag: Tag) : CardTransceiver, Closeable {
    override var uid: ImmutableByteArray? = null
        private set

    private var isoDep: IsoDep? = null

    fun connect() {
        close()

        val isoDep = IsoDep.get(tag) ?: throw CardProtocolUnsupportedException("ISO14443")

        isoDep.connect()
        this.isoDep = isoDep

        uid = tag.id.toImmutable()
    }

    override fun close() {
        val isoDep = this.isoDep
        this.isoDep = null


        // Android can declare IOExecption here but we really don't care.
        try {
            isoDep?.close()
        } catch (e: IOException) {
        }
    }

    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray =
            wrapAndroidExceptions { isoDep!!.transceive(data.dataCopy)
        }.toImmutable()
}


class AndroidNfcATransceiver(private val tag: Tag) : CardTransceiver, Closeable {
    override var uid: ImmutableByteArray? = null
        private set

    private var nfcA: NfcA? = null

    fun connect() {
        close()

        val nfcA = NfcA.get(tag) ?: throw CardProtocolUnsupportedException("ISO14443-A")

        nfcA.connect()
        this.nfcA = nfcA

        uid = tag.id.toImmutable()
    }

    override fun close() {
        val nfcA = this.nfcA
        this.nfcA = null
        uid = null

        try {
            nfcA?.close()
        } catch (e: IOException) {
        }
    }

    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray = wrapAndroidExceptions {
            nfcA!!.transceive(data.dataCopy)
        }.toImmutable()
}
