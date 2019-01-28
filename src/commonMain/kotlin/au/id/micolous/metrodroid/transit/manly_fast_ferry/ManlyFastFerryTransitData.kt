/*
 * ManlyFastFerryTransitData.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.manly_fast_ferry

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitFactory
import au.id.micolous.metrodroid.transit.erg.ErgTrip
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgRecord

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 *
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 *
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Manly-Fast-Ferry
 */

@Parcelize
class ManlyFastFerryTransitData (override val metadata: ErgMetadataRecord,
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
            ) { record, baseTime -> ManlyFastFerryTrip(record, baseTime) },
            metadata = ErgTransitData.getMetadata(records)!!,
            mBalance = ErgTransitData.getBalance(records)
    )

    constructor(card: ClassicCard) : this(ErgTransitData.readRecords(card))

    companion object {
        private const val NAME = "Manly Fast Ferry"
        private const val AGENCY_ID = 0x0227
        internal val TIME_ZONE = MetroTimeZone.SYDNEY
        internal const val CURRENCY = "AUD"

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.manly_fast_ferry_card,
                name = NAME,
                locationId = R.string.location_sydney_australia,
                cardType = CardType.MifareClassic,
                keysRequired = true)

        val FACTORY: ClassicCardTransitFactory = object : ErgTransitFactory() {
            override val ergAgencyID: Int
                get() = AGENCY_ID

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitData(card: ClassicCard): TransitData =
                    ManlyFastFerryTransitData(card)

            override fun parseTransitIdentity(card: ClassicCard) =
                    parseTransitIdentity(card, NAME)
        }
    }
}
