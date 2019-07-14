/*
 * GautrainTransitData.kt
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

package au.id.micolous.metrodroid.transit.gautrain

import au.id.micolous.metrodroid.card.CardType

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger
import au.id.micolous.metrodroid.transit.ovc.OVChipIndex
import au.id.micolous.metrodroid.transit.ovc.OVChipSubscription
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

private const val NAME = "Gautrain"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_gauteng,
        imageId = R.drawable.gautrain,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        cardType = CardType.MifareClassic,
        keysRequired = true)

private fun formatSerial(serial: Long) = NumberUtils.zeroPad(serial, 10)

private fun getSerial(card: ClassicCard) = card[0,0].data.byteArrayToLong(0, 4)

@Parcelize
data class GautrainBalanceBlock(val mBalance: Int,
                                val mTxn: Int,
                                private val mTxnRefill: Int,
                                private val mA: Int,
                                private val mB: Int,
                                private val mC: Int): Parcelable {
    companion object {
        fun parse(input: ImmutableByteArray): GautrainBalanceBlock =
                GautrainBalanceBlock(
                        mA = input.getBitsFromBuffer(0, 30),
                        mTxn = input.getBitsFromBuffer(30, 16),
                        mB = input.getBitsFromBuffer(46, 12),
                        mTxnRefill = input.getBitsFromBuffer(58, 16),
                        mC = input.getBitsFromBuffer(74, 1),
                        mBalance = input.getBitsFromBufferSigned(75, 16) xor 0x7fff.inv())
    }
}

@Parcelize
data class GautrainTransitData(private val mSerial: Long,
                               override val trips: List<Trip>,
                               override val subscriptions: List<GautrainSubscription>,
                               private val mExpdate: Int,
                               private val mBalances: List<GautrainBalanceBlock>,
                               private val mIndex: OVChipIndex) : TransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val balance: TransitBalance?
        get() = TransitBalanceStored(GautrainLookup.parseCurrency(mBalances.maxBy { it.mTxn }?.mBalance ?: 0),
                En1545FixedInteger.parseDate(mExpdate, GautrainLookup.timeZone))

    override fun getRawFields(level: RawLevel): List<ListItem>? = mBalances.mapIndexed {
        idx, bal -> ListItem("Bal $idx", bal.toString())
    } + mIndex.getRawFields(level)
}

object GautrainTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, formatSerial(getSerial(card)))

    override fun parseTransitData(card: ClassicCard): GautrainTransitData {
        val index = OVChipIndex.parse(card[39].readBlocks(11, 4))
        val transactions = (35..38).flatMap { sector -> (0..12 step 2).map {
            block -> card[sector].readBlocks(block, 2) } }.mapNotNull { GautrainTransaction.parse(it) }
        val trips = TransactionTripLastPrice.merge(transactions)
        return GautrainTransitData(
                mSerial = getSerial(card),
                trips = trips,
                mExpdate = card[0, 1].data.getBitsFromBuffer(88, 20),
                mBalances = listOf(card[39][9].data, card[39][10].data).map { GautrainBalanceBlock.parse(it) },
                subscriptions = OVChipTransitData.getSubscriptions(card, index, GautrainSubscription.Companion::parse),
                mIndex = index)
    }

    private val GAU_HEADER = ImmutableByteArray.fromHex("b180000006b55c0013aee4")

    override fun earlyCheck(sectors: List<ClassicSector>) =
            sectors[0].readBlocks(1, 1).copyOfRange(0, 11) == GAU_HEADER

    override val earlySectors get() = 2
}
