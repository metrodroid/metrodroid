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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Selecta payment cards
 *
 * Reference: https://dyrk.org/2015/09/03/faille-nfc-distributeur-selecta/
 */

@Parcelize
data class SelectaFranceTransitData(private var mBalance: Int = 0,
                                    private var mSerial: Int = 0) : TransitData() {

    override fun getSerialNumber() = mSerial.toString()

    override fun getCardName(): String = NAME

    constructor(card: ClassicCard) : this(
            mSerial = getSerial(card),
            mBalance = Utils.byteArrayToInt(card[1, 2].data, 0, 3))

    public override fun getBalance(): TransitBalance? = TransitCurrency.EUR(mBalance)

    companion object {
        private const val NAME = "Selecta France"

        private val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_france)
                .setCardType(CardType.MifareClassic)
                .setPreview()
                .build()

        private fun getSerial(card: ClassicCard): Int = Utils.byteArrayToInt(card[1, 0].data, 13, 3)

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun earlyCheck(sectors: List<ClassicSector>) = try {
                val toc = sectors[0][1].data
                // Check toc entries for sectors 10,12,13,14 and 15
                Utils.byteArrayToInt(toc, 2, 2) == 0x0938
            } catch (ignored: IndexOutOfBoundsException) {
                // If that sector number is too high, then it's not for us.
                // If we can't read we can't do anything
                false
            } catch (ignored: UnauthorizedException) {
                false
            }

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity = TransitIdentity(NAME, Integer.toString(getSerial(card)))

            override fun parseTransitData(classicCard: ClassicCard): TransitData = SelectaFranceTransitData(classicCard)

            override fun earlySectors() = 1

            override fun getAllCards(): MutableList<CardInfo> = Collections.singletonList(CARD_INFO)
        }
    }
}
