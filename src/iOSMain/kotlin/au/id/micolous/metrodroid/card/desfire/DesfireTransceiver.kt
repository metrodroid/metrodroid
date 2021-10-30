/*
 * ISO7816Transceiver.kt
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

package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import au.id.micolous.metrodroid.util.toNSData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import platform.CoreNFC.NFCISO7816APDU
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlin.native.concurrent.freeze

class DesfireTransceiver(val tag: NFCMiFareTagProtocol): CardTransceiver {
    override val uid: ImmutableByteArray? = tag.identifier.toImmutable()

    init {
       freeze()
    }

    // Kotlin-objc interface behaves weirdly if we pass it directly,
    // packing as data class is nicer and works better
    data class Capsule (
        val rep: NSData?,
        val sw1: UByte,
        val sw2: UByte,
        val err: NSError?
    ) {
        init {
            freeze()
        }
    }

    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        Log.d(TAG, ">>> $data")
        val (rep, sw1, sw2, err) = runBlocking {
            val chan = Channel<Capsule>()
            chan.freeze()
            val lambda = { data: NSData?, sw1: UByte, sw2: UByte, err: NSError? ->
                    callback(channel = chan, reply = data, sw1 = sw1, sw2 = sw2, error = err)
            }
            lambda.freeze()
            tag.sendMiFareISO7816Command(NFCISO7816APDU(data = data.toNSData()),
                    completionHandler = lambda)
            chan.receive()
        }
        if (err != null || rep == null) {
            Log.d(TAG, "<!< $err")
            if (err?.code == 100L)
              throw CardLostException(err.toString())
            throw CardTransceiveException(err.toString())
        } else {
            val repl = rep.toImmutable() + ImmutableByteArray.of(sw1.toByte(), sw2.toByte())
            Log.d(TAG, "<<< $repl")
            return repl
        }
    }

    companion object {
        private const val TAG = "DesfireTransceiver"

        fun callback(channel: SendChannel<Capsule>, reply: NSData?,
                     sw1: UByte, sw2: UByte, error: NSError?) {
            val capsule = Capsule(reply, sw1, sw2, error)
            runBlocking {
                channel.send(capsule)
                channel.close()
            }
        }
    }
}
