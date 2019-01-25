package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData
import au.id.micolous.metrodroid.transit.charlie.CharlieCardTransitData
import au.id.micolous.metrodroid.transit.chc_metrocard.ChcMetrocardTransitData
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.kiev.KievTransitData
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData
import au.id.micolous.metrodroid.transit.metroq.MetroQTransitData
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData
import au.id.micolous.metrodroid.transit.ricaricami.RicaricaMiTransitData
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData
import au.id.micolous.metrodroid.transit.selecta.SelectaFranceTransitData
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData
import au.id.micolous.metrodroid.transit.serialonly.*
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData
import au.id.micolous.metrodroid.transit.troika.TroikaHybridTransitData
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.transit.zolotayakorona.ZolotayaKoronaTransitData

object ClassicCardFactoryRegistry {
    val allFactories = listOf(
            OVChipTransitData.FACTORY,
            // Search through ERG on MIFARE Classic compatibles.
            ManlyFastFerryTransitData.FACTORY,
            ChcMetrocardTransitData.FACTORY,
            // Fallback
            ErgTransitData.FALLBACK_FACTORY,
            // Nextfare
            SeqGoTransitData.FACTORY,
            LaxTapTransitData.FACTORY, MspGotoTransitData.FACTORY,
            // Fallback
            NextfareTransitData.FALLBACK_FACTORY,

            SmartRiderTransitData.FACTORY,

            TroikaHybridTransitData.FACTORY,
            PodorozhnikTransitData.FACTORY,
            StrelkaTransitData.FACTORY,
            CharlieCardTransitData.FACTORY,
            RicaricaMiTransitData.FACTORY,
            BilheteUnicoSPTransitData.FACTORY,
            KievTransitData.FACTORY,
            MetroQTransitData.FACTORY,
            EasyCardTransitData.FACTORY,
            TartuTransitFactory(),
            SelectaFranceTransitData.FACTORY,
            SunCardTransitData.FACTORY,
            ZolotayaKoronaTransitData.FACTORY,
            RkfTransitData.FACTORY,

            // This check must be THIRD TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            UnauthorizedClassicTransitData.FACTORY,
            // This check must be SECOND TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all empty sectors
            BlankClassicTransitData.FACTORY,
            // This check must be LAST.
            //
            // This is for agencies who don't have identifying "magic" in their card.
            FallbackFactory())
}
