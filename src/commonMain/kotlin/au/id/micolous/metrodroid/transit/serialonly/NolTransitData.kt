/*
 * NolTransitData.kt
 *
 * Copyright 2019  Google
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
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
data class NolTransitData (private val mSerial: Int?, private val mType: Int?): SerialOnlyTransitData() {

    override val reason: Reason
        get() = Reason.LOCKED

    override val extraInfo: List<ListItem>?
        get() = super.extraInfo.orEmpty() + listOf(ListItem(R.string.card_type, when (mType) {
            0x4d5 -> Localizer.localizeString(R.string.nol_silver)
            0x4d9 -> Localizer.localizeString(R.string.nol_red)
            else -> Localizer.localizeString(R.string.unknown_format, "${mType?.toString(16)}")
        }))

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = Localizer.localizeString(NAME)

    companion object {
        private const val APP_ID_SERIAL = 0xffffff
        private val NAME = R.string.card_name_nol
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_dubai,
                imageId = R.drawable.nol,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.UAE,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun getSerial(card: DesfireCard) =
                card.getApplication(APP_ID_SERIAL)?.getFile(8)?.data?.getBitsFromBuffer(
                    61, 32)

        private fun formatSerial(serial: Int?) =
                if (serial != null)
                    NumberUtils.formatNumber(serial.toLong(), " ", 3, 3, 4)
                else
                    null

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) =
                    (0x4078 in appIds) && (APP_ID_SERIAL in appIds)

            override fun parseTransitData(card: DesfireCard) =
                    NolTransitData(mSerial = getSerial(card),
                            mType = card.getApplication(APP_ID_SERIAL)?.getFile(8)
                                    ?.data?.byteArrayToInt(0xc, 2))

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(getSerial(card)))

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
