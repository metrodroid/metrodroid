package au.id.micolous.metrodroid.transit.octopus

import au.id.micolous.metrodroid.transit.TransitData

expect class OctopusTransitData : TransitData {
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

        val FACTORY: FelicaCardTransitFactory = object : FelicaCardTransitFactory {

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