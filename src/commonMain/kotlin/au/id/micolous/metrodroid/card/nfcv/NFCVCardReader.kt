/*
 * NFCVCardReader.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.nfcv

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

object NFCVCardReader {
    private const val MAX_PAGES = 255
    fun dumpTag(
        tech: CardTransceiver,
        feedbackInterface: TagReaderFeedbackInterface
    ): NFCVCard {
        feedbackInterface.updateProgressBar(0, MAX_PAGES)

        feedbackInterface.updateStatusText(Localizer.localizeString(R.string.vicinity_reading))
        feedbackInterface.showCardType(null)

        val sysInfoRsp = try {
            tech.transceive(ImmutableByteArray.of(0x22, 0x2b) + tech.uid!!)
        } catch (e: Exception) {
            null
        }

        val sysInfo = sysInfoRsp?.sliceOffLen(1, sysInfoRsp.lastIndex)
        var isPartialRead = false

        // Now iterate through the pages and grab all the data
        val pages = mutableListOf<NFCVPage>()
        var page = 0
        while (true) {
            // Find first unread page
            if (page >= MAX_PAGES)
                break
            // read command
            val rd = ImmutableByteArray.of(0x22, 0x20) + tech.uid!! + ImmutableByteArray.ofB(page)
            val res: ImmutableByteArray
            try {
                res = tech.transceive(rd)
            } catch (e: CardLostException) {
                isPartialRead = true
                break
            } catch (e: Exception) {
                break
            }
            if (res.isEmpty() || res[0] != 0.toByte())
                break
            pages += NFCVPage(res.sliceOffLen(1, res.lastIndex))
            page++
            feedbackInterface.updateProgressBar(page, 256)
        }

        // Now we have pages to stuff in the card.
        return NFCVCard(sysInfo, pages, isPartialRead = isPartialRead)
    }
}