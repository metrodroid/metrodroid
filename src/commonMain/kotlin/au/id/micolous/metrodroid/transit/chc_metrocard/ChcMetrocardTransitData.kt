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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.NZD
import au.id.micolous.metrodroid.transit.TransitCurrencyRef
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitDataCapsule
import au.id.micolous.metrodroid.transit.erg.ErgTransitFactory
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Transit data type for pre-2016 Metrocard (Christchurch, NZ).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * The post-2016 version of this card is a DESFire card made by INIT.
 *
 * Documentation: https://github.com/micolous/metrodroid/wiki/Metrocard-%28Christchurch%29
 */
@Parcelize
class ChcMetrocardTransitData(override val capsule: ErgTransitDataCapsule) : ErgTransitData() {
    override val currency: TransitCurrencyRef get() = CURRENCY
    override val cardName get() = NAME
    override val timezone get() = TIME_ZONE

    override val serialNumber: String?
        get() = capsule.cardSerial?.let { internalFormatSerialNumber(it) }


    override val balance get(): TransitBalance? {
        val b = super.balance ?: return null

        val expiry = getLastUseDaystamp() ?: return b
        // Cards not used for 3 years will expire
        return TransitBalanceStored(b.balance, expiry + Duration.yearsLocal(3))
    }

    companion object {
        private const val NAME = "Metrocard"
        private const val AGENCY_ID = 0x0136
        internal val TIME_ZONE = MetroTimeZone.AUCKLAND
        internal val CURRENCY = ::NZD
        internal const val CHC_METROCARD_STR = "chc_metrocard"

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.chc_metrocard,
                name = NAME,
                locationId = R.string.location_christchurch_nz,
                cardType = CardType.MifareClassic,
                keysRequired = true,
                region = TransitRegion.NEW_ZEALAND,
                resourceExtraNote = R.string.card_note_chc_metrocard)

        val FACTORY: ClassicCardTransitFactory = object : ErgTransitFactory() {
            override fun parseTransitData(card: ClassicCard) =
                    ChcMetrocardTransitData(parse(card, ::ChcMetrocardTransaction))

            override fun parseTransitIdentity(card: ClassicCard) =
                    parseTransitIdentity(card, NAME)

            override val ergAgencyID: Int
                get() = AGENCY_ID

            override val allCards get() = listOf(CARD_INFO)

            override fun getSerialNumber(metadata: ErgMetadataRecord) =
                    internalFormatSerialNumber(metadata.cardSerial)
        }

        private fun internalFormatSerialNumber(serial: ImmutableByteArray) =
                serial.byteArrayToInt().toString()
    }
}
