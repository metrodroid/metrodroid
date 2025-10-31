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

import au.id.micolous.metrodroid.card.CardTransceiverIOSPlain
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import au.id.micolous.metrodroid.util.toNSData
import platform.CoreNFC.NFCFeliCaTagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError

class FelicaTransceiverIOS(val tag: NFCFeliCaTagProtocol): FelicaTransceiver, CardTransceiverIOSPlain() {
    override val uid: ImmutableByteArray? = tag.currentIDm.toImmutable()
    override val defaultSystemCode : Int? = tag.currentSystemCode.toImmutable().byteArrayToInt()

    override fun send(dt: ImmutableByteArray, cb: (NSData?, NSError?) -> Unit) {
        tag.sendFeliCaCommandPacket(
            commandPacket = dt.drop(1).toNSData(),
            completionHandler = cb)
    }
}
