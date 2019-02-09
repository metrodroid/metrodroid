/*
 * ChcMetrocardTransitData.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.chc_metrocard

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord
import java.util.*

/**
 * Transit data type for pre-2016 Metrocard (Christchurch, NZ).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * The post-2016 version of this card is a DESFire card made by INIT.
 *
 * Documentation: https://github.com/micolous/metrodroid/wiki/Metrocard-%28Christchurch%29
 */
class ChcMetrocardTransitData private constructor(card: ClassicCard) :
        ErgTransitData(card, CURRENCY) {

    override fun newTrip(purse: ErgPurseRecord, epoch: GregorianCalendar?) =
            ChcMetrocardTransaction(purse, epoch!!)

    override fun getCardName() = NAME

    override fun formatSerialNumber(metadataRecord: ErgMetadataRecord) =
            internalFormatSerialNumber(metadataRecord)

    override fun getTimezone(): TimeZone = TIME_ZONE

    override fun getBalance(): TransitBalance? {
        val b = super.getBalance() ?: return null

        var expiry = lastUseTimestamp
        if (expiry != null) {
            // Cards not used for 3 years will expire
            expiry = expiry.clone() as Calendar
            expiry.add(Calendar.YEAR, 3)
        }

        return TransitBalanceStored(b.balance, expiry)
    }

    companion object {
        private const val NAME = "Metrocard"
        private const val AGENCY_ID = 0x0136
        private val TIME_ZONE = TimeZone.getTimeZone("Pacific/Auckland")
        internal const val CURRENCY = "NZD"
        internal const val CHC_METROCARD_STR = "chc_metrocard"

        private val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.chc_metrocard)
                .setName(ChcMetrocardTransitData.NAME)
                .setLocation(R.string.location_christchurch_nz)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .setExtraNote(R.string.card_note_chc_metrocard)
                .build()

        val FACTORY: ClassicCardTransitFactory = object : ErgTransitData.ErgTransitFactory() {
            override fun parseTransitData(classicCard: ClassicCard) =
                    ChcMetrocardTransitData(classicCard)

            override fun parseTransitIdentity(card: ClassicCard) =
                    parseTransitIdentity(card, NAME)

            override fun getErgAgencyID() = AGENCY_ID

            override fun getAllCards() = listOf(CARD_INFO)

            override fun getSerialNumber(metadata: ErgMetadataRecord) =
                    internalFormatSerialNumber(metadata)
        }

        private fun internalFormatSerialNumber(metadataRecord: ErgMetadataRecord) =
                metadataRecord.cardSerialDec.toString()
    }
}
