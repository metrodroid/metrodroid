/*
 * UltralightTransceiverIOS.kt
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

package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.card.CardTransceiverIOSPlain
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import au.id.micolous.metrodroid.util.toNSData
import platform.CoreNFC.NFCMiFareTagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError

class UltralightTransceiverIOS(val tag: NFCMiFareTagProtocol): CardTransceiverIOSPlain() {
    override val uid: ImmutableByteArray? = tag.identifier.toImmutable()

    override fun send(dt: ImmutableByteArray, cb: (NSData?, NSError?) -> Unit) {
        tag.sendMiFareCommand(dt.toNSData(), cb)
    }
}
