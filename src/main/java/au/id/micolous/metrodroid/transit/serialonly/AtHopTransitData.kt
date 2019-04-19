/*
 * AtHopStubTransitData.java
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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.NumberUtils
import kotlinx.android.parcel.Parcelize
import org.jetbrains.annotations.NonNls

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
        private val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_auckland)
                .setCardType(CardType.MifareDesfire)
                .setExtraNote(R.string.card_note_card_number_only)
                .build()

        private fun getSerial(card: DesfireCard) =
                card.getApplication(APP_ID_SERIAL)?.getFile(8)?.data?.getBitsFromBuffer(
                    61, 32)

        @NonNls
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

            override fun parseTransitIdentity(desfireCard: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(getSerial(desfireCard)))

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
