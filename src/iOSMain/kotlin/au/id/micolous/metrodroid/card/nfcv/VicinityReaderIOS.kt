//
// VicinityReaderIOS.kt
//
// Copyright 2019,2021 Google
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

package au.id.micolous.metrodroid.card.nfcv

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardReaderIOS
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import platform.CoreNFC.NFCISO15693TagProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError


object VicinityReaderIOS : CardReaderIOS<NFCISO15693TagProtocol> {
    data class Capsule(val contents: ImmutableByteArray?, val reachedEnd: Boolean, val partialRead: Boolean)

    override fun dump(tag: NFCISO15693TagProtocol, feedback: TagReaderFeedbackInterface): Card {
        val sectors = mutableListOf<ImmutableByteArray?>()
        var partialRead = false
        for (sectorIdx in 0..255) {
            val chan = Channel<Capsule>()
            val lambda = { data: NSData?, errorIn: NSError? ->
                print("Read $sectorIdx -> $data, $errorIn")
                runBlocking {
                    when (errorIn?.code) {
                        102.toLong() -> {
                            chan.send(Capsule(null, reachedEnd = true, partialRead = false))
                        }
                        100.toLong() -> {
                            chan.send(Capsule(null, reachedEnd = false, partialRead = true))
                        }
                        else -> {
                            chan.send(Capsule(data?.toImmutable(), reachedEnd = false, partialRead = false))
                        }
                    }
                }
            }
            tag.readSingleBlockWithRequestFlags(0x22u,
                    blockNumber = sectorIdx.toUByte(),
                    completionHandler = lambda)
            val cap = runBlocking {
                chan.receive()
            }
            if (cap.reachedEnd || cap.partialRead) {
                partialRead = cap.partialRead
                break
            }
            sectors += cap.contents
        }
        val nfcv = NFCVCard(sysInfo = null,
                pages = sectors.map {
                    NFCVPage(dataRaw = it ?: ImmutableByteArray.empty(),
                            isUnauthorized = it == null || it.isEmpty())
                }, isPartialRead = partialRead)
        return Card(tagId = tag.identifier.toImmutable().reverseBuffer(), scannedAt = TimestampFull.now(), vicinity = nfcv)
    }
}
