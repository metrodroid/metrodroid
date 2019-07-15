/*
 * OpalTransitData.kt
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.opal

import au.id.micolous.metrodroid.transit.*

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.VisibleForTesting
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Transit data type for Opal (Sydney, AU).
 *
 *
 * This uses the publicly-readable file on the card (7) in order to get the data.
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Opal
 */
@Parcelize
class OpalTransitData (
        private val mSerialNumber: Int,
        private val mBalance: Int, // cents
        private val mChecksum: Int,
        /**
         * Gets the number of weekly trips taken on this Opal card. Maxes out at 15 trips.
         */
        val weeklyTrips: Int,
        private val mAutoTopup: Boolean,
        /**
         * Gets the last action performed on the Opal card.
         *
         * Valid values are in OpalData.ACTION_*.
         */
        val lastTransaction: Int,
        /**
         * Gets the last mode of travel.
         *
         * Valid values are in OpalData.MODE_*. This does not use the Mode class, due to the merger
         * of Ferry and Light Rail travel.
         */
        val lastTransactionMode: Int,
        private val mMinute: Int,
        private val mDay: Int,
        /**
         * Gets the serial number of the latest transaction on the Opal card.
         */
        val lastTransactionNumber: Int,
        private val mLastDigit: Int
): TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(NAME)

    public override val balance: TransitCurrency?
        get() = TransitCurrency.AUD(mBalance)

    override val serialNumber: String?
        get() = formatSerialNumber(mSerialNumber, mLastDigit)

    @VisibleForTesting
    val lastTransactionTime
        get() = OPAL_EPOCH.dayMinute(mDay, mMinute)

    override val info: List<ListItem>?
        get() = listOfNotNull(
                HeaderListItem(R.string.general),
                ListItem(R.string.opal_weekly_trips, weeklyTrips.toString()),
                if (!Preferences.hideCardNumbers) {
                    ListItem(R.string.checksum, mChecksum.toString())
                } else null,

                HeaderListItem(R.string.last_transaction),
                if (!Preferences.hideCardNumbers) {
                    ListItem(R.string.transaction_counter, lastTransactionNumber.toString())
                } else null,
                ListItem(R.string.date, TimestampFormatter.longDateFormat(lastTransactionTime)),
                ListItem(R.string.time, TimestampFormatter.timeFormat(lastTransactionTime)),
                ListItem(R.string.vehicle_type, OpalData.getLocalisedMode(lastTransactionMode)),
                ListItem(R.string.transaction_type, OpalData.getLocalisedAction(lastTransaction)))


    // Opal has no concept of "subscriptions" (travel pass), only automatic top up.
    override val subscriptions: List<Subscription>?
        get() = if (mAutoTopup) {
            listOf(OpalSubscription.instance)
        } else emptyList()

    override val onlineServicesPage: String?
        get() = "https://m.opal.com.au/"

    companion object {
        private val NAME = R.string.card_name_opal
        const val APP_ID = 0x314553
        const val FILE_ID = 0x7

        private fun parse(desfireCard: DesfireCard): OpalTransitData? {
            val dataRaw = desfireCard.getApplication(APP_ID)?.getFile(FILE_ID)?.data ?: return null

            val data = dataRaw.sliceOffLen(0, 16).reverseBuffer()

            try {
                return OpalTransitData(
                        mChecksum = data.getBitsFromBuffer(0, 16),
                        weeklyTrips = data.getBitsFromBuffer(16, 4),
                        mAutoTopup = data.getBitsFromBuffer(20, 1) == 0x01,
                        lastTransaction = data.getBitsFromBuffer(21, 4),
                        lastTransactionMode = data.getBitsFromBuffer(25, 3),
                        mMinute = data.getBitsFromBuffer(28, 11),
                        mDay = data.getBitsFromBuffer(39, 15),
                        mBalance = data.getBitsFromBufferSigned(54, 21),
                        lastTransactionNumber = data.getBitsFromBuffer(75, 16),
                        // Skip bit here
                        mLastDigit = data.getBitsFromBuffer(92, 4),
                        mSerialNumber = data.getBitsFromBuffer(96, 32)
                )
            } catch (ex: Exception) {
                throw RuntimeException("Error parsing Opal data", ex)
            }
        }

        @VisibleForTesting
        val CARD_INFO = CardInfo(
                imageId = R.drawable.opal_card,
                name = NAME,
                locationId = R.string.location_sydney_australia,
                cardType = CardType.MifareDesfire,
                resourceExtraNote = R.string.card_note_opal)

        val TIME_ZONE = MetroTimeZone.SYDNEY
        private val OPAL_EPOCH = Epoch.local(1980, TIME_ZONE)

        private fun formatSerialNumber(serialNumber: Int, lastDigit: Int) =
                NumberUtils.formatNumber(3085_2200_0000_0000L + (serialNumber * 10L) + lastDigit,
                        " ", 4, 4, 4, 4)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity {
                val dataRaw = card.getApplication(APP_ID)?.getFile(FILE_ID)?.data
                        ?: return TransitIdentity(NAME, null)
                val data = dataRaw.sliceOffLen(0, 5).reverseBuffer()

                val lastDigit = data.getBitsFromBuffer(4, 4)
                val serialNumber = data.getBitsFromBuffer(8, 32)
                return TransitIdentity(NAME, formatSerialNumber(serialNumber, lastDigit))
            }
        }
    }
}
