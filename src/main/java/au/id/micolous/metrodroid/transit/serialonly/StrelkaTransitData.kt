/*
 * StrelkaTransitData.java
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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

/**
 * Strelka cards.
 */

@Parcelize
data class StrelkaTransitData (private val mSerial: String): SerialOnlyTransitData() {
    override fun getSerialNumber() = formatShortSerial(mSerial)

    public override val extraInfo
        get() = listOf(ListItem(R.string.strelka_long_serial, mSerial))

    override val reason
        get() = SerialOnlyTransitData.Reason.MORE_RESEARCH_NEEDED

    override fun getCardName() = Utils.localizeString(R.string.card_name_strelka)

    companion object {
        private val CARD_INFO = CardInfo.Builder()
                .setName(Utils.localizeString(R.string.card_name_strelka))
                .setLocation(R.string.location_moscow)
                .setCardType(CardType.MifareClassic)
                .setExtraNote(R.string.card_note_card_number_only)
                .setImageId(R.drawable.strelka_card, R.drawable.iso7810_id1_alpha)
                .setKeysRequired()
                .setPreview()
                .build()

        private fun formatShortSerial(serial: String) =
                NumberUtils.groupString(serial.substring(8), " ", 4, 4)

        private fun getSerial(card: ClassicCard) =
                card[12, 0].data.getHexString(2, 10).substring(0, 19)

        fun parse(card: ClassicCard) =
                StrelkaTransitData(mSerial = getSerial(card))

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) =
                    TransitIdentity(Utils.localizeString(R.string.card_name_strelka),
                        formatShortSerial(getSerial(card)))

            override fun parseTransitData(classicCard: ClassicCard) =
                    parse(classicCard)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val toc = sectors[0][2].data
                // Check toc entries for sectors 10,12,13,14 and 15
                return (toc.byteArrayToInt(4, 2) == 0x18f0
                        && toc.byteArrayToInt(8, 2) == 5
                        && toc.byteArrayToInt(10, 2) == 0x18e0
                        && toc.byteArrayToInt(12, 2) == 0x18e8)
            }

            // 1 is actually enough but let's show Troika+Strelka as Troika
            override fun earlySectors() = 2

            override fun getAllCards() = listOf(CARD_INFO)
        }
    }
}
