/*
 * IstanbulKartTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko, Michael Farrell
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
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Transit data type for IstanbulKart.
 *
 *
 * This is a very limited implementation of reading IstanbulKart, because most of the data is stored in
 * locked files.
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/IstanbulKart
 */
@Parcelize
class IstanbulKartTransitData (private val mSerial: String,
                               private val mSerial2: String): SerialOnlyTransitData() {

    public override val extraInfo: List<ListItem>?
        get() = listOf(ListItem(R.string.istanbulkart_2nd_card_number, mSerial2))

    override val reason: Reason
        get() = Reason.LOCKED

    override val cardName get() = NAME

    override val serialNumber get() = formatSerial(mSerial)

    companion object {
        private const val NAME = "IstanbulKart"
        private const val APP_ID = 0x422201

        private fun parse(card: DesfireCard): IstanbulKartTransitData? {
            val metadata = card.getApplication(APP_ID)?.getFile(2)?.data

            try {
                val serial = parseSerial(metadata) ?: return null
                return IstanbulKartTransitData(
                        mSerial = serial,
                        mSerial2 = card.tagId.toHexString().uppercase()
                )
            } catch (ex: Exception) {
                throw RuntimeException("Error parsing IstanbulKart data", ex)
            }
        }

        private val CARD_INFO = CardInfo(
                name = NAME,
                cardType = CardType.MifareDesfire,
                locationId = R.string.location_istanbul,
                resourceExtraNote = R.string.card_note_card_number_only,
                imageId = R.drawable.istanbulkart_card,
                region = TransitRegion.TURKEY,
                imageAlphaId = R.drawable.iso7810_id1_alpha)

        /**
         * Parses a serial number in 0x42201 file 0x2
         * @param file content of the serial file
         * @return String with the complete serial number, or null on error
         */
        private fun parseSerial(file: ImmutableByteArray?) =
                file?.getHexString(0, 8)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) = (APP_ID in appIds)

            override val allCards get() = listOf(CARD_INFO)

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity {
                val serial = parseSerial(card.getApplication(APP_ID)?.getFile(2)?.data)
                return TransitIdentity(NAME,
                        if (serial != null) formatSerial(serial) else null)
            }
        }

        private fun formatSerial(serial: String) =
                NumberUtils.groupString(serial, " ", 4, 4, 4)
    }
}
