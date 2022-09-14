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
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class TampereTransitData (
        override val serialNumber: String?,
        private val mBalance: Int?,
        override val trips: List<Trip>?,
        override val subscriptions: List<TampereSubscription>?,
        private val mHolderName: String?,
        private val mHolderBirthDate: Int?,
        private val mIssueDate: Int?): TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(NAME)

    public override val balance: TransitCurrency?
        get() = mBalance?.let { TransitCurrency.EUR(it) }

    override val info: List<ListItem>?
        get() = listOfNotNull(
                if (mHolderName?.isEmpty() != false) null else ListItem(R.string.card_holders_name, mHolderName),
                if (mHolderBirthDate == 0 || mHolderBirthDate == null) null else
                    ListItem(R.string.date_of_birth, parseDaystamp(mHolderBirthDate).format()),
                ListItem(R.string.issue_date, mIssueDate?.let { parseDaystamp(it) }?.format())
        )

    companion object {
        // Finish for "Tampere travel card". It doesn't seem to have a more specific
        // brand name.
        val NAME = R.string.card_name_tampere
        const val APP_ID = 0x121ef

        private fun getSerialNumber(app: DesfireApplication?) =
                app?.getFile(0x07)?.data?.toHexString()?.substring(2, 20)?.let {
                    NumberUtils.groupString(it, " ", 6, 4, 4, 3)
                }

        private fun parse(desfireCard: DesfireCard): TampereTransitData {
            val app = desfireCard.getApplication(APP_ID)
            val file4 = app?.getFile(0x04)?.data
            val holderName = file4?.sliceOffLen(6, 24)?.readASCII()
            val holderBirthDate = file4?.byteArrayToIntReversed(0x22, 2)
            val issueDate = file4?.byteArrayToIntReversed(0x2a, 2)
            val file2 = app?.getFile(0x02)?.data
            var balance: Int? = null
            val subs = mutableListOf<TampereSubscription>()
            if (file2 != null && file2.size >= 96) {
                val blockPtr = if ((file2.byteArrayToInt(0, 1) - file2.byteArrayToInt(48, 1)) and 0xff > 0x80)
                    48
                else
                    0
                for (i in 0..2) {
                    val contractRaw = file2.sliceOffLen(blockPtr + 4 + 12 * i, 12)
                    val type = contractRaw.byteArrayToInt(2, 1)
                    when (type) {
                        0 -> continue
                        0x3 -> subs += TampereSubscription(
                                mA = contractRaw.sliceOffLen(0, 2),
                                mB = contractRaw.sliceOffLen(3, 3),
                                mStart = null,
                                mEnd = contractRaw.byteArrayToInt(6, 2),
                                mC = contractRaw.sliceOffLen(8, 4),
                                mType = type)
                        7 -> balance = contractRaw.byteArrayToInt(7, 2)
                        0xf -> subs += TampereSubscription(
                                mA = contractRaw.sliceOffLen(0, 2),
                                mB = contractRaw.sliceOffLen(3, 3),
                                mStart = contractRaw.byteArrayToInt(6, 2),
                                mEnd = contractRaw.byteArrayToInt(8, 2),
                                mC = contractRaw.sliceOffLen(10, 2),
                                mType = type
                        )
                        else -> subs += TampereSubscription(mA = contractRaw, mType = type)
                    }
                }
            }
            return TampereTransitData(
                    serialNumber = getSerialNumber(app),
                    mBalance = balance ?: app?.getFile(0x01)?.data?.byteArrayToIntReversed(),
                    trips = (app?.getFile(0x03) as? RecordDesfireFile)?.records?.map { TampereTrip.parse(it) },
                    mHolderName = holderName,
                    mHolderBirthDate = holderBirthDate,
                    mIssueDate = issueDate,
                    subscriptions = subs.ifEmpty { null }
            )
        }

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_tampere,
                imageId = R.drawable.tampere,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                region = TransitRegion.FINLAND,
                cardType = CardType.MifareDesfire)

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard) = TransitIdentity(NAME,
                    getSerialNumber(card.getApplication(APP_ID)))
        }

        private val EPOCH = Epoch.local(1900, MetroTimeZone.HELSINKI)

        fun parseDaystamp(day: Int): Daystamp = EPOCH.days(day)

        fun parseTimestamp(day: Int, minute: Int): TimestampFull = EPOCH.dayMinute(day,
                minute)
    }
}
