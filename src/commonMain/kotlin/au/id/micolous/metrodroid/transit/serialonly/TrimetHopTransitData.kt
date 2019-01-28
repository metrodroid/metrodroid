/*
 * TrimetHopTransitData.java
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
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Transit data type for TriMet Hop Fastpass.
 *
 *
 * This is a very limited implementation of reading TrimetHop, because only
 * little data is stored on the card
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/TrimetHopFastPass
 */
@Parcelize
data class TrimetHopTransitData(private val mSerial: Int?,
                                private val mIssueDate: Int?) : SerialOnlyTransitData() {

    public override val extraInfo: List<ListItem>?
        get() = mIssueDate?.let { listOf(ListItem(R.string.issue_date,
                TimestampFormatter.dateTimeFormat(parseTime(mIssueDate)))) }

    override val reason: SerialOnlyTransitData.Reason
        get() = SerialOnlyTransitData.Reason.NOT_STORED

    override val cardName get() = NAME

    override val serialNumber get() = formatSerial(mSerial)

    companion object {
        private const val NAME = "Hop Fastpass"
        private const val APP_ID = 0xe010f2

        private val TZ = MetroTimeZone.LOS_ANGELES

        private val CARD_INFO = CardInfo(
                name = NAME,
                cardType = CardType.MifareDesfire,
                imageId = R.drawable.trimethop_card,
                locationId = R.string.location_portland,
                resourceExtraNote = R.string.card_note_card_number_only)

        private fun parse(card: DesfireCard): TrimetHopTransitData? {
            val app = card.getApplication(APP_ID) ?: return null
            val file1 = app.getFile(1)?.data
            val serial = parseSerial(app)
            val issueDate = file1?.byteArrayToInt(8, 4)
            if (serial == null && issueDate == null) return null

            return TrimetHopTransitData(
                    mSerial = serial,
                    mIssueDate = issueDate)
        }

        private fun parseSerial(app: DesfireApplication?) =
                app?.getFile(0)?.data?.byteArrayToInt(0xc, 4)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override val allCards get() = listOf(CARD_INFO)

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(parseSerial(card.getApplication(APP_ID))))
        }

        private fun formatSerial(ser: Int?) =
                if (ser != null)
                    "01-001-${NumberUtils.zeroPad(ser, 8)}-RA"
                else
                    null

        private fun parseTime(date: Int) = Epoch.utc(1970, TZ).seconds(date.toLong())
    }
}
