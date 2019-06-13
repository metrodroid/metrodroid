package au.id.micolous.metrodroid.card.desfire

import au.id.micolous.metrodroid.transit.intercard.IntercardTransitData
import au.id.micolous.metrodroid.transit.adelaide.AdelaideMetrocardTransitData
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData
import au.id.micolous.metrodroid.transit.hsl.HSLTransitData
import au.id.micolous.metrodroid.transit.magnacarta.MagnaCartaTransitData
import au.id.micolous.metrodroid.transit.opal.OpalTransitData
import au.id.micolous.metrodroid.transit.orca.OrcaTransitData
import au.id.micolous.metrodroid.transit.serialonly.*
import au.id.micolous.metrodroid.transit.tampere.TampereTransitData
import au.id.micolous.metrodroid.transit.tfi_leap.LeapTransitData
import au.id.micolous.metrodroid.transit.unknown.BlankDesfireTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedDesfireTransitData

object DesfireCardTransitRegistry {
    val allFactories: List<DesfireCardTransitFactory> = listOf(
            OrcaTransitData.FACTORY,
            ClipperTransitData.FACTORY,
            HSLTransitData.FACTORY,
            OpalTransitData.FACTORY,
            MykiTransitData.FACTORY,
            IstanbulKartTransitData.FACTORY,
            LeapTransitData.FACTORY,
            TrimetHopTransitData.FACTORY,
            AdelaideMetrocardTransitData.FACTORY,
            AtHopTransitData.FACTORY,
            NolTransitData.FACTORY,
            NextfareDesfireTransitFactory(),
            TampereTransitData.FACTORY,
            MagnaCartaTransitData.FACTORY,
            IntercardTransitData.FACTORY,
            BlankDesfireTransitData.FACTORY,
            UnauthorizedDesfireTransitData.FACTORY)
}
