/*
 * BonobusTransitData.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.bonobus

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
data class BonobusTransitData(private val mSerial: Long, private val mBalance: Int,
                              override val trips: List<BonobusTrip>,
                              private val mHeader: ImmutableByteArray,
                              private val mIssueDate: Int,
                              private val mExpiryDate: Int,
                              private val mJ: Int) : TransitData() {
    override val serialNumber get() = mSerial.toString()

    override val cardName get() = BonobusTransitFactory.NAME

    override val balance get() = TransitBalanceStored(
            TransitCurrency.EUR(mBalance),
            name = null,
            validFrom = parseDate(mIssueDate),
            validTo = parseDate(mExpiryDate))

    override fun getRawFields(level: RawLevel)= listOf(
            ListItem(FormattedString("Header"), mHeader.toHexDump()),
            ListItem("J", NumberUtils.intToHex(mJ)))

    companion object {
        fun parseDate(input: Int) =
                Daystamp(
                        (input shr 9) + 2000,
                        ((input shr 5) and 0xf) - 1,
                        input and 0x1f
                )

        fun getSerial(card: ClassicCard) = card[0,0].data.byteArrayToLongReversed(0, 4)

        fun parse(card: ClassicCard): BonobusTransitData {
            val trips = (7..15).flatMap { sec -> card[sec].blocks.dropLast(1) }.mapNotNull { BonobusTrip.parse(it.data) }
            return BonobusTransitData(
                    mSerial = getSerial(card),
                    trips = trips,
                    mBalance = card[4, 0].data.byteArrayToIntReversed(0, 4),
                    mIssueDate = card[0, 2].data.byteArrayToInt(10, 2),
                    mExpiryDate = card[0, 2].data.byteArrayToInt(12, 2),
                    mHeader = card[0, 2].data.sliceOffLen(0, 10),
                    mJ = card[0, 2].data.byteArrayToInt(14, 2))
        }
    }
}
