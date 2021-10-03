/*
 * TPFCardTransitData.kt
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
package au.id.micolous.metrodroid.transit.serialonly

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class TPFCardTransitData (private val mSerial: ImmutableByteArray): SerialOnlyTransitData() {

    override val reason: Reason
        get() = Reason.LOCKED

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    companion object {
        private const val NAME = "TPF card"
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_fribourg_ch,
                cardType = CardType.MifareDesfire,
                imageId = R.drawable.tpf_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.SWITZERLAND,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun getSerial(card: DesfireCard) =
                card.tagId.reverseBuffer()

        private fun formatSerial(serial: ImmutableByteArray) = serial.toHexString().toUpperCase()
    }

    object Factory : DesfireCardTransitFactory {
        override fun earlyCheck(appIds: IntArray) = (0x43544b in appIds)

        override fun parseTransitData(card: DesfireCard) =
                TPFCardTransitData(mSerial = getSerial(card))

        override fun parseTransitIdentity(card: DesfireCard) =
                TransitIdentity(NAME, formatSerial(getSerial(card)))

        override val allCards get() = listOf(CARD_INFO)
    }
}
