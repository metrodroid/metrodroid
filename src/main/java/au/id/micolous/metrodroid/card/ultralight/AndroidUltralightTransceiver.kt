/*
 * AndroidUltralightTransceiver.kt
 *
 * Copyright 2019 Google
 *
 * Octopus reading code based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General private License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General private License for more details.
 *
 * You should have received a copy of the GNU General private License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.card.ultralight

import android.nfc.tech.MifareUltralight
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.util.Utils
import java.io.IOException

class AndroidUltralightTransceiver(val tech: MifareUltralight) : UltralightTransceiver {
    override fun readPages(pageNumber: Int): ByteArray {
        try {
            return tech.readPages(pageNumber)
        } catch (e: IOException) {
            throw CardTransceiveException(e, Utils.getErrorMessage(e))
        }
    }

    override fun transceive(data: ByteArray): ByteArray {
        try {
            return tech.transceive(data)
        } catch (e: IOException) {
            throw CardTransceiveException(e, Utils.getErrorMessage(e))
        }
    }

    override fun reconnect() {
        tech.close()
        tech.connect()
    }
}
