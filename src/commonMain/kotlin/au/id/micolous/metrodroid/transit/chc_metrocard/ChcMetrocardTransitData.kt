/*
 * ChcMetrocardTransitData.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitFactory
import au.id.micolous.metrodroid.transit.erg.ErgTrip
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgRecord

/**
 * Transit data type for Metrocard (Christchurch, NZ).
 *
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */

@Parcelize
class ChcMetrocardTransitData (override val metadata: ErgMetadataRecord,
                               override val mBalance: Int,
                               override val trips: List<ErgTrip>?): ErgTransitData() {

    override val cardName: String
        get() = NAME
    override val timezone: MetroTimeZone
        get() = TIME_ZONE
    override val currency: String
        get() = CURRENCY

    constructor(records: List<ErgRecord>) : this(
            trips = ErgTransitData.parseTrips(records
            ) { record, baseTime -> ChcMetrocardTrip(record, baseTime) },
            metadata = ErgTransitData.getMetadata(records)!!,
            mBalance = ErgTransitData.getBalance(records)
    )

    constructor(card: ClassicCard) : this(ErgTransitData.readRecords(card))

    override fun formatSerialNumber(metadataRecord: ErgMetadataRecord): String {
        return metadataRecord.cardSerialDec.toString()
    }

    companion object {
        private const val NAME = "Metrocard"
        private const val AGENCY_ID = 0x0136
        internal val TIME_ZONE = MetroTimeZone.AUCKLAND
        internal const val CURRENCY = "NZD"

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.chc_metrocard,
                name = ChcMetrocardTransitData.NAME,
                locationId = R.string.location_christchurch_nz,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                resourceExtraNote = R.string.card_note_chc_metrocard,
                preview = true)

        val FACTORY: ClassicCardTransitFactory = object : ErgTransitFactory() {
            override val ergAgencyID: Int
                get() = AGENCY_ID

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitData(card: ClassicCard) = ChcMetrocardTransitData(card)

            override fun parseTransitIdentity(card: ClassicCard) = parseTransitIdentity(card, NAME)
        }
    }
}
