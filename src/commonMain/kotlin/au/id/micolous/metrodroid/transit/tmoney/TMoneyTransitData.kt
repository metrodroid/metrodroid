/*
 * TmoneyTransitData.java
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

package au.id.micolous.metrodroid.transit.tmoney


import au.id.micolous.metrodroid.transit.*

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class TMoneyTransitData (override val serialNumber: String?,
                         private val mBalance: Int,
                         private val mDate: String?,
                         override val trips: List<TMoneyTrip>): TransitData() {

    public override val balance: TransitCurrency?
        get() = TransitCurrency.KRW(mBalance)

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_tmoney)

    override val info: List<ListItem>?
        get() = listOf(ListItem(R.string.tmoney_date, mDate))

    constructor(tMoneyCard: TMoneyCard) : this(
        serialNumber = parseSerial(tMoneyCard),
        mBalance = tMoneyCard.balance.byteArrayToInt(),
        mDate = parseDate(tMoneyCard),
        trips = tMoneyCard.transactionRecords?.mapNotNull { TMoneyTrip.parseTrip(it) }.orEmpty()
    )

    companion object {
        val CARD_INFO = CardInfo(
                imageId = R.drawable.tmoney_card,
                name = Localizer.localizeString(R.string.card_name_tmoney),
                locationId = R.string.location_seoul,
                cardType = CardType.ISO7816,
                preview = true)

        fun parseTransitIdentity(card: TMoneyCard): TransitIdentity {
            return TransitIdentity(Localizer.localizeString(R.string.card_name_tmoney), parseSerial(card))
        }

        private fun getSerialTag(card: TMoneyCard): ImmutableByteArray? {
            return ISO7816TLV.findBERTLV(card.appFci ?: return null, "b0", false)
        }

        private fun parseSerial(card: TMoneyCard): String? {
            return NumberUtils.groupString(
                    getSerialTag(card)?.getHexString(4, 8) ?: return null,
                    " ", 4, 4, 4)
        }

        private fun parseDate(card: TMoneyCard): String? {
            val tmoneytag = getSerialTag(card) ?: return null
            return (tmoneytag.getHexString(17, 2) + "/"
                    + tmoneytag.getHexString(19, 1))
        }
    }
}
