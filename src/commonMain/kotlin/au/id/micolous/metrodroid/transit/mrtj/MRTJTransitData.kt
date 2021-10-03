/*
 * MRTJTransitData.kt
 *
 * Handling MRT Jakarta multi trip card data.
 *
 * Copyright 2019 Bondan Sumbodo <sybond@gmail.com>
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

package au.id.micolous.metrodroid.transit.mrtj

import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class MRTJTransitData(
        private val mCurrentBalance: Int,
        private val mTransactionCounter: Int,
        private val mLastTransAmount: Int,
        override val serialNumber: String?) : TransitData() {

    public override val balance: TransitCurrency?
        get() = TransitCurrency.IDR(mCurrentBalance)

    override val cardName: String
        get() = NAME

    override val info: List<ListItem>?
        get() = listOfNotNull(
                HeaderListItem(R.string.kmt_other_data),
                if (!Preferences.hideCardNumbers) {
                    ListItem(R.string.transaction_counter, mTransactionCounter.toString())
                } else null,
                ListItem(R.string.kmt_last_trx_amount,
                        TransitCurrency.IDR(mLastTransAmount).maybeObfuscateFare().formatCurrencyString(false)))

    companion object {
        // defines
        private const val NAME = "Kartu Jelajah Berganda MRTJ"

        const val SYSTEMCODE_MRTJ = 0x9373
        // TODO:
        // - Figure out what is content of 0x100B, assumed it is the card ID (encrypted or encoded)
        //
        //   Content of 0x100B: 28010101 264a0001 00000000 00000000
        //   Printed Card Num : MJ01 1190 2100 3733
        //   S.N              : JK0247709483
        //

        const val SERVICE_MRTJ_ID = 0x100B

        const val SERVICE_MRTJ_BALANCE = 0x10D7

        private fun parse(card: FelicaCard): MRTJTransitData {
            val serviceBalance = card.getSystem(SYSTEMCODE_MRTJ)?.getService(SERVICE_MRTJ_BALANCE)
            val blocksBalance = serviceBalance?.blocks
            val blockBalance = blocksBalance?.get(0)
            val dataBalance = blockBalance?.data
            val mCurrentBalance = dataBalance?.byteArrayToIntReversed(0, 4) ?: 0
            val mTransactionCounter = dataBalance?.byteArrayToInt(13, 3) ?: 0
            val mLastTransAmount = dataBalance?.byteArrayToIntReversed(4, 4) ?: 0

            return MRTJTransitData(mCurrentBalance = mCurrentBalance,
                    mTransactionCounter = mTransactionCounter, mLastTransAmount = mLastTransAmount, serialNumber = "")
        }

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.mrtj_card,
                name = NAME,
                locationId = R.string.location_jakarta,
                cardType = CardType.FeliCa,
                region = TransitRegion.INDONESIA,
                resourceExtraNote = R.string.mrtj_extra_note)


        val FACTORY: FelicaCardTransitFactory = object : FelicaCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(systemCodes: List<Int>) = SYSTEMCODE_MRTJ in systemCodes

            override fun parseTransitData(card: FelicaCard) = parse(card)

            override fun parseTransitIdentity(card: FelicaCard) = TransitIdentity(NAME,
                    "")
        }
    }
}
