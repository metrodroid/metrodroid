/*
 * JavaCardTransceiver.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.kotlin.pcsc.*
import au.id.micolous.kotlin.pcsc.Card
import au.id.micolous.metrodroid.card.felica.FelicaTransceiver
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.getErrorMessage
import au.id.micolous.metrodroid.util.toImmutable
import kotlinx.coroutines.runBlocking
import java.io.Closeable

fun <T> wrapJavaExceptions(f: () -> T): T {
    try {
        return f()
    } catch (e: PCSCError) {
        throw CardTransceiveException(getErrorMessage(e), e)
    }
}

/**
 * Implements a wrapper for kotlin-pcsc API to [CardTransceiver].
 */
open class JavaCardTransceiver(
    private val context: Context,
    private val terminal: String,
    private val printTrace: Boolean = false,
    // TODO: fix bugs that necessitate this
    private val skipGetUID: Boolean = false
) : CardTransceiver, Closeable {

    final override var uid: ImmutableByteArray? = null
        private set

    private var card : Card? = null
    var atr: Atr? = null
        private set
    var cardType: CardType = CardType.Unknown

    fun connect() {
        close()

        val card = wrapJavaExceptions {
            // TODO: protocol
            context.connect(terminal, ShareMode.Shared, Protocol.Any)
        }

        this.card = card
        val status = card.status()
        val atr = Atr.parseAtr(status.atr.toImmutable())

        val pcscAtr = atr?.pcscAtr

        cardType = if (pcscAtr != null) {
            // println("ATR standard: ${pcscAtr.standard}")
            when (pcscAtr.standard) {
                PCSCAtr.Standard.FELICA -> CardType.FeliCa

                PCSCAtr.Standard.ISO_15693_PART_1,
                PCSCAtr.Standard.ISO_15693_PART_2,
                PCSCAtr.Standard.ISO_15693_PART_3,
                PCSCAtr.Standard.ISO_15693_PART_4 -> CardType.Vicinity

                PCSCAtr.Standard.ISO_14443A_PART_1,
                PCSCAtr.Standard.ISO_14443A_PART_2,
                PCSCAtr.Standard.ISO_14443A_PART_3 -> CardType.ISO7816


                else -> CardType.Unknown
            }
        } else {
            CardType.ISO7816
        }

        if (skipGetUID) {
            this.uid = ImmutableByteArray.empty()
        } else {
            try {
                // GET DATA -> UID
                // PC/SC Part 3 section 3.2.2.1.3
                val ret = runBlocking { transceive(ImmutableByteArray.fromHex("ffca000000")) }
                this.uid = ret.sliceOffLenSafe(0, ret.count() - 2)
            } catch (e: CardTransceiveException) {
                // Contact cards don't support this
                this.uid = ImmutableByteArray.empty()
            }
        }

        this.atr = atr
    }

    override fun close() {
        val card = this.card
        this.card = null
        this.uid = null

        card?.disconnect(DisconnectDisposition.Reset)
    }

    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        if (printTrace) println(">>> ${data.getHexString()}")

        val r = wrapJavaExceptions {
            card!!.transmit(data.dataCopy)
        }.toImmutable()

        if (printTrace) println("<<< ${r.getHexString()}")
        return r
    }
}

class JavaFeliCaTransceiver private constructor(
    private val transceiver: JavaCardTransceiver) : FelicaTransceiver {
    override val uid: ImmutableByteArray?
        get() = transceiver.uid

    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        requireFeliCa()

        // Wrap in a pseudo APDU
        val dataExchange = ImmutableByteArray.fromHex("d44001")

        val p = ImmutableByteArray.fromHex("ff000000") +
            (dataExchange.count() + data.count()).toByte() +
            dataExchange + data

        val ret = transceiver.transceive(p)

        if (!ret.startsWith(ImmutableByteArray.fromHex("d541"))) {
            // Malformed protocol response
            throw CardTransceiveException("Unexpected response: ${ret.getHexString()}")
        }

        // TODO: PN532/ACR122-specific code
        return when (ret[2].toInt() and 0xff) {
            // Error codes are listed in UM0701-02 s7.1 (Error handling)
            0 -> ret.sliceOffLen(3, ret.count() - 5)

            // Timeout
            1 -> throw CardLostException("timeout")

            // TODO: Handle this better.
            else -> throw CardTransceiveException("PN532 returned error: ${ret.getHexString()}")
        }
    }

    override val defaultSystemCode: Int? = null // TODO

    private fun requireFeliCa() {
        require(transceiver.cardType == CardType.FeliCa) { "cardType != FeliCa" }
        val uid = transceiver.uid
        require(uid != null) { "IDm (uid) must be set for FeliCa"}
        require(uid.count() == 8) {
            "IDm (uid) must be 8 bytes for FeliCa, got: [${uid.getHexString()}]"}
    }

    companion object {
        fun wrap(transceiver: JavaCardTransceiver): JavaFeliCaTransceiver {
            val v = JavaFeliCaTransceiver(transceiver)
            v.requireFeliCa()
            return v
        }
    }
}
