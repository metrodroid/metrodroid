/*
 * TampereTransitData.kt
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
package au.id.micolous.metrodroid.transit.tampere

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

private val EPOCH = Epoch.utc(1900, MetroTimeZone.HELSINKI)
private fun parseTimestamp(day: Int, minute: Int): TimestampFull = EPOCH.dayMinute(day,
        minute)

private fun parseDaystamp(day: Int): Daystamp = EPOCH.days(day)

@Parcelize
class TampereTrip(private val mDay: Int, private val mMinute: Int,
                  private val mFare: Int,
                  private val mMinutesSinceFirstStamp: Int,
                  private val mTransport: Int,
                  private val mA: Int, // 0x8b for first trip in a day, 0xb otherwise, 0 for refills
                  private val mB: Int, // Always 4
                  private val mC: Int, // Always 0
                  private val mRoute: Int,
                  private val mEventCode: Int, // 3 = topup, 5 = first tap, 11 = transfer
                  private val mFlags: Int
                  ): Trip() {
    override val startTimestamp: Timestamp?
        get() = parseTimestamp(mDay, mMinute)
    override val fare: TransitCurrency?
        get() = TransitCurrency.EUR(mFare)
    override val mode: Mode
        get() = when (mTransport) {
            0xba, 0xde -> Mode.BUS // ba = bus. de = ?? (used for subscriptions)
            0x4c -> Mode.TICKET_MACHINE // 4c is ASCII for 'L' from Finnish "lataa" = "top-up"
            else -> Mode.OTHER
        }

    override val isTransfer: Boolean
        get() = (mFlags and 0x40000) != 0

    override val humanReadableRouteID get() = mRoute.toString()

    override val routeName get() = TampereTransitData.getRouteName(mRoute)

    override fun getRawFields(level: TransitData.RawLevel) = "A=0x${mA.toString(16)}/B=$mB/C=$mC" +
            (if (level != TransitData.RawLevel.UNKNOWN_ONLY) "/EventCode=$mEventCode/flags=0x${mFlags.toString(16)}/sinceFirstStamp=$mMinutesSinceFirstStamp/transport=0x${mTransport.toString(16)}" else "")

    companion object {
        fun parse(raw: ImmutableByteArray): TampereTrip? {
            val type = raw.byteArrayToIntReversed(4, 1)
            val fare = raw.byteArrayToIntReversed(8, 2).let {
                if (type == 0x4c)
                    -it
                else
                    it
            }
            val minuteField = raw.byteArrayToIntReversed(6, 2)
            val cField = raw.byteArrayToIntReversed(10, 2)
            return TampereTrip(mDay = raw.byteArrayToIntReversed(0, 2),
                    mMinute = minuteField shr 5,
                    mFare = fare,
                    mMinutesSinceFirstStamp = raw.byteArrayToIntReversed(2, 1),
                    mTransport = type,
                    mA = raw.byteArrayToIntReversed(3, 1),
                    mB = raw.byteArrayToIntReversed(5, 1),
                    mC = cField and 3,
                    mRoute = cField shr 2,
                    mEventCode = minuteField and 0x1f,
                    mFlags = raw.byteArrayToIntReversed(12, 3)
            // Last byte: CRC-8-maxim checksum of the record
            )
        }
    }
}

@Parcelize
class TampereTransitData (
        override val serialNumber: String?,
        private val mBalance: Int?,
        override val trips: List<Trip>?,
        private val mHolderName: String?,
        private val mHolderBirthDate: Int?,
        private val mIssueDate: Int?): TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(NAME)

    public override val balance: TransitCurrency?
        get() = mBalance?.let { TransitCurrency.EUR(it) }

    override val info: List<ListItem>?
        get() = listOf(
                ListItem(R.string.card_holders_name, mHolderName),
                ListItem(R.string.date_of_birth, mHolderBirthDate?.let { parseDaystamp(it) }?.format()),
                ListItem(R.string.issue_date, mIssueDate?.let { parseDaystamp(it) }?.format())
        )

    companion object {
        // Finish for "Tampere travel card". It doesn't seem to have a more specific
        // brand name.
        val NAME = R.string.card_name_tampere
        const val APP_ID = 0x121ef

        private fun getSerialNumber(app: DesfireApplication?) = app?.getFile(0x07)?.data?.toHexString()?.substring(2, 20)

        private fun parse(desfireCard: DesfireCard): TampereTransitData? {
            val app = desfireCard.getApplication(APP_ID)
            val file4 = app?.getFile(0x04)?.data
            val holderName = file4?.sliceOffLen(6, 24)?.readASCII()
            val holderBirthDate = file4?.byteArrayToIntReversed(0x22, 2)
            val issueDate = file4?.byteArrayToIntReversed(0x2a, 2)
            return TampereTransitData(
                    serialNumber = getSerialNumber(app),
                    mBalance = app?.getFile(0x01)?.data?.byteArrayToIntReversed(),
                    trips = (app?.getFile(0x03) as? RecordDesfireFile)?.records?.mapNotNull { TampereTrip.parse(it) },
                    mHolderName = holderName,
                    mHolderBirthDate = holderBirthDate,
                    mIssueDate = issueDate
            )
        }

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_tampere,
                imageId = R.drawable.tampere,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.FINLAND,
                cardType = CardType.MifareDesfire)

        fun getRouteName(routeNumber: Int) = FormattedString("${routeNumber/100}/${routeNumber%100}")

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard) = TransitIdentity(NAME,
                    getSerialNumber(card.getApplication(APP_ID)))
        }
    }
}
