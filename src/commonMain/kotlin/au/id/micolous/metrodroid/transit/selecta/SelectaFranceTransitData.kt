/*
 * SelectaFRanceTransitData.kt
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
package au.id.micolous.metrodroid.transit.selecta

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*

/**
 * Selecta payment cards
 *
 * Reference: https://dyrk.org/2015/09/03/faille-nfc-distributeur-selecta/
 */

@Parcelize
data class SelectaFranceTransitData(private val mBalance: Int,
                                    private val mSerial: Int) : TransitData() {

    override val serialNumber get() = mSerial.toString()

    override val cardName get(): String = NAME

    public override val balance get(): TransitBalance? = TransitCurrency.EUR(mBalance)

    companion object {
        private const val NAME = "Selecta France"

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_france,
                cardType = CardType.MifareClassic,
                preview = true)

        private fun getSerial(card: ClassicCard): Int = card[1, 0].data.byteArrayToInt(13, 3)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun earlyCheck(sectors: List<ClassicSector>) =
                    sectors[0][1].data.byteArrayToInt(2, 2) == 0x0938

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity = TransitIdentity(NAME, getSerial(card).toString())

            override fun parseTransitData(card: ClassicCard): TransitData =
                    SelectaFranceTransitData(mSerial = getSerial(card),
                            mBalance = card[1, 2].data.byteArrayToInt(0, 3))

            override val earlySectors get() = 1

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
