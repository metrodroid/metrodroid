/*
 * MagnaCartaTransitData.kt
 *
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
package au.id.micolous.metrodroid.transit.magnacarta

import au.id.micolous.metrodroid.transit.*

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
class MagnaCartaTransitData (
        private val mBalance: Int? // cents
): TransitData() {
    override val serialNumber: String?
        get() = null

    override val cardName: String
        get() = NAME

    override val balance
        get() = mBalance?.let { TransitCurrency.EUR(it) }

    companion object {
        const val NAME = "MagnaCarta"
        const val APP_ID_BALANCE = 0xf080f3

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_germany,
                region = TransitRegion.GERMANY,
                imageId = R.drawable.ximedes,
                cardType = CardType.MifareDesfire)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID_BALANCE in appIds

            override fun parseTransitData(card: DesfireCard): MagnaCartaTransitData? {
                val file2 = card.getApplication(APP_ID_BALANCE)?.getFile(2)
                val balance = file2?.data?.byteArrayToInt(6, 2)
                return MagnaCartaTransitData(mBalance = balance)
            }

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity =
                    TransitIdentity(NAME, null)

            override val hiddenAppIds: List<Int>
                get() = listOf(APP_ID_BALANCE)
        }
    }
}
