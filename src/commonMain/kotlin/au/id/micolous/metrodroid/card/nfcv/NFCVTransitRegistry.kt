package au.id.micolous.metrodroid.card.nfcv

import au.id.micolous.metrodroid.transit.ndef.NdefVicinityTransitFactory
import au.id.micolous.metrodroid.transit.unknown.BlankNFCVTransitFactory

object NFCVTransitRegistry {
    val allFactories = listOf(
         NdefVicinityTransitFactory,
         BlankNFCVTransitFactory
     )
}
