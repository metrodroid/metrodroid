/*
 * SunCardTransitData.kt
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

package au.id.micolous.metrodroid.transit.serialonly

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * SunCard cards.
 */

@Parcelize
data class SunCardTransitData(private val mSerial: Int = 0) : SerialOnlyTransitData() {

    override val serialNumber get() = formatSerial(mSerial)

    override val reason
        get() = SerialOnlyTransitData.Reason.NOT_STORED

    override val cardName get() = NAME

    override val extraInfo
        get () = listOf(
                ListItem(R.string.full_serial_number, formatLongSerial(mSerial)),
                ListItem(R.string.full_serial_number, formatBarcodeSerial(mSerial)))

    private constructor(card: ClassicCard) : this(getSerial(card))

    companion object {
        private const val NAME = "SunRail SunCard"
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_orlando,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_card_number_only,
                keysRequired = true,
                preview = true)

        private fun formatSerial(serial: Int) = serial.toString()

        private fun formatLongSerial(serial: Int) = "637426" + NumberUtils.zeroPad(serial, 10)

        private fun formatBarcodeSerial(serial: Int) = "799366314176000637426" + NumberUtils.zeroPad(serial, 10)

        private fun getSerial(card: ClassicCard) = card[0, 1].data.byteArrayToInt(3, 4)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME,
                    formatSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard) = SunCardTransitData(card)

            override fun earlyCheck(sectors: List<ClassicSector>) =
                // I hope it is magic as other than zeros, ff's and serial there is nothing
                // on the card
                    sectors[0][1].data.byteArrayToInt(7, 4) == 0x070515ff

            override val earlySectors get() = 1

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
