/*
 * AtHopStubTransitData.kt
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Stub implementation for AT HOP (Auckland, NZ).
 *
 *
 * https://github.com/micolous/metrodroid/wiki/AT-HOP
 */
@Parcelize
data class AtHopTransitData (private val mSerial: Int?): SerialOnlyTransitData() {

    override val reason: SerialOnlyTransitData.Reason
        get() = SerialOnlyTransitData.Reason.LOCKED

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    companion object {
        private const val APP_ID_SERIAL = 0xffffff
        private const val NAME = "AT HOP"
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_auckland,
                cardType = CardType.MifareDesfire,
                imageId = R.drawable.athopcard,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.NEW_ZEALAND,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun getSerial(card: DesfireCard) =
                card.getApplication(APP_ID_SERIAL)?.getFile(8)?.data?.getBitsFromBuffer(
                    61, 32)

        private fun formatSerial(serial: Int?) =
                if (serial != null)
                    "7824 6702 " + NumberUtils.formatNumber(serial.toLong(), " ", 4, 4, 3)
                else
                    null

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) =
                    (0x4055 in appIds) && (APP_ID_SERIAL in appIds)

            override fun parseTransitData(card: DesfireCard) =
                    AtHopTransitData(mSerial = getSerial(card))

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(getSerial(card)))

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
