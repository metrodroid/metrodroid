/*
 * StrelkaTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Strelka cards.
 */

@Parcelize
data class StrelkaTransitData (private val mSerial: String): SerialOnlyTransitData() {
    override val serialNumber get() = formatShortSerial(mSerial)

    public override val extraInfo
        get() = listOf(ListItem(R.string.strelka_long_serial, mSerial))

    override val reason
        get() = Reason.MORE_RESEARCH_NEEDED

    override val cardName get() = Localizer.localizeString(R.string.card_name_strelka)

    companion object {
        private val CARD_INFO = CardInfo(
                name = R.string.card_name_strelka,
                locationId = R.string.location_moscow,
                cardType = CardType.MifareClassic,
                resourceExtraNote = R.string.card_note_card_number_only,
                imageId = R.drawable.strelka_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.RUSSIA,
                keysRequired = true,
                preview = true)

        private fun formatShortSerial(serial: String) =
                NumberUtils.groupString(serial.substring(8), " ", 4, 4)

        private fun getSerial(card: ClassicCard) =
                card[12, 0].data.getHexString(2, 10).substring(0, 19)

        fun parse(card: ClassicCard) =
                StrelkaTransitData(mSerial = getSerial(card))

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(Localizer.localizeString(R.string.card_name_strelka),
                        formatShortSerial(getSerial(card)))

            override fun parseTransitData(card: ClassicCard) =
                    parse(card)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val toc = sectors[0][2].data
                // Check toc entries for sectors 10,12,13,14 and 15
                return (toc.byteArrayToInt(4, 2) == 0x18f0
                        && toc.byteArrayToInt(8, 2) == 5
                        && toc.byteArrayToInt(10, 2) == 0x18e0
                        && toc.byteArrayToInt(12, 2) == 0x18e8)
            }

            override fun isDynamicKeys(sectors: List<ClassicSector>, sectorIndex: Int,
                                       keyType: ClassicSectorKey.KeyType): Boolean =
                    sectorIndex in listOf(13, 14)

            // 1 is actually enough but let's show Troika+Strelka as Troika
            override val earlySectors get() = 2

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
