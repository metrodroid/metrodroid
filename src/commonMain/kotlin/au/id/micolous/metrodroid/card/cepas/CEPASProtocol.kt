/*
 * CEPASProtocol.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.card.cepas

import au.id.micolous.metrodroid.card.iso7816.ISO7816Exception
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray

class CEPASProtocol(private val mTagTech: ISO7816Protocol) {

    fun getPurse(purseId: Int): ImmutableByteArray? {
        try {
            return mTagTech.sendRequest(ISO7816Protocol.CLASS_90,
                    0x32.toByte(), purseId.toByte(), 0.toByte(), 0.toByte()).ifEmpty { null }
        } catch (ex: ISO7816Exception) {
            Log.w(TAG, "Error reading purse $purseId", ex)
            return null
        }

    }

    fun getHistory(purseId: Int): ImmutableByteArray? {
        var historyBuff: ImmutableByteArray
        try {
            historyBuff = mTagTech.sendRequest(
                    ISO7816Protocol.CLASS_90,
                    0x32.toByte(), purseId.toByte(), 0.toByte(),
                    0.toByte(), ImmutableByteArray.of(0.toByte()))
        } catch (ex: ISO7816Exception) {
            Log.w(TAG, "Error reading purse history $purseId", ex)
            return null
        }

        try {
            val historyBuff2 = mTagTech.sendRequest(
                    ISO7816Protocol.CLASS_90,
                    0x32.toByte(), purseId.toByte(), 0.toByte(),
                    0.toByte(),
                    ImmutableByteArray.of((historyBuff.size / 16).toByte()))
            historyBuff += historyBuff2
        } catch (ex: ISO7816Exception) {
            Log.w(TAG, "Error reading 2nd purse history $purseId", ex)
        }

        return historyBuff
    }

    companion object {
        private const val TAG = "CEPASProtocol"
    }
}
