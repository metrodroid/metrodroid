/*
 * EZLinkTransitData.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
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

package au.id.micolous.metrodroid.transit.ezlink

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.cepas.CEPASApplication
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class EZLinkTransitData (override val serialNumber: String?,
                         private val mBalance: Int?,
                         override val trips: List<EZLinkTrip>): TransitData() {
    override val cardName: String
        get() = getCardIssuer(serialNumber)

    // This is stored in cents of SGD
    public override val balance: TransitCurrency?
        get() = if (mBalance != null) TransitCurrency.SGD(mBalance) else null

    companion object {
        fun parse(cepasCard: CEPASApplication) : EZLinkTransitData {
            val purse = cepasCard.getPurse(3) ?: return EZLinkTransitData(
                    serialNumber = null,
                    mBalance = null,
                    trips = parseTrips(cepasCard, "CEPAS")
            )
            val canNo = CEPASPurse(purse).can.toHexString()
            return EZLinkTransitData(
                    serialNumber = canNo,
                    mBalance = CEPASPurse(purse).purseBalance,
                    trips = parseTrips(cepasCard, getCardIssuer(canNo))
            )
        }

        private fun parseTrips(card: CEPASApplication, cardName : String): List<EZLinkTrip> {
            val history = CEPASHistory(card.getHistory(3) ?: return emptyList())
            val transactions = history.transactions
            return transactions.map { EZLinkTrip(it, cardName) }
        }

        private const val EZLINK_STR = "ezlink"

        val EZ_LINK_CARD_INFO = CardInfo(
                imageId = R.drawable.ezlink_card,
                name = "EZ-Link",
                locationId = R.string.location_singapore,
                cardType = CardType.CEPAS)

        private val NETS_FLASHPAY_CARD_INFO = CardInfo(
                imageId = R.drawable.nets_card,
                name = "NETS FlashPay",
                locationId = R.string.location_singapore,
                cardType = CardType.CEPAS)
        val ALL_CARD_INFOS = listOf(
                EZLinkTransitData.EZ_LINK_CARD_INFO,
                EZLinkTransitData.NETS_FLASHPAY_CARD_INFO
        )

        private val EPOCH = Epoch.utc(1995, MetroTimeZone.SINGAPORE, -8 * 60)

        fun timestampToCalendar(timestamp: Long) = EPOCH.seconds(timestamp)

        internal fun daysToCalendar(days: Int) = EPOCH.days(days)

        fun getCardIssuer(canNo: String?) = when (canNo?.substring(0, 3)) {
                "100" -> "EZ-Link"
                "111" -> "NETS"
                else -> "CEPAS"
            }

        fun earlyCardInfo(purseData: ImmutableByteArray): CardInfo {
            val canNo = purseData.sliceOffLen(8, 8).toHexString()
            return when (canNo.substring(0, 3)) {
                "100" -> EZ_LINK_CARD_INFO
                "111" -> NETS_FLASHPAY_CARD_INFO
                else -> EZ_LINK_CARD_INFO
            }
        }

        fun getStation(code: String): Station {
            return if (code.length != 3) Station.unknown(code) else StationTableReader.getStation(EZLINK_STR,
                    ImmutableByteArray.fromASCII(code).byteArrayToInt(), code)
        }

        fun check(cepasCard: CEPASApplication): Boolean {
            return cepasCard.getPurse(3) != null
        }

        fun parseTransitIdentity(card: CEPASApplication): TransitIdentity {
            val purseRaw = card.getPurse(3) ?: return TransitIdentity("CEPAS", null)
            val purse = CEPASPurse(purseRaw)
            val canNo = purse.can.toHexString()
            return TransitIdentity(getCardIssuer(canNo), canNo)
        }

        val notice: String?
            get() = StationTableReader.getNotice(EZLINK_STR)
    }
}
