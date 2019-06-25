/*
 * IntercardTransitData.kt
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
package au.id.micolous.metrodroid.transit.intercard

import au.id.micolous.metrodroid.transit.*

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem

@Parcelize
class IntercardTransitData (
        private val mSerialNumber: Long,
        private val mBalance: Int?, // 10th of cents
        private val mLastTransaction: Int?
): TransitData() {

    override val cardName: String
        get() = NAME

    public override val balance
        get() = mBalance?. let { parseCurrency(it) }

    override val serialNumber: String?
        get() = mSerialNumber.toString()

    override val info: List<ListItem>? get() = listOfNotNull(
        mLastTransaction?.let { ListItem(R.string.last_transaction, parseCurrency(mLastTransaction).formatCurrencyString(true))}
    )

    companion object {
        const val NAME = "Intercard"
        const val APP_ID_BALANCE = 0x5f8415

        private val CARD_INFO = CardInfo(
                name = NAME,
                locationId = R.string.location_germany_and_switzerland,
                imageId = R.drawable.logo_intercard,
                cardType = CardType.MifareDesfire)

		// FIXME: Apparently this system may be either in euro or in Swiss Francs. Unfortunately
		// Swiss Franc one still has string "EUR" in file 0, so this suggests a lazy adaptation.
		// I don't have samples from Germany to compare.
		// File 7 in application 0x5f8405 seems to contain
		// some useful data including maybe university ID but
		// I can't know without further samples
        private fun parseCurrency(input: Int): TransitCurrency = TransitCurrency(input / 10, "CHF")

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID_BALANCE in appIds

            override fun parseTransitData(card: DesfireCard): IntercardTransitData? {
                val file1 = card.getApplication(APP_ID_BALANCE)?.getFile(1)
                val balance = file1?.data?.byteArrayToIntReversed()
                return IntercardTransitData(mBalance = balance,
                        mLastTransaction = (file1?.fileSettings as? ValueDesfireFileSettings)?.limitedCreditValue,
                        mSerialNumber = card.tagId.byteArrayToLongReversed()
                )
            }

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity =
                    TransitIdentity(NAME, card.tagId.byteArrayToLongReversed().toString())
        }
    }
}
