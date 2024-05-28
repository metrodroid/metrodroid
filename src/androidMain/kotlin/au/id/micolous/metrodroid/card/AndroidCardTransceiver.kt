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
import android.nfc.tech.*
import au.id.micolous.metrodroid.card.felica.FelicaTransceiver
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.getErrorMessage
import au.id.micolous.metrodroid.util.toImmutable
import java.io.Closeable
import java.io.IOException

fun <T>wrapAndroidExceptions(f: () -> T): T {
    try {
        return f()
    } catch (e: TagLostException) {
        throw CardLostException(getErrorMessage(e), e)
    } catch (e: IOException) {
        throw CardTransceiveException(getErrorMessage(e), e)
    }
}

abstract class AndroidCardTransceiver<T: TagTechnology>
    (protected val tag: Tag, private val opener: (Tag) -> T?): CardTransceiver, Closeable {
    final override var uid: ImmutableByteArray? = null
        private set

    protected var protocol: T? = null

    fun connect() {
        close()

        try {
            val prot = opener(tag) ?: throw CardProtocolUnsupportedException("FeliCa")

            prot.connect()
            this.protocol = prot
            this.uid = tag.id.toImmutable()
            postConnect()
        } catch (e: Exception) {
            close()
        }
    }

    override fun reconnect() {
        connect()
    }

    override fun close() {
        val prot = this.protocol
        this.protocol = null
        try {
            if (prot?.isConnected == true) {
                prot.close()
            }
        } catch (e: IOException) {
        }
    }

    open fun postConnect() {}
}


/**
 * Wrapper for Android's [Tag] class, to implement the [CardTransceiver] interface.
 */
class AndroidFelicaTransceiver(tag: Tag) : FelicaTransceiver,
    AndroidCardTransceiver<NfcF>(tag, NfcF::get) {
    override var defaultSystemCode: Int? = null
        private set
    override var pmm: ImmutableByteArray? = null
        private set

    override fun postConnect() {
        this.defaultSystemCode = protocol?.systemCode?.toImmutable()?.byteArrayToInt()
        this.pmm = protocol?.manufacturer?.toImmutable()
    }

    override fun transceive(data: ImmutableByteArray): ImmutableByteArray = wrapAndroidExceptions {
        protocol!!.transceive(data.dataCopy)
    }.toImmutable()
}

/**
 * Wrapper for Android's [Tag] class, to implement the [CardTransceiver] interface.
 */
class AndroidNfcVTransceiver(tag: Tag) : AndroidCardTransceiver<NfcV>(tag, NfcV::get) {
    override fun transceive(data: ImmutableByteArray): ImmutableByteArray =
            wrapAndroidExceptions { protocol!!.transceive(data.dataCopy) }.toImmutable()
}

class AndroidIsoTransceiver(tag: Tag) : AndroidCardTransceiver<IsoDep>(tag, IsoDep::get) {
    override fun transceive(data: ImmutableByteArray): ImmutableByteArray =
            wrapAndroidExceptions { protocol!!.transceive(data.dataCopy)
        }.toImmutable()
}


class AndroidNfcATransceiver(tag: Tag) : AndroidCardTransceiver<NfcA>(tag, NfcA::get) {
    override fun transceive(data: ImmutableByteArray): ImmutableByteArray = wrapAndroidExceptions {
            protocol!!.transceive(data.dataCopy)
        }.toImmutable()
}
