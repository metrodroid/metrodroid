/*
 * FelicaTransceiverIOS.kt
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

package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toNSData
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlin.coroutines.suspendCoroutine

class FelicaTransceiverIOS(val tag: SwiftWrapper, defaultSysCode: NSData): FelicaTransceiver {
    override val uid: ImmutableByteArray? = tag.getIdentifier().toImmutable()
    override val defaultSystemCode : Int? = defaultSysCode.toImmutable().byteArrayToInt()

    data class Capsule (
        val reply: NSData,
        val err: NSError?)

    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        val (repl, err) = suspendCoroutine<Capsule> { cont ->
            Log.d(TAG, ">>> $data")
            // iOS adds the length byte itself
            tag.transmit(data.sliceOffLen(1, data.size - 1).toNSData()) { cap -> cont.resumeWith(Result.success(cap)) }
        }
        if (err != null) {
            Log.d(TAG, "<!< $err")
            if (err.code == 100L)
              throw CardLostException(err.toString())
            throw CardTransceiveException(err.toString())
        } else {
            val rep = repl.toImmutable()
            Log.d(TAG, "<<< $rep")
            return rep
        }
    }

    // Kotlin apparently lack imports for NFCTagMiFare
    interface SwiftWrapper {
        fun getIdentifier(): NSData
        fun transmit(input: NSData, callback: (Capsule) -> Unit)
    }

    companion object {
        private const val TAG = "FelicaTransceiver"
    }
}
