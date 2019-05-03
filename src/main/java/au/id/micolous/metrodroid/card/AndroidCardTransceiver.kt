/*
 * AndroidCardTransceiver.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import android.nfc.TagLostException

import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.toImmutable
import java.io.IOException

fun <T>wrapAndroidExceptions(f: () -> T): T {
    try {
        return f()
    } catch (e: TagLostException) {
        throw CardLostException(Utils.getErrorMessage(e))
    } catch (e: IOException) {
        throw CardTransceiveException(e, Utils.getErrorMessage(e))
    }
}

/**
 * Wrapper for Android to implement the [CardTransceiver] interface.
 */
class AndroidCardTransceiver(private val transceive: (ByteArray) -> ByteArray) : CardTransceiver {
    override suspend fun transceive(data: ImmutableByteArray): ImmutableByteArray = wrapAndroidExceptions {
        transceive(data.dataCopy).toImmutable()
    }
}
