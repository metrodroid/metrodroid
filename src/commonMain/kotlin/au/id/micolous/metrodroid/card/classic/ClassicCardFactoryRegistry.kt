package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData
import au.id.micolous.metrodroid.transit.bonobus.BonobusTransitFactory
import au.id.micolous.metrodroid.transit.charlie.CharlieCardTransitData
import au.id.micolous.metrodroid.transit.chc_metrocard.ChcMetrocardTransitData
import au.id.micolous.metrodroid.transit.chilebip.ChileBipTransitFactory
import au.id.micolous.metrodroid.transit.cifial.CifialTransitFactory
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.gautrain.GautrainTransitFactory
import au.id.micolous.metrodroid.transit.kazan.KazanTransitFactory
import au.id.micolous.metrodroid.transit.kiev.KievTransitData
import au.id.micolous.metrodroid.transit.komuterlink.KomuterLinkTransitFactory
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData
import au.id.micolous.metrodroid.transit.metromoney.MetroMoneyTransitFactory
import au.id.micolous.metrodroid.transit.metroq.MetroQTransitData
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData
import au.id.micolous.metrodroid.transit.ndef.NdefClassicTransitFactory
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.umarsh.UmarshTransitFactory
import au.id.micolous.metrodroid.transit.otago.OtagoGoCardTransitFactory
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData
import au.id.micolous.metrodroid.transit.oyster.OysterTransitData
import au.id.micolous.metrodroid.transit.pilet.KievDigitalTransitFactory
import au.id.micolous.metrodroid.transit.pilet.TartuTransitFactory
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData
import au.id.micolous.metrodroid.transit.ricaricami.RicaricaMiTransitData
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData
import au.id.micolous.metrodroid.transit.selecta.SelectaFranceTransitData
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData
import au.id.micolous.metrodroid.transit.serialonly.*
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData
import au.id.micolous.metrodroid.transit.touchngo.TouchnGoTransitFactory
import au.id.micolous.metrodroid.transit.troika.TroikaHybridTransitData
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitFactory
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.transit.waikato.WaikatoCardTransitFactory
import au.id.micolous.metrodroid.transit.warsaw.WarsawTransitData
import au.id.micolous.metrodroid.transit.yargor.YarGorTransitFactory
import au.id.micolous.metrodroid.transit.zolotayakorona.ZolotayaKoronaTransitData

object ClassicCardFactoryRegistry {
    val classicFactories = listOf(
            OVChipTransitData.FACTORY,

            // ERG
            ManlyFastFerryTransitData.FACTORY,
            ChcMetrocardTransitData.FACTORY,
            // ERG Fallback
            ErgTransitData.FALLBACK_FACTORY,

            // Cubic Nextfare
            SeqGoTransitData.FACTORY,
            LaxTapTransitData.FACTORY,
            MspGotoTransitData.FACTORY,
            // Cubic Nextfare Fallback
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
            SelectaFranceTransitData.FACTORY,
            SunCardTransitData.FACTORY,
            ZolotayaKoronaTransitData.FACTORY,
            RkfTransitData.FACTORY,
            OtagoGoCardTransitFactory,
            WaikatoCardTransitFactory,
            TouchnGoTransitFactory,
            KomuterLinkTransitFactory,
            BonobusTransitFactory,
            GautrainTransitFactory,
            MetroMoneyTransitFactory,
            OysterTransitData.FACTORY,
            KazanTransitFactory,
            UmarshTransitFactory,
            ChileBipTransitFactory,
            WarsawTransitData.FACTORY,
            CifialTransitFactory,
            YarGorTransitFactory,

            TartuTransitFactory, // Must be before NDEF as it's a special case of Ndef
            NdefClassicTransitFactory,

            // This check must be THIRD TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            UnauthorizedClassicTransitData.FACTORY,
            // This check must be SECOND TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all empty sectors
            BlankClassicTransitFactory,
            // This check must be LAST.
            //
            // This is for agencies who don't have identifying "magic" in their card.
            FallbackFactory)

    val plusFactories = listOf(
            KievDigitalTransitFactory,
            NdefClassicTransitFactory,

            // This check must be THIRD TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            UnauthorizedClassicTransitData.FACTORY,
            // This check must be SECOND TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all empty sectors
            BlankClassicTransitFactory
    )
    val allFactories = classicFactories + plusFactories
}
