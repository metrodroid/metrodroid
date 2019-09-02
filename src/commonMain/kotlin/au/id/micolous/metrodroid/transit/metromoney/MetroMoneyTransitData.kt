/*
 * MetroMoneyTransitData.kt
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

package au.id.micolous.metrodroid.transit.metromoney

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

private val NAME = R.string.card_name_metromoney
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_tbilisi,
        imageId = R.drawable.metromoney,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        cardType = CardType.MifareClassic,
        keysRequired = true, keyBundle = "metromoney")

private fun strDate(raw: ImmutableByteArray, off: Int): String
        = "${raw.getBitsFromBuffer(off, 6)+2000}.${raw.getBitsFromBuffer(off+6, 4)}.${raw.getBitsFromBuffer(off+10, 5)} " +
        "${raw.getBitsFromBuffer(off+15, 5)}:${raw.getBitsFromBuffer(off+20, 6)}:${raw.getBitsFromBuffer(off+26, 6)}"

private fun formatSerial(serial: Long) = NumberUtils.zeroPad(serial, 10)

private fun getSerial(card: ClassicCard) = card[0,0].data.byteArrayToLongReversed(0, 4)

@Parcelize
data class MetroMoneyTransitData(private val mSerial: Long, private val mBalance: Int,
                                 private val mDate1: String, private val mDate2: String,
                                 private val mDate3: String, private val mDate4: String) : TransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = Localizer.localizeString(NAME)

    override val balance get() = TransitCurrency(mBalance, "GEL")

    override fun getRawFields(level: RawLevel): List<ListItem> = listOf(
            ListItem("Date1", mDate1),
            ListItem("Date2", mDate2),
            ListItem("Date3", mDate3),
            ListItem("Date4", mDate4)
    )
}

object MetroMoneyTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, formatSerial(getSerial(card)))

    override fun parseTransitData(card: ClassicCard): MetroMoneyTransitData {
        return MetroMoneyTransitData(
                mSerial = getSerial(card),
                mBalance = card[1,1].data.byteArrayToIntReversed(0, 4),
                mDate1 = strDate(card[0,1].data, 48),
                mDate2 = strDate(card[1,2].data, 32),
            mDate3 = strDate(card[1,2].data, 96),
            mDate4 = strDate(card[2,2].data, 32)
        )
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            HashUtils.checkKeyHash(sectors[0], "metromoney",
                    "c48676dac68ec332570a7c20e12e08cb", "5d2457ed5f196e1757b43d074216d0d0") >= 0

    override val earlySectors get() = 1
}
