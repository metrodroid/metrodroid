/*
 * BlankUltralightTransitData.kt
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

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Handle MIFARE Ultralight with no non-default data
 */
@Suppress("PLUGIN_WARNING")
@Parcelize
class BlankUltralightTransitData : BlankTransitData() {
    override val cardName: String
        get() = Localizer.localizeString(R.string.blank_mfu_card)

    companion object {

        val FACTORY: UltralightCardTransitFactory = object : UltralightCardTransitFactory {
            /**
             * @param card Card to read.
             * @return true if all sectors on the card are blank.
             */
            override fun check(card: UltralightCard): Boolean {
                val pages = card.pages
                val model = card.cardModel

                // check to see if all sectors are blocked
                for ((idx, p) in pages.withIndex()) {
                    // Page 2 is serial, internal and lock bytes
                    // Page 3 is OTP counters
                    // User memory is page 4 and above
                    if (idx <= 2) {
                        continue
                    }
                    if (p.isUnauthorized) {
                        // At least one page is "closed", this is not for us
                        return false
                    }
                    val data = p.data
                    if (idx == 0x2) {
                        if (data[2].toInt() != 0 || data[3].toInt() != 0)
                            return false
                        continue
                    }

                    if (model.startsWith("NTAG21")) {
                        // Factory-set data on NTAG
                        if (model == "NTAG213") {
                            if (idx == 0x03 && data.contentEquals(byteArrayOf(0xE1.toByte(), 0x10, 0x12, 0)))
                                continue
                            if (idx == 0x04 && data.contentEquals(byteArrayOf(0x01, 0x03, 0xA0.toByte(), 0x0C)))
                                continue
                            if (idx == 0x05 && data.contentEquals(byteArrayOf(0x34, 0x03, 0, 0xFE.toByte())))
                                continue
                        }

                        if (model == "NTAG215") {
                            if (idx == 0x03 && data.contentEquals(byteArrayOf(0xE1.toByte(), 0x10, 0x3E, 0)))
                                continue
                            if (idx == 0x04 && data.contentEquals(byteArrayOf(0x03, 0, 0xFE.toByte(), 0)))
                                continue
                            // Page 5 is all null
                        }

                        if (model == "NTAG215") {
                            if (idx == 0x03 && data.contentEquals(byteArrayOf(0xE1.toByte(), 0x10, 0x6D, 0)))
                                continue
                            if (idx == 0x04 && data.contentEquals(byteArrayOf(0x03, 0, 0xFE.toByte(), 0)))
                                continue
                            // Page 5 is all null
                        }

                        // Ignore configuration pages
                        if (idx == pages.size - 5) {
                            // LOCK BYTE / RFUI
                            // Only care about first three bytes
                            if (data.sliceOffLen(0, 3).contentEquals(byteArrayOf(0, 0, 0)))
                                continue
                        }

                        if (idx == pages.size - 4) {
                            // MIRROR / RFUI / MIRROR_PAGE / AUTH0
                            // STRG_MOD_EN = 1
                            // AUTHO = 0xff
                            if (data.contentEquals(byteArrayOf(4, 0, 0, 0xFF.toByte())))
                                continue
                        }

                        if (idx == pages.size - 3) {
                            // ACCESS / RFUI
                            // Only care about first byte
                            if (data[0].toInt() == 0)
                                continue
                        }

                        if (idx == pages.size - 2) {
                            // PWD (always masked)
                            // PACK / RFUI
                            continue
                        }
                    } else {
                        // page 0x10 and 0x11 on 384-bit card are config
                        if (pages.size == 0x14) {
                            if (idx == 0x10 && data.contentEquals(byteArrayOf(0, 0, 0, -1)))
                                continue
                            if (idx == 0x11 && data.contentEquals(byteArrayOf(0, 5, 0, 0)))
                                continue
                        }
                    }

                    if (!data.contentEquals(byteArrayOf(0, 0, 0, 0))) {
                        return false
                    }
                }
                return true
            }

            override fun parseTransitData(card: UltralightCard) = BlankUltralightTransitData()

            override fun parseTransitIdentity(card: UltralightCard) =
                    TransitIdentity(Localizer.localizeString(R.string.blank_mfu_card), null)

            override val allCards get() = emptyList<CardInfo>() // Not worth bragging about
        }
    }
}
