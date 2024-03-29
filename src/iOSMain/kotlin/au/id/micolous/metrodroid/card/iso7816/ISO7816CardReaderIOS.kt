/*
 * ISO7816CardReaderIOS.kt
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

package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardReaderIOS
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Log
import platform.CoreNFC.NFCISO7816TagProtocol

object ISO7816CardReaderIOS : CardReaderIOS<NFCISO7816TagProtocol> {
    override fun dump(tag: NFCISO7816TagProtocol,
             feedback: TagReaderFeedbackInterface): Card {
        val xfer = ISO7816Transceiver(tag)
        Log.d(TAG, "Start dump ${xfer.uid}")
        val df = ISO7816Card.dumpTag(xfer, feedback, coreNFC = true)
        return Card(
            tagId = xfer.uid?.let { if (it.size == 10) it.sliceOffLen(0, 7) else it }!!,
            scannedAt = TimestampFull.now(), iso7816 = df
        )
    }
    
    private const val TAG = "ISO7816CardReaderIOS"
}
