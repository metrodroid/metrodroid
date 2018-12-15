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
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * SunCard cards.
 */

@Parcelize
data class SunCardTransitData (private val mSerial: Int = 0): SerialOnlyTransitData() {

    override fun getSerialNumber() = formatSerial(mSerial)

    override fun getReason() = SerialOnlyTransitData.Reason.NOT_STORED

    override fun getCardName() = NAME

    override fun getExtraInfo() = listOf(
            ListItem(R.string.full_serial_number, formatLongSerial(mSerial)),
            ListItem(R.string.full_serial_number, formatBarcodeSerial(mSerial)))

    private constructor(card: ClassicCard) : this(getSerial(card))

    companion object {
        private const val NAME = "SunRail SunCard"
        val CARD_INFO = CardInfo.Builder()
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

        private fun getSerial(card: ClassicCard) = Utils.byteArrayToInt(card.getSector(0)
                    .getBlock(1).data, 3, 4)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME,
                        formatSerial(getSerial(card)))

            override fun parseTransitData(classicCard: ClassicCard) = SunCardTransitData(classicCard)

            override fun earlyCheck(sectors: MutableList<ClassicSector>) = try {
                // I hope it is magic as other than zeros, ff's and serial there is nothing
                // on the card
                Utils.byteArrayToInt(sectors[0].getBlock(1).data, 7, 4) == 0x070515ff
            } catch (ignored: IndexOutOfBoundsException) {
                // If that sector number is too high, then it's not for us.
                // If we can't read we can't do anything
                false
            } catch (ignored: UnauthorizedException) {
                false
            }

            override fun earlySectors() = 1

            override fun getAllCards(): MutableList<CardInfo> = Collections.singletonList(CARD_INFO)
        }
    }
}
