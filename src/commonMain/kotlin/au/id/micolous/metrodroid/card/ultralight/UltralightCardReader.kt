/*
 * UltralightCardReader.kt
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

import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable

object UltralightCardReader {
    private const val TAG = "UltralightCardReader"

    fun dumpTag(tech: UltralightTransceiver, feedbackInterface: TagReaderFeedbackInterface): UltralightCard? {
        feedbackInterface.updateProgressBar(0, 1)
        feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfu_detect))

        val p = UltralightProtocol(tech)
        val t = p.getCardType()

        if (t.pageCount <= 0) {
            return null
        }

        feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfu_reading))
        feedbackInterface.showCardType(null)

        // Now iterate through the pages and grab all the datas
        var pageNumber = 0
        var pageBuffer = ImmutableByteArray(0)
        val pages = mutableListOf<UltralightPage>()
        var unauthorized = false
        while (pageNumber < t.pageCount) {
            if (pageNumber % 4 == 0) {
                feedbackInterface.updateProgressBar(pageNumber, t.pageCount)
                // Lets make a new buffer of data. (16 bytes = 4 pages * 4 bytes)
                try {
                    pageBuffer = tech.readPages(pageNumber)
                    unauthorized = false
                } catch (e: CardTransceiveException) {
                    // Transceive failure, maybe authentication problem
                    unauthorized = true
                    Log.d(TAG, "Unable to read page $pageNumber", e)
                }

            }

            // Now lets stuff this into some pages.
            if (!unauthorized) {
                pages.add(UltralightPage(
                        pageBuffer.sliceOffLen(
                        pageNumber % 4 * UltralightCard.PAGE_SIZE,
                        UltralightCard.PAGE_SIZE)))
            } else {
                pages.add(UltralightPage.unauthorized())
            }
            pageNumber++
        }

        // Now we have pages to stuff in the card.
        return UltralightCard(cardModel = t.toString(), pages = pages, isPartialRead = false)
    }
}
