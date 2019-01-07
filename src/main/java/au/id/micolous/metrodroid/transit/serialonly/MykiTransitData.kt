/*
 * MykiTransitData.java
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

import android.net.Uri
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import kotlinx.android.parcel.Parcelize
import org.jetbrains.annotations.NonNls
import java.util.*

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

    override val reason: SerialOnlyTransitData.Reason
        get() = SerialOnlyTransitData.Reason.LOCKED

    override fun getCardName() = NAME

    override fun getSerialNumber() = mSerial

    override fun getMoreInfoPage(): Uri? =
            Uri.parse("https://micolous.github.io/metrodroid/myki")

    companion object {
        const val NAME = "Myki"
        const val APP_ID_1 = 0x11f2
        const val APP_ID_2 = 0xf010f2

        // 308425 as a uint32_le (the serial number prefix)
        private val MYKI_HEADER = ImmutableByteArray.fromHex("c9b40400")
        private const val MYKI_PREFIX: Long = 308425

        private val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.myki_card)
                .setName(MykiTransitData.NAME)
                .setCardType(CardType.MifareDesfire)
                .setLocation(R.string.location_victoria_australia)
                .setExtraNote(R.string.card_note_card_number_only)
                .build()


        private fun parse(desfireCard: DesfireCard): MykiTransitData {
            val metadata = desfireCard.getApplication(APP_ID_1)!!.getFile(15)!!.data

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

                val data = file.data ?: return false

                // Check that we have the correct serial prefix (308425)
                return data.copyOfRange(0, 4).contentEquals(MYKI_HEADER)
            }

            override fun parseTransitData(desfireCard: DesfireCard) = parse(desfireCard)

            override fun earlyCheck(appIds: IntArray) = (APP_ID_1 in appIds) && (APP_ID_2 in appIds)

            override fun getAllCards() = listOf(CARD_INFO)

            override fun parseTransitIdentity(desfireCard: DesfireCard) =
                    TransitIdentity(NAME, parseSerial(
                            desfireCard.getApplication(APP_ID_1)!!.getFile(15)!!.data
                    ))
        }

        /**
         * Parses a serial number in 0x11f2 file 0xf
         * @param file content of the serial file
         * @return String with the complete serial number, or null on error
         */
        private fun parseSerial(file: ImmutableByteArray): String? {
            val serial1 = file.byteArrayToLongReversed(0, 4)
            if (serial1 != MYKI_PREFIX) {
                return null
            }

            val serial2 = file.byteArrayToLongReversed(4, 4)
            if (serial2 > 99999999) {
                return null
            }

            @NonNls val formattedSerial = String.format(Locale.ENGLISH, "%06d%08d", serial1, serial2)
            return formattedSerial + Utils.calculateLuhn(formattedSerial)
        }
    }
}
