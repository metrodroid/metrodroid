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

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

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
data class TrimetHopTransitData(private val mSerial: Int,
                                private val mIssueDate: Int) : SerialOnlyTransitData() {

    public override val extraInfo: List<ListItem>?
        get() = listOf(ListItem(R.string.issue_date,
                Utils.dateTimeFormat(parseTime(mIssueDate))))

    override val reason: SerialOnlyTransitData.Reason
        get() = SerialOnlyTransitData.Reason.NOT_STORED

    override fun getCardName() = NAME

    override fun getSerialNumber() = formatSerial(mSerial)

    companion object {
        private const val NAME = "Hop Fastpass"
        private const val APP_ID = 0xe010f2

        private val TZ = TimeZone.getTimeZone("America/Los_Angeles")

        private val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setCardType(CardType.MifareDesfire)
                .setImageId(R.drawable.trimethop_card)
                .setLocation(R.string.location_portland)
                .setExtraNote(R.string.card_note_card_number_only)
                .build()

        private fun parse(card: DesfireCard): TrimetHopTransitData {
            val app = card.getApplication(APP_ID)
            val file1 = app!!.getFile(1)!!.data
            return TrimetHopTransitData(
                    mSerial = parseSerial(app),
                    mIssueDate = Utils.byteArrayToInt(file1, 8, 4))
        }

        private fun parseSerial(app: DesfireApplication) =
                Utils.byteArrayToInt(app.getFile(0)!!.data, 0xc, 4)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun getAllCards() = listOf(CARD_INFO)

            override fun parseTransitData(desfireCard: DesfireCard) = parse(desfireCard)

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity(NAME, formatSerial(parseSerial(card.getApplication(APP_ID)!!)))
        }

        private fun formatSerial(ser: Int) =
                String.format(Locale.ENGLISH, "01-001-%08d-RA", ser)

        private fun parseTime(date: Int): Calendar {
            val c = GregorianCalendar(TZ)
            // Unix Time
            c.timeInMillis = date * 1000L
            return c
        }
    }
}
