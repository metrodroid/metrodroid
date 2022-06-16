/*
 * HoloTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko, Michael Farrell, Trevor Nielsen
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
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.VisibleForTesting
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem

/**
 * Transit data type for HOLO card.
 *
 * This is a very limited implementation of reading HOLO, because only
 * little data is stored on the card
 *
 * Documentation of format: https://github.com/metrodroid/metrodroid/wiki/HOLO
 */
@Parcelize
data class HoloTransitData(
    private val mSerial: Int?,
    private val mLastTransactionTimestamp: Int,
    private val mManufacturingId: String,
) : SerialOnlyTransitData() {

    public override val extraInfo: List<ListItem>
        get() = listOf(
            ListItem(
                Localizer.localizeFormatted(R.string.last_transaction),
                when (mLastTransactionTimestamp) {
                    0 -> Localizer.localizeFormatted(R.string.never)
                    else -> TimestampFormatter.dateTimeFormat(parseTime(mLastTransactionTimestamp))
                }
            ),
            ListItem(R.string.manufacture_id, mManufacturingId),
        )

    override val reason: Reason
        get() = Reason.NOT_STORED

    override val cardName get() = NAME

    override val serialNumber get() = formatSerial(mSerial)

    companion object {
        private const val NAME = "HOLO"
        private const val APP_ID = 0x6013f2

        private val TZ = MetroTimeZone.HONOLULU

        @VisibleForTesting
        val CARD_INFO = CardInfo(
            name = NAME,
            cardType = CardType.MifareDesfire,
            imageId = R.drawable.holo_card,
            locationId = R.string.location_oahu,
            region = TransitRegion.USA,
            resourceExtraNote = R.string.card_note_card_number_only
        )

        private fun parse(card: DesfireCard): HoloTransitData? {
            val app = card.getApplication(APP_ID) ?: return null
            val file0 = app.getFile(0)?.data ?: return null
            val file1 = app.getFile(1)?.data ?: return null
            val serial = parseSerial(app) ?: return null

            val mfgId = "1-001-${file0.convertBCDtoInteger(8, 3)}${file0.byteArrayToInt(0xb, 3)}-XA"
            val lastTransactionTimestamp = file1.byteArrayToInt(8, 4)

            return HoloTransitData(
                mSerial = serial,
                mLastTransactionTimestamp = lastTransactionTimestamp,
                mManufacturingId = mfgId
            )
        }

        private fun parseSerial(app: DesfireApplication?) =
            app?.getFile(0)?.data?.convertBCDtoInteger(0xe, 2)

        object HoloTransitFactory : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override val allCards get() = listOf(CARD_INFO)

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard) =
                TransitIdentity(NAME, formatSerial(parseSerial(card.getApplication(APP_ID))))
        }

        private fun formatSerial(ser: Int?) =
            if (ser != null)
                "31059300 1 ***** *${ser}"
            else
                null

        private fun parseTime(date: Int) = Epoch.utc(1970, TZ).seconds(date.toLong())
    }
}
