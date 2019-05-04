/*
 * KMTTransitData.java
 *
 * Copyright 2018 Bondan Sumbodo <sybond@gmail.com>
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

package au.id.micolous.metrodroid.transit.kmt

import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
class KMTTransitData (override val trips: List<KMTTrip>,
                      override val serialNumber: String?,
                      private val mCurrentBalance: Int,
                      private val mTransactionCounter: Int,
                      private val mLastTransAmount: Int): TransitData() {

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
        private const val NAME = "Kartu Multi Trip"
        const val SYSTEMCODE_KMT = 0x90b7
        const val SERVICE_KMT_ID = 0x300B
        const val SERVICE_KMT_BALANCE = 0x1017
        const val SERVICE_KMT_HISTORY = 0x200F
        val KMT_EPOCH = Epoch.utc(2000, MetroTimeZone.JAKARTA, -6 * 60)

        private fun parse(card: FelicaCard): KMTTransitData {
            val serialNumber = getSerial(card)
            val serviceBalance = card.getSystem(SYSTEMCODE_KMT)?.getService(SERVICE_KMT_BALANCE)
            val blocksBalance = serviceBalance?.blocks
            val blockBalance = blocksBalance?.get(0)
            val dataBalance = blockBalance?.data
            val mCurrentBalance = dataBalance?.byteArrayToIntReversed(0, 4) ?: 0
            val mTransactionCounter = dataBalance?.byteArrayToInt(13, 3) ?: 0
            val mLastTransAmount = dataBalance?.byteArrayToIntReversed(4, 4) ?: 0

            val trips = card.getSystem(SYSTEMCODE_KMT)
                    ?.getService(SERVICE_KMT_HISTORY)?.blocks
                    ?.mapNotNull { block ->
                        if (block.data[0].toInt() != 0 && block.data.byteArrayToInt(8, 2) != 0) {
                            KMTTrip.parse(block)
                        } else
                            null
                    }
            return KMTTransitData(trips = trips.orEmpty(), serialNumber = serialNumber, mCurrentBalance = mCurrentBalance,
                    mTransactionCounter = mTransactionCounter, mLastTransAmount = mLastTransAmount)
        }

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.kmt_card,
                name = KMTTransitData.NAME,
                locationId = R.string.location_jakarta,
                cardType = CardType.FeliCa,
                resourceExtraNote = R.string.kmt_extra_note)

        private fun getSerial(card: FelicaCard): String? {
            val dataID = card.getSystem(SYSTEMCODE_KMT)?.getService(SERVICE_KMT_ID)
                    ?.blocks?.get(0)?.data ?: return null
            return if (dataID.isASCII())
                dataID.readASCII()
            else
                dataID.toHexString()
        }

        val FACTORY: FelicaCardTransitFactory = object : FelicaCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(systemCodes: List<Int>) = SYSTEMCODE_KMT in systemCodes

            override fun parseTransitData(card: FelicaCard) = parse(card)

            override fun parseTransitIdentity(card: FelicaCard) = TransitIdentity(NAME,
                    card.getSystem(SYSTEMCODE_KMT)?.getService(SERVICE_KMT_ID)?.
                            getBlock(0)?.data?.readASCII() ?: "-")
        }
    }
}

