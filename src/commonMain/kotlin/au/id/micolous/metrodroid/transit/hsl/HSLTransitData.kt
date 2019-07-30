/*
 * HSLTransitData.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireApplication
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Implements a reader for HSL transit cards.
 *
 * Documentation and sample libraries for this are available at:
 * http://dev.hsl.fi/#travel-card
 *
 * The documentation (in Finnish) is available at:
 * http://dev.hsl.fi/hsl-card-java/HSL-matkakortin-kuvaus.pdf
 *
 * Machine translation to English:
 * https://translate.google.com/translate?sl=auto&tl=en&js=y&prev=_t&hl=en&ie=UTF-8&u=http%3A%2F%2Fdev.hsl.fi%2Fhsl-card-java%2FHSL-matkakortin-kuvaus.pdf&edit-text=&act=url
 */
@Parcelize
class HSLTransitData(override val serialNumber: String?,
                     private val mBalance: Int,
                     override val trips: List<Trip>,
                     override val subscriptions: List<Subscription>?,
                     val applicationVersion: Int?,
                     val applicationKeyVersion: Int?,
                     val platformType: Int?,
                     val securityLevel: Int?) : TransitData() {

    override fun getRawFields(level: RawLevel): List<ListItem> = super.getRawFields(level).orEmpty() + listOf(
            ListItem("Application version", applicationVersion.toString()),
            ListItem("Application key version", applicationKeyVersion.toString()),
            ListItem("Platform type", platformType.toString()),
            ListItem("Security Level", securityLevel.toString())
    )

    override val cardName: String
        get() = "HSL"

    public override val balance: TransitCurrency?
        get() = TransitCurrency.EUR(mBalance)

    companion object {
        private fun parseTrips(app: DesfireApplication, version: Int): List<HSLTransaction> {
            val recordFile = app.getFile(0x04) as? RecordDesfireFile ?: return listOf()
            return recordFile.records.mapNotNull { HSLTransaction.parseLog(it, version) }
        }

        private fun addEmbedTransaction(trips: MutableList<HSLTransaction>, embed: HSLTransaction) {
            val sameIdx = trips.indices.find { idx -> trips[idx].timestamp == embed.timestamp }
            if (sameIdx != null) {
                val same = trips[sameIdx]
                trips.removeAt(sameIdx)
                trips.add(HSLTransaction.merge(same, embed))
            } else {
                trips.add(embed)
            }
        }

        private fun parse(app: DesfireApplication, version: Int): HSLTransitData {
            val appInfo = app.getFile(0x08)?.data
            val serialNumber = appInfo?.toHexString()?.substring(2, 20)?.let { formatSerial(it) }

            val balData = app.getFile(0x02)!!.data
            val mBalance = balData.getBitsFromBuffer(0, 20)
            val mLastRefill = HSLRefill.parse(balData)

            val trips = parseTrips(app, version).toMutableList()

            val arvo = app.getFile(0x03)?.data?.let { HSLArvo.parse(it, version) }

            val kausi = app.getFile(0x01)?.data?.let { HSLKausi.parse(it, version) }

            arvo?.lastTransaction?.let { addEmbedTransaction(trips, it) }

            kausi?.transaction?.let { addEmbedTransaction(trips, it) }

            return HSLTransitData(serialNumber = serialNumber,
                    subscriptions = kausi?.subs.orEmpty() + listOfNotNull(arvo),
                    mBalance = mBalance,
                    //Read data from application info
                    applicationVersion = appInfo?.getBitsFromBuffer(0, 4),
                    applicationKeyVersion = appInfo?.getBitsFromBuffer(4, 4),
                    platformType = appInfo?.getBitsFromBuffer(80, 3),
                    securityLevel = appInfo?.getBitsFromBuffer(83, 1),
                    trips = TransactionTrip.merge(trips + listOfNotNull(mLastRefill)))
        }

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.hsl_card,
                name = "HSL",
                locationId = R.string.location_helsinki_finland,
                resourceExtraNote = R.string.hsl_extra_note,
                cardType = CardType.MifareDesfire)

        const val APP_ID_V1 = 0x1120ef
        const val APP_ID_V2 = 0x1420ef

        fun formatSerial(input: String) = input.let { NumberUtils.groupString(it, " ", 6, 4, 4) }

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID_V1 in appIds || APP_ID_V2 in appIds

            override fun parseTransitData(card: DesfireCard) = card.getApplication(APP_ID_V1)?.let { parse(it, 1) } ?:
            card.getApplication(APP_ID_V2)?.let { parse(it, 2) }

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity {
                val data = card.getApplication(APP_ID_V1)?.getFile(0x08)?.data
                        ?: card.getApplication(APP_ID_V2)?.getFile(0x08)?.data
                return TransitIdentity("HSL", data?.toHexString()?.substring(2, 20)?.let { formatSerial(it) })
            }
        }
    }
}
