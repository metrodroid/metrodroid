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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

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
class TartuTransitFactory : ClassicCardTransitFactory() {

    override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
        val sector0 = sectors[0]
        if (sector0[1].data.byteArrayToInt(2, 4) != 0x03e103e1)
            return false
        val sector1 = sectors[1]
        if (!sector1[0].data.sliceOffLen(7, 9)
                        .contentEquals(ImmutableByteArray.fromASCII("pilet.ee:")))
            return false
        if (!sector1[1].data.sliceOffLen(0, 6)
                        .contentEquals(ImmutableByteArray.fromASCII("ekaart")))
            return false
        return true
    }

    override val earlySectors get() = 2

    override fun parseTransitIdentity(card: ClassicCard) =
            TransitIdentity(NAME, parseSerial(card).substring(8))

    override fun parseTransitData(card: ClassicCard): TransitData =
            TartuTransitData(mSerial = parseSerial(card))

    override val allCards get() = listOf(CARD_INFO)

    @Parcelize
    private data class TartuTransitData (private val mSerial: String): SerialOnlyTransitData() {
        override val extraInfo: List<ListItem>
            get() = listOf(ListItem(R.string.full_serial_number, mSerial))

        override val reason: SerialOnlyTransitData.Reason
            get() = SerialOnlyTransitData.Reason.NOT_STORED

        override val serialNumber get() = mSerial.substring(8)

        override val cardName get() = NAME
    }

    companion object {
        private const val NAME = "Tartu Bus"

        private val CARD_INFO = CardInfo(
                name = NAME,
                cardType = CardType.MifareClassic,
                locationId = R.string.location_tartu,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun parseSerial(card: ClassicCard) =
                card[2, 0].data.sliceOffLen(7, 9).readASCII() +
                        card[2, 1].data.sliceOffLen(0, 10).readASCII()
    }
}
