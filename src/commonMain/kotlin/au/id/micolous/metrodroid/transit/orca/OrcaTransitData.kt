/*
 * OrcaTransitData.kt
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
 * Copyright 2015 Sean CyberKitsune McClenaghan
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package au.id.micolous.metrodroid.transit.orca

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*

@Parcelize
class OrcaTransitData (private val mSerialNumber: Int?,
                       private val mBalance: Int?,
                       override val trips: List<Trip>): TransitData() {

    override val cardName: String
        get() = "ORCA"

    public override val balance: TransitCurrency?
        get() = if (mBalance != null) TransitCurrency.USD(mBalance) else null

    override val serialNumber: String?
        get() = mSerialNumber?.toString()

    companion object {
        internal const val AGENCY_CT = 0x02
        internal const val AGENCY_KCM = 0x04
        internal const val AGENCY_ST = 0x07
        internal const val AGENCY_WSF = 0x08

        const val APP_ID = 0x3010f2

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.orca_card,
                name = "ORCA",
                locationId = R.string.location_seattle,
                cardType = CardType.MifareDesfire)

        private fun parseTrips(card: DesfireCard, fileId: Int, isTopup: Boolean): List<TransactionTripAbstract> {
            val file = card.getApplication(APP_ID)?.getFile(fileId) as? RecordDesfireFile ?: return emptyList()
            val useLog = file.records.map { OrcaTransaction(it, isTopup) }
            return TransactionTrip.merge(useLog)
        }

        private fun parse(desfireCard: DesfireCard): OrcaTransitData {
            val mSerialNumber = desfireCard.getApplication(0xffffff)?.getFile(0x0f)?.data?.byteArrayToInt(4, 4)

            val mBalance = desfireCard.getApplication(APP_ID)?.getFile(0x04)?.data?.byteArrayToInt(41, 2)

            val trips = parseTrips(desfireCard, 2, false) + parseTrips(desfireCard, 3, true)

            return OrcaTransitData(trips = trips, mBalance = mBalance, mSerialNumber = mSerialNumber)
        }

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity {
                try {
                    val data = card.getApplication(0xffffff)?.getFile(0x0f)?.data
                    return TransitIdentity("ORCA", data?.byteArrayToInt(4, 4)?.toString())
                } catch (ex: Exception) {
                    throw RuntimeException("Error parsing ORCA serial", ex)
                }

            }
        }
    }

}
