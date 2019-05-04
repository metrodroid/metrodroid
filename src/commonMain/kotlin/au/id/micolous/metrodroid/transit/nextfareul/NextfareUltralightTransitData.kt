/*
 * TroikaUltralightTransitData.java
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
package au.id.micolous.metrodroid.transit.nextfareul

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class NextfareUltralightTransitDataCapsule (
        val mProductCode: Int,
        val mSerial: Long,
        val mType: Byte,
        val mBaseDate: Int,
        val mMachineCode: Int,
        val mExpiry: Int,
        val mBalance: Int,
        val trips: List<TransactionTripAbstract>): Parcelable

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
abstract class NextfareUltralightTransitData : TransitData() {

    abstract val timeZone: MetroTimeZone

    abstract val capsule: NextfareUltralightTransitDataCapsule

    public override val balance: TransitBalance?
        get() = TransitBalanceStored(
                makeCurrency(capsule.mBalance),
                parseDateTime(timeZone, capsule.mBaseDate, capsule.mExpiry, 0))

    override val serialNumber: String?
        get() = formatSerial(capsule.mSerial)

    override val info: List<ListItem>?
        get() {
            val items = mutableListOf<ListItem>()
            if (capsule.mType.toInt() == 8)
                items.add(ListItem(R.string.ticket_type, R.string.compass_ticket_type_concession))
            else
                items.add(ListItem(R.string.ticket_type, R.string.compass_ticket_type_regular))

            val productName = getProductName(capsule.mProductCode)
            if (productName != null)
                items.add(ListItem(R.string.compass_product_type, productName))
            else
                items.add(ListItem(R.string.compass_product_type, capsule.mProductCode.toString(16)))
            items.add(ListItem(R.string.compass_machine_code, capsule.mMachineCode.toString(16)))
            return items
        }

    protected abstract fun makeCurrency(value: Int): TransitCurrency

    protected abstract fun getProductName(productCode: Int): String?

    companion object {
        fun parse(card: UltralightCard,
                  makeTransaction : (raw: ImmutableByteArray, baseDate: Int) -> NextfareUltralightTransaction): NextfareUltralightTransitDataCapsule {
            val page0 = card.getPage(4).data
            val page1 = card.getPage(5).data
            val page3 = card.getPage(7).data
            val lowerBaseDate = page0[3].toInt() and 0xff
            val upperBaseDate = page1[0].toInt() and 0xff
            val mBaseDate = upperBaseDate shl 8 or lowerBaseDate
            val transactions = listOf(8, 12).filter { isTransactionValid(card,it) }.map {
                makeTransaction(card.readPages(it, 4), mBaseDate) }
            var trLater: NextfareUltralightTransaction? = null
            for (tr in transactions)
                if (trLater == null || tr.isSeqNoGreater(trLater))
                    trLater = tr
            return NextfareUltralightTransitDataCapsule(
                    mExpiry = trLater?.expiry ?: 0,
                    mBalance = trLater?.balance ?: 0,
                    trips = TransactionTrip.merge(transactions),
                    mBaseDate = mBaseDate,
                    mSerial = getSerial(card),
                    mType = page0[1],
                    mProductCode = page1[2].toInt() and 0x7f,
                    mMachineCode = page3.byteArrayToIntReversed(0, 2)
            )
        }

        private fun isTransactionValid(card: UltralightCard, startPage: Int): Boolean {
            return !card.readPages(startPage, 3).isAllZero()
        }

        fun getSerial(card: UltralightCard): Long {
            val manufData0 = card.getPage(0).data
            val manufData1 = card.getPage(1).data
            val uid = manufData0.byteArrayToLong(1, 2) shl 32 or manufData1.byteArrayToLong(0, 4)
            val serial = uid + 1000000000000000L
            val luhn = NumberUtils.calculateLuhn(serial.toString())
            return serial * 10 + luhn
        }

        fun formatSerial(serial: Long): String {
            return NumberUtils.formatNumber(serial, " ", 4, 4, 4, 4, 4)
        }

        fun parseDateTime(tz: MetroTimeZone, baseDate: Int, date: Int, time: Int): Timestamp {
            val t = TimestampFull(tz, (baseDate shr 9) + 2000,
                    (baseDate shr 5 and 0xf) - 1,
                    baseDate and 0x1f, 0, 0, 0)
            return t + Duration.daysLocal(-date) + Duration.mins(time)
        }
    }
}
