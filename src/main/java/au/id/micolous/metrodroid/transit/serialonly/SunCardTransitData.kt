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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * SunCard cards.
 */

@Parcelize
data class SunCardTransitData(private val mSerial: Int = 0) : SerialOnlyTransitData() {

    override fun getSerialNumber() = formatSerial(mSerial)

    override val reason
        get() = SerialOnlyTransitData.Reason.NOT_STORED

    override fun getCardName() = NAME

    override val extraInfo
        get () = listOf(
                ListItem(R.string.full_serial_number, formatLongSerial(mSerial)),
                ListItem(R.string.full_serial_number, formatBarcodeSerial(mSerial)))

    private constructor(card: ClassicCard) : this(getSerial(card))

    companion object {
        private const val NAME = "SunRail SunCard"
        private val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_orlando)
                .setCardType(CardType.MifareClassic)
                .setExtraNote(R.string.card_note_card_number_only)
                .setKeysRequired()
                .setPreview()
                .build()

        private fun formatSerial(serial: Int) = serial.toString()

        private fun formatLongSerial(serial: Int) = "637426%010d".format(Locale.ENGLISH, serial)

        private fun formatBarcodeSerial(serial: Int) = "799366314176000637426%010d".format(Locale.ENGLISH, serial)

        private fun getSerial(card: ClassicCard) = card[0, 1].data.byteArrayToInt(3, 4)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME,
                    formatSerial(getSerial(card)))

            override fun parseTransitData(classicCard: ClassicCard) = SunCardTransitData(classicCard)

            override fun earlyCheck(sectors: List<ClassicSector>) =
                // I hope it is magic as other than zeros, ff's and serial there is nothing
                // on the card
                    sectors[0][1].data.byteArrayToInt(7, 4) == 0x070515ff

            override fun earlySectors() = 1

            override fun getAllCards() = listOf(CARD_INFO)
        }
    }
}
