/*
 * UltralightCardReaderA.kt
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

package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

object UltralightCardReaderA {
    suspend fun dumpTagA(tech: CardTransceiver,
                        feedbackInterface: TagReaderFeedbackInterface): UltralightCard? {
        feedbackInterface.updateProgressBar(0, 1)

        feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfu_reading))
        feedbackInterface.showCardType(null)

        // Now iterate through the pages and grab all the datas
        var pageBuffer = ImmutableByteArray.empty()
        while (true) {
            // Find first unread page
            val page = pageBuffer.size / 4
            if (page >= 0x100)
                break
            // read command
            val rd = ImmutableByteArray.ofB(0x30, page)
            val res: ImmutableByteArray
            try {
                res = tech.transceive(rd)
            } catch (e: Exception) {
                break
            }
            pageBuffer += res
            feedbackInterface.updateProgressBar(pageBuffer.size, 1024)
        }

        val numPages = (pageBuffer.size / 4)
        val pages = (0 until numPages).map {i ->
            UltralightPage(pageBuffer.sliceOffLen(i * 4, 4))
        }

        // Now we have pages to stuff in the card.
        return UltralightCard("", pages, isPartialRead = false)
    }
}