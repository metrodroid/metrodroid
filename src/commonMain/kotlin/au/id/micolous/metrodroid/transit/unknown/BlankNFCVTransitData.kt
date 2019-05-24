/*
 * BlankNFCVTransitData.java
 *
 * Copyright 2018 Google
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
package au.id.micolous.metrodroid.transit.unknown

import au.id.micolous.metrodroid.card.nfcv.NFCVCard
import au.id.micolous.metrodroid.card.nfcv.NFCVCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Handle NFCV with no non-default data
 */
@Suppress("PLUGIN_WARNING")
@Parcelize
class BlankNFCVTransitData : BlankTransitData() {
    override val cardName: String
        get() = Localizer.localizeString(R.string.blank_mfu_card)

    companion object {
        val FACTORY: NFCVCardTransitFactory = object : NFCVCardTransitFactory {
            /**
             * @param card Card to read.
             * @return true if all sectors on the card are blank.
             */
            override fun check(card: NFCVCard): Boolean {
                val pages = card.pages

                return pages.isNotEmpty() && pages.all { it.data.isAllZero() }
            }

            override fun parseTransitData(card: NFCVCard) = BlankNFCVTransitData()

            override fun parseTransitIdentity(card: NFCVCard) =
                    TransitIdentity(Localizer.localizeString(R.string.blank_nfcv_card), null)
        }
    }
}
