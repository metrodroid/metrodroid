/*
 * PrestoTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko, Michael Farrell, Philip Duncan
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
 * Transit data type for PRESTO (Ontario, Canada).
 *
 *
 * This is a very limited implementation of reading PRESTO, because most of the data is stored in
 * locked files.
 *
 *
 * https://github.com/micolous/metrodroid/wiki/PRESTO
 */
@Parcelize
data class PrestoTransitData (private val mSerial: Int?): SerialOnlyTransitData() {

    override val reason: Reason
        get() = Reason.LOCKED

    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    companion object {
        private const val APP_ID_SERIAL = 0xff30ff
        private const val NAME = "PRESTO"
        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_ontario,
                cardType = CardType.MifareDesfire,
                imageId = R.drawable.presto_card,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.CANADA,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun getSerial(card: DesfireCard) =
                card.getApplication(APP_ID_SERIAL)?.getFile(8)?.data?.getBitsFromBuffer(
                    85, 24)

        private fun formatSerial(serial: Int?): String? {
            val s = serial ?: return null
            val main = "312401 ${NumberUtils.formatNumber(s.toLong(), " ", 4, 4)} 00"
            return main + NumberUtils.calculateLuhn(main.replace(" ", ""))
        }

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) =
                    (0x2000 in appIds) && (APP_ID_SERIAL in appIds)

            override fun parseTransitData(card: DesfireCard) =
                    PrestoTransitData(mSerial = getSerial(card))

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(getSerial(card)))

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
