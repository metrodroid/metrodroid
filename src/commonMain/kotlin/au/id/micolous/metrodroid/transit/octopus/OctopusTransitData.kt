/*
 * OctopusTransitData.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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
package au.id.micolous.metrodroid.transit.octopus


import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.card.felica.FelicaService
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.china.NewShenzhenTransitData
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
@Parcelize
class OctopusTransitData private constructor(private val mOctopusBalance: Int?,
                                             private val mShenzhenBalance: Int?) : TransitData() {

    private val mHasOctopus get() = mOctopusBalance != null
    private val mHasShenzhen get() = mShenzhenBalance != null

    // Octopus balance takes priority 1
    // Shenzhen Tong balance takes priority 2
    override val balances: List<TransitBalance>?
        get() {
            val bals = mutableListOf<TransitBalance>()
            if (mOctopusBalance != null) {
                bals.add(TransitCurrency.HKD(mOctopusBalance))
            }
            if (mShenzhenBalance != null) {
                bals.add(TransitCurrency.CNY(mShenzhenBalance))
            }
            return bals
        }

    // TODO: Find out where this is on the card.
    override val serialNumber: String?
        get() = null

    override val cardName: String
        get() = when {
            mHasShenzhen && mHasOctopus -> Localizer.localizeString(R.string.card_name_octopus_szt_dual)
            mHasShenzhen -> Localizer.localizeString(R.string.card_name_szt)
            else -> Localizer.localizeString(R.string.card_name_octopus)
        }

    companion object {
        private fun getBalance(service: FelicaService?): Int? {
            val metadata = service?.getBlock(0)?.data ?: return null
            return metadata.byteArrayToInt(0, 4) - 350
        }
        private fun parse(card: FelicaCard) = OctopusTransitData(
                mOctopusBalance = getBalance(card.getSystem(SYSTEMCODE_OCTOPUS)?.getService(SERVICE_OCTOPUS)),
                mShenzhenBalance = getBalance(card.getSystem(SYSTEMCODE_SZT)?.getService(SERVICE_SZT))
        )

        const val SYSTEMCODE_SZT = 0x8005
        const val SYSTEMCODE_OCTOPUS = 0x8008

        const val SERVICE_OCTOPUS = 0x0117
        const val SERVICE_SZT = 0x0118

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.octopus_card,
                imageAlphaId = R.drawable.octopus_card_alpha,
                name = Localizer.localizeString(R.string.card_name_octopus),
                locationId = R.string.location_hong_kong,
                cardType = CardType.FeliCa)

        val FACTORY: FelicaCardTransitFactory = object : FelicaCardTransitFactory() {

            // Shenzhen Tong is added to supported list by new Shenzhen Tong code.
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(systemCodes: List<Int>) =
                    SYSTEMCODE_OCTOPUS in systemCodes || SYSTEMCODE_SZT in systemCodes

            override fun getCardInfo(systemCodes: List<Int>): CardInfo? {
                // OctopusTransitData is special, because it handles two types of cards.  So we can just
                // directly say which cardInfo matches.
                if (SYSTEMCODE_OCTOPUS in systemCodes)
                    return CARD_INFO // also dual-mode cards.

                if (SYSTEMCODE_SZT in systemCodes)
                    return NewShenzhenTransitData.CARD_INFO
                return null
            }

            override fun parseTransitData(card: FelicaCard) = parse(card)

            override fun parseTransitIdentity(card: FelicaCard): TransitIdentity {
                val hasOctopus = card.getSystem(SYSTEMCODE_OCTOPUS) != null
                val hasSzt = card.getSystem(SYSTEMCODE_SZT) != null
                return when {
                    hasSzt && hasOctopus -> // Dual-mode card.
                        TransitIdentity(Localizer.localizeString(R.string.card_name_octopus_szt_dual), null)
                    hasSzt -> // SZT-only card.
                        TransitIdentity(Localizer.localizeString(R.string.card_name_szt), null)
                    else ->
                        // Octopus-only card.
                        TransitIdentity(Localizer.localizeString(R.string.card_name_octopus), null)
                }
            }
        }
    }
}
