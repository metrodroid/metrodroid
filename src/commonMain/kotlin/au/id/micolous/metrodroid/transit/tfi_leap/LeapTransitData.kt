/*
 * LeapTransitData.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.tfi_leap

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.StationTableReader

private const val NAME = "Leap"

@Parcelize
class LockedLeapTransitData : TransitData() {
    override val serialNumber: String?
        get() = null

    public override val balance: TransitBalance?
        get() = null

    override val info: List<ListItem>?
        get() = listOf(ListItem(R.string.leap_locked_warning, ""))

    override val cardName: String
        get() = NAME
}

@Parcelize
private class AccumulatorBlock(private val mAccumulators: List<Pair<Int, Int>>, // agency, value
                               private val mAccumulatorRegion: Int?,
                               private val mAccumulatorScheme: Int?,
                               private val mAccumulatorStart: TimestampFull) : Parcelable {

    // Fare cap explanation: https://about.leapcard.ie/fare-capping
    //
    // There are two types of caps:
    // - Daily travel spend
    // - Weekly travel spend
    //
    // There are then two levels of caps:
    // - Single-operator spend (and each operator has different thresholds)
    // - All-operator spend (which applies to the sum of all fares)
    //
    // Certain services are excluded from the caps.
    val info: List<ListItem>
        get()  = listOf(ListItem(
                    R.string.leap_period_start,
                    TimestampFormatter.dateTimeFormat(mAccumulatorStart)),
            ListItem(R.string.leap_accumulator_region, mAccumulatorRegion.toString()),
            ListItem(R.string.leap_accumulator_total,
                    TransitCurrency.EUR(mAccumulatorScheme!!)
                            .maybeObfuscateBalance().formatCurrencyString(true))) +
                mAccumulators.filter { (_, value) -> value != 0 }.map { (agency, value) ->
                    ListItem(
                            FormattedString(Localizer.localizeString(R.string.leap_accumulator_agency,
                                    StationTableReader.getOperatorName(LeapTransitData.LEAP_STR, agency,
                                            false))),
                            TransitCurrency.EUR(value).maybeObfuscateBalance().formatCurrencyString(true)
                    )
                }

    constructor(file: ImmutableByteArray, offset: Int) : this(
            mAccumulatorStart = LeapTransitData.parseDate(file, offset),
            mAccumulatorRegion = file[offset + 4].toInt(),
            mAccumulatorScheme = file.byteArrayToInt(offset + 5, 3),
            mAccumulators = (0..3).map { i ->
                Pair(file.byteArrayToInt(offset + 8 + 2 * i, 2),
                        LeapTransitData.parseBalance(file, offset + 0x10 + 3 * i))
            }
            // 4 bytes hash
    )
}

@Parcelize
class LeapTransitData private constructor(private val mIssueDate: Timestamp,
                                          override val serialNumber: String,
                                          private val mBalance: Int,
                                          private val mIssuerId: Int,
                                          private val mInitDate: Timestamp,
                                          private val mExpiryDate: Timestamp,
                                          private val mDailyAccumulators: AccumulatorBlock,
                                          private val mWeeklyAccumulators: AccumulatorBlock,
                                          override val trips: List<LeapTrip>) : TransitData() {

    public override val balance: TransitBalance?
        get() = TransitBalanceStored(TransitCurrency.EUR(mBalance), null, mExpiryDate)

    override val info: List<ListItem>?
        get() = listOfNotNull(
            ListItem(R.string.initialisation_date,
                    mInitDate.format()),
            ListItem(R.string.issue_date,
                    mIssueDate.format()),
            if (Preferences.hideCardNumbers) {
                ListItem(R.string.card_issuer, mIssuerId.toString())
            } else null,
            HeaderListItem(R.string.leap_daily_accumulators)) +
            mDailyAccumulators.info +

            HeaderListItem(R.string.leap_weekly_accumulators) +
            mWeeklyAccumulators.info

    override val cardName: String
        get() = NAME

    companion object {
        private const val APP_ID = 0xaf1122
        internal const val LEAP_STR = "tfi_leap"

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_ireland,
                cardType = CardType.MifareDesfire,
                imageId = R.drawable.leap_card,
                iOSSupported = false,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                resourceExtraNote = R.string.card_note_leap,
                region = TransitRegion.IRELAND,
                preview = true)

        private const val BLOCK_SIZE = 0x180
        private val LEAP_EPOCH = Epoch.utc(1997, MetroTimeZone.DUBLIN)

        private fun parse(card: DesfireCard): TransitData {
            val app = card.getApplication(APP_ID)
            if (app!!.getFile(2) is UnauthorizedDesfireFile) {
                return LockedLeapTransitData()
            }
            val file2 = app.getFile(2)!!.data

            val file4 = app.getFile(4)!!.data

            val file6 = app.getFile(6)!!.data

            val balanceBlock = chooseBlock(file6, 6)
            // 1 byte unknown
            val mInitDate = parseDate(file6, balanceBlock + 1)
            val mExpiryDate = mInitDate.plus(Duration.yearsLocal(12))
            // 1 byte unknown

            // offset: 0xc

            //offset 0x20
            val trips = mutableListOf<LeapTrip?>()

            trips.add(LeapTrip.parseTopup(file6, 0x20))
            trips.add(LeapTrip.parseTopup(file6, 0x35))
            trips.add(LeapTrip.parseTopup(file6, 0x20 + BLOCK_SIZE))
            trips.add(LeapTrip.parseTopup(file6, 0x35 + BLOCK_SIZE))

            trips.add(LeapTrip.parsePurseTrip(file6, 0x80))
            trips.add(LeapTrip.parsePurseTrip(file6, 0x90))
            trips.add(LeapTrip.parsePurseTrip(file6, 0x80 + BLOCK_SIZE))
            trips.add(LeapTrip.parsePurseTrip(file6, 0x90 + BLOCK_SIZE))

            app.getFile(9)?.data?.also { file9 ->
                for (i in 0..6)
                    trips.add(LeapTrip.parseTrip(file9, 0x80 * i))
            }

            val capBlock = chooseBlock(file6, 0xa6)

            return LeapTransitData(
                    serialNumber = getSerial(card),
                    mBalance = parseBalance(file6, balanceBlock + 9),
                    trips = LeapTrip.postprocess(trips),
                    mExpiryDate = mExpiryDate,
                    mInitDate = mInitDate,
                    mIssueDate = parseDate(file4, 0x22),
                    mIssuerId = file2.byteArrayToInt(0x22, 3),
                    // offset 0x140
                    mDailyAccumulators = AccumulatorBlock(file6, capBlock + 0x140),
                    mWeeklyAccumulators = AccumulatorBlock(file6, capBlock + 0x160)
            )
        }


        private fun chooseBlock(file: ImmutableByteArray, txidoffset: Int): Int {
            val txIdA = file.byteArrayToInt(txidoffset, 2)
            val txIdB = file.byteArrayToInt(BLOCK_SIZE + txidoffset, 2)

            return if (txIdA > txIdB) {
                0
            } else BLOCK_SIZE
        }

        fun parseBalance(file: ImmutableByteArray, offset: Int): Int {
            return file.getBitsFromBufferSigned(offset * 8, 24)
        }

        fun parseDate(file: ImmutableByteArray, offset: Int): TimestampFull {
            val sec = file.byteArrayToInt(offset, 4)
            return LEAP_EPOCH.seconds(sec.toLong())
        }

        private fun getSerial(card: DesfireCard): String {
            val app = card.getApplication(APP_ID)
            val serial = app!!.getFile(2)!!.data.byteArrayToInt(0x25, 4)
            val initDate = parseDate(app.getFile(6)!!.data, 1)
            // luhn checksum of number without date is always 6
            val checkDigit = (NumberUtils.calculateLuhn(serial.toString()) + 6) % 10
            return (NumberUtils.formatNumber(serial.toLong(), " ", 5, 4) + checkDigit + " "
                    + NumberUtils.zeroPad(initDate.monthNumberOneBased, 2) +
                    NumberUtils.zeroPad((initDate.year) % 100, 2))
        }

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity = try {
                TransitIdentity(NAME, getSerial(card))
            } catch (e: Exception) {
                TransitIdentity(
                    Localizer.localizeString(R.string.locked_leap), null)
            }

            override fun createUnlocker(appIds: Int, manufData: ImmutableByteArray) = createUnlockerDispatch(appIds, manufData)
        }

        fun earlyCheck(appId: Int): Boolean {
            return appId == APP_ID
        }
    }
}
