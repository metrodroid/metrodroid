/*
 * TartuTransitData.java
 *
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko
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
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import org.jetbrains.annotations.NonNls

/**
 * Transit data type for Tartu bus card.
 *
 *
 * This is a very limited implementation of reading TartuBus, because only
 * little data is stored on the card
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/TartuBus
 */
class TartuTransitFactory : ClassicCardTransitFactory {

    override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
        try {
            val sector0 = sectors[0]
            if (Utils.byteArrayToInt(sector0[1].data, 2, 4) != 0x03e103e1)
                return false
            val sector1 = sectors[1]
            if (!Utils.byteArraySlice(sector1[0].data, 7, 9)
                            .contentEquals(Utils.stringToByteArray("pilet.ee:")))
                return false
            if (!Utils.byteArraySlice(sector1[1].data, 0, 6)
                            .contentEquals(Utils.stringToByteArray("ekaart")))
                return false
            return true
        } catch (ignored: IndexOutOfBoundsException) {
            // If that sector number is too high, then it's not for us.
        } catch (ignored: UnauthorizedException) {
        }

        return false
    }

    override fun earlySectors() = 2

    override fun parseTransitIdentity(classicCard: ClassicCard) =
            TransitIdentity(NAME, parseSerial(classicCard).substring(8))

    override fun parseTransitData(classicCard: ClassicCard): TransitData =
            TartuTransitData(mSerial = parseSerial(classicCard))

    override fun getAllCards() = listOf(CARD_INFO)

    @Parcelize
    private data class TartuTransitData (private val mSerial: String): SerialOnlyTransitData() {
        override val extraInfo: List<ListItem>
            get() = listOf(ListItem(R.string.full_serial_number, mSerial))

        override val reason: SerialOnlyTransitData.Reason
            get() = SerialOnlyTransitData.Reason.NOT_STORED

        override fun getSerialNumber() = mSerial.substring(8)

        override fun getCardName() = NAME
    }

    companion object {
        private const val NAME = "Tartu Bus"

        private val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setCardType(CardType.MifareClassic)
                .setLocation(R.string.location_tartu)
                .setExtraNote(R.string.card_note_card_number_only)
                .build()

        @NonNls
        private fun parseSerial(card: ClassicCard) =
                String(Utils.byteArraySlice(card[2, 0].data, 7, 9), Utils.getASCII()) +
                        String(Utils.byteArraySlice(card[2, 1].data, 0, 10), Utils.getASCII())
    }
}
