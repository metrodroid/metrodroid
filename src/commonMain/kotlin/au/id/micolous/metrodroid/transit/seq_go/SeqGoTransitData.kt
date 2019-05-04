/*
 * SeqGoTransitData.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.AUD
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitDataCapsule
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
@Parcelize
class SeqGoTransitData (override val capsule: NextfareTransitDataCapsule,
                        private val mTicketType: SeqGoData.TicketType): NextfareTransitData() {
    override val currency
        get() = ::AUD

    override val cardName: String
        get() = NAME

    override val moreInfoPage: String?
        get() = "https://micolous.github.io/metrodroid/seqgo"

    override val onlineServicesPage: String?
        get() = "https://gocard.translink.com.au/"

    /**
     * The base implementation of hasUnknownStations from Nextfare always returns false, but we can
     * return the correct value for Go card.
     *
     * @return true if there are unknown station IDs on the card.
     */
    override val hasUnknownStations: Boolean
        get() = capsule.hasUnknownStations

    override val ticketClass: String?
        get() = Localizer.localizeString(mTicketType.description)

    override val timezone: MetroTimeZone
        get() = TIME_ZONE

    companion object {
        private const val NAME = "Go card"

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.seqgo_card,
                imageAlphaId = R.drawable.seqgo_card_alpha,
                name = NAME,
                locationId = R.string.location_brisbane_seq_australia,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                resourceExtraNote = R.string.card_note_seqgo)

        val SYSTEM_CODE1 = ImmutableByteArray.fromHex(
                "5A5B20212223"
        )

        val SYSTEM_CODE2 = ImmutableByteArray.fromHex(
                "202122230101"
        )

        private val TIME_ZONE = MetroTimeZone.BRISBANE

        private const val TAG = "SeqGoTransitData"

        val FACTORY: ClassicCardTransitFactory = object : NextfareTransitData.NextFareTransitFactory() {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
                return super.parseTransitIdentity(card, NAME)
            }

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                val sector0 = sectors[0]
                val blockData = sector0.getBlock(1).data
                if (!blockData.copyOfRange(1, 9).contentEquals(NextfareTransitData.MANUFACTURER)) {
                    return false
                }

                val systemCode = blockData.copyOfRange(9, 15)
                //Log.d(TAG, "SystemCode = " + Utils.getHexString(systemCode));
                return systemCode.contentEquals(SYSTEM_CODE1) || systemCode.contentEquals(SYSTEM_CODE2)
            }

            override fun parseTransitData(card: ClassicCard): TransitData {
                val capsule = parse(card, TIME_ZONE,
                        ::SeqGoTrip,
                        { SeqGoRefill(NextfareTripCapsule(it), it.isAutomatic) })

                val ticketNum = capsule.mConfig?.ticketType
                val ticketType = SeqGoData.TICKET_TYPES[ticketNum] ?: SeqGoData.TicketType.UNKNOWN

                return SeqGoTransitData(capsule, ticketType)
            }
        }

        val notice: String?
            get() = StationTableReader.getNotice(SeqGoData.SEQ_GO_STR)
    }
}
