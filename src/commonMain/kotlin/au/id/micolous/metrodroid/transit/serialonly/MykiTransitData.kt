/*
 * MykiTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Transit data type for Myki (Melbourne, AU).
 *
 *
 * This is a very limited implementation of reading Myki, because most of the data is stored in
 * locked files.
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Myki
 */
@Parcelize
class MykiTransitData (private val mSerial: String): SerialOnlyTransitData() {

    override val reason: Reason
        get() = Reason.LOCKED

    override val cardName get() = NAME

    override val serialNumber get() = mSerial

    override val moreInfoPage get() = "https://micolous.github.io/metrodroid/myki"

    companion object {
        const val NAME = "Myki"
        const val APP_ID_1 = 0x11f2
        const val APP_ID_2 = 0xf010f2

        // 308425 as a uint32_le (the serial number prefix)
        private val MYKI_HEADER = ImmutableByteArray.fromHex("c9b40400")
        private const val MYKI_PREFIX: Long = 308425

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.myki_card,
                name = NAME,
                cardType = CardType.MifareDesfire,
                locationId = R.string.location_victoria_australia,
                region = TransitRegion.AUSTRALIA,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun parse(desfireCard: DesfireCard): MykiTransitData {
            val metadata = desfireCard.getApplication(APP_ID_1)?.getFile(15)?.data

            val serial = parseSerial(metadata) ?: throw RuntimeException("Invalid Myki data (parseSerial = null)")

            return MykiTransitData(mSerial = serial)
        }

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun check(card: DesfireCard): Boolean {
                val app1 = card.getApplication(APP_ID_1)
                if (app1 == null || card.getApplication(APP_ID_2) == null) {
                    return false
                }

                val file = app1.getFile(15) ?: return false

                // Check that we have the correct serial prefix (308425)
                return file.data.copyOfRange(0, 4).contentEquals(MYKI_HEADER)
            }

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun earlyCheck(appIds: IntArray) = (APP_ID_1 in appIds) && (APP_ID_2 in appIds)

            override val allCards get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, parseSerial(
                            card.getApplication(APP_ID_1)?.getFile(15)?.data
                    ))
        }

        /**
         * Parses a serial number in 0x11f2 file 0xf
         * @param file content of the serial file
         * @return String with the complete serial number, or null on error
         */
        private fun parseSerial(file: ImmutableByteArray?): String? {
            if (file == null) return null

            val serial1 = file.byteArrayToLongReversed(0, 4)
            if (serial1 != MYKI_PREFIX) {
                return null
            }

            val serial2 = file.byteArrayToLongReversed(4, 4)
            if (serial2 > 99999999) {
                return null
            }

            val formattedSerial = NumberUtils.zeroPad(serial1, 6) +  NumberUtils.zeroPad(serial2, 8)
            return formattedSerial + NumberUtils.calculateLuhn(formattedSerial)
        }
    }
}
