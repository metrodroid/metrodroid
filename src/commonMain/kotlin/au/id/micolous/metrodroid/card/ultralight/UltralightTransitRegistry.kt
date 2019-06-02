package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.transit.clipper.ClipperUltralightTransitData
import au.id.micolous.metrodroid.transit.ovc.OvcUltralightTransitFactory
import au.id.micolous.metrodroid.transit.nextfareul.NextfareUnknownUltralightTransitData
import au.id.micolous.metrodroid.transit.pisa.PisaUltralightTransitFactory
import au.id.micolous.metrodroid.transit.serialonly.MRTUltralightTransitFactory
import au.id.micolous.metrodroid.transit.ventra.VentraUltralightTransitData
import au.id.micolous.metrodroid.transit.yvr_compass.CompassUltralightTransitData
import au.id.micolous.metrodroid.transit.troika.TroikaUltralightTransitData
import au.id.micolous.metrodroid.transit.unknown.BlankUltralightTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedUltralightTransitData
import au.id.micolous.metrodroid.transit.venezia.VeneziaUltralightTransitFactory

object UltralightTransitRegistry {
     val allFactories = listOf(
            TroikaUltralightTransitData.FACTORY,
            CompassUltralightTransitData.FACTORY,
            VentraUltralightTransitData.FACTORY,
            // This must be after the checks for known Nextfare MFU deployments.
            NextfareUnknownUltralightTransitData.FACTORY,
            ClipperUltralightTransitData.FACTORY,
            OvcUltralightTransitFactory(),
            MRTUltralightTransitFactory(),
            VeneziaUltralightTransitFactory(),
            PisaUltralightTransitFactory(),
            BlankUltralightTransitData.FACTORY,
            // This check must be LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            UnauthorizedUltralightTransitData.FACTORY)
}
