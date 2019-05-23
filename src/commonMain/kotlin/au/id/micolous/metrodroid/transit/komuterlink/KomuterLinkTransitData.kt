/*
 * KomuterLinkTransitData.kt
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

package au.id.micolous.metrodroid.transit.komuterlink

import au.id.micolous.metrodroid.card.CardType

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

private const val NAME = "KomuterLink"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_malaysia,
        cardType = CardType.MifareClassic,
        keysRequired = true)

private fun parseTimestamp(input: ImmutableByteArray, off: Int): TimestampFull {
    val hour = input.getBitsFromBuffer(off * 8, 5)
    val min = input.getBitsFromBuffer(off * 8 + 5, 6)
    val y = input.getBitsFromBuffer(off * 8 + 17, 6) + 2000
    val month = input.getBitsFromBuffer(off * 8 + 23, 4)
    val d = input.getBitsFromBuffer(off * 8 + 27, 5)
    return TimestampFull(tz = MetroTimeZone.KUALA_LUMPUR, year = y, month = month - 1, day = d, hour = hour,
            min = min)
}

@Parcelize
data class KomuterLinkTrip(private val mAmount: Int, private val mNewBalance: Int,
                           override val startTimestamp: TimestampFull,
                           override val mode: Mode,
                           private val mTransactionId: Int) : Trip() {
    override val fare: TransitCurrency?
        get() = TransitCurrency(mAmount, "MYR")

    companion object {
        fun parse (sec: ClassicSector, sign: Int, mode: Mode): KomuterLinkTrip? {
            if (sec[0].isEmpty)
                return null
            return KomuterLinkTrip(
                    mAmount = sec[0].data.byteArrayToInt(10, 2) * sign,
                    // zeros here, probably part of new balance
                    mNewBalance = sec[0].data.byteArrayToInt(14, 2),
                    startTimestamp = parseTimestamp(sec[0].data, 0),
                    mode = mode,
                    mTransactionId = sec[0].data.byteArrayToInt(4, 2)
            )
        }
    }
}

@Parcelize
data class KomuterLinkTransitData(private val mBalance: Int, private val mSerial: Long,
                                  private val mIssueTimestamp: TimestampFull,
                                  override val trips: List<KomuterLinkTrip>,
                                  private val mCardNo: Int,
                                  private val mStoredLuhn: Int) : TransitData() {
    override val serialNumber: String?
        get() = NumberUtils.zeroPad(mSerial, 10)
    override val cardName get() = NAME

    override val balance get() = TransitCurrency(mBalance, "MYR")

    override val info: List<ListItem>?
        get() {
            // Prefix may be wrong as CardNo is not printed anywhere
            val partialCardNo = "1" + NumberUtils.zeroPad(mCardNo, 10)
            val cardNo = partialCardNo + NumberUtils.calculateLuhn(partialCardNo)
            return listOf(
                    ListItem("CardNo", cardNo),
                    ListItem(R.string.issue_date, mIssueTimestamp.format()))
        }
}

class KomuterLinkTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, NumberUtils.zeroPad(card[0, 0].data.byteArrayToLongReversed(0, 4), 10))

    override fun parseTransitData(card: ClassicCard): KomuterLinkTransitData {
        return KomuterLinkTransitData(
                mBalance = card[2, 0].data.byteArrayToIntReversed(0, 4),
                mSerial = card[0, 0].data.byteArrayToLongReversed(0, 4),
                mIssueTimestamp = parseTimestamp(card[1,0].data, 5),
                mCardNo = card[0,2].data.byteArrayToInt(4, 4),
                mStoredLuhn = card[0,2].data[8].toInt() and 0xff,
                trips = listOfNotNull(
                        KomuterLinkTrip.parse(card[4], -1, Trip.Mode.TICKET_MACHINE),
                        KomuterLinkTrip.parse(card[7], +1, Trip.Mode.TRAIN)
                        ))
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            sectors[0][1].data == ImmutableByteArray.fromHex("0f0102030405060708090a0b0c0d0e0f")

    override val earlySectors get() = 1
}
