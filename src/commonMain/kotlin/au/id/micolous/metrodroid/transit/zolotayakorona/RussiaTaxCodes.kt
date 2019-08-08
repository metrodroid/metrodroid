package au.id.micolous.metrodroid.transit.zolotayakorona

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.util.NumberUtils

// Tax codes assigned by Russian Tax agency for places both inside Russia (e.g. Moscow) and outside (e.g. Baikonur)
// They are used by Zolotaya Korona and Umarsh
// This dataset may also include additional codes used by those systems
object RussiaTaxCodes {
    fun BCDToTimeZone(bcd: Int): MetroTimeZone = TAX_CODES[bcd]?.second ?: MetroTimeZone.MOSCOW
    fun codeToName(regionNum: Int): String = TAX_CODES[NumberUtils.intToBCD(regionNum)]?.first?.let { Localizer.localizeString(it) } ?: Localizer.localizeString(R.string.unknown_format, regionNum)
    fun BCDToName(regionNum: Int): String = TAX_CODES[regionNum]?.first?.let { Localizer.localizeString(it) } ?: Localizer.localizeString(R.string.unknown_format, regionNum.toString(16))

    private val TAX_CODES = mapOf(
            // List of cities is taken from Zolotaya Korona website. Regions match
            // license plate regions
            //
            // Gorno-Altaysk
            0x04 to Pair(R.string.russia_region_04_altai_republic, MetroTimeZone.KRASNOYARSK),
            // Syktyvkar and Ukhta
            0x11 to Pair(R.string.russia_region_11_komi, MetroTimeZone.KIROV),
            0x12 to Pair(R.string.russia_region_12_mari_el, MetroTimeZone.MOSCOW),
            0x18 to Pair(R.string.russia_region_18_udmurt, MetroTimeZone.SAMARA),
            // Biysk
            0x22 to Pair(R.string.russia_region_22_altai, MetroTimeZone.KRASNOYARSK),
            // Krasnodar and Sochi
            0x23 to Pair(R.string.russia_region_23_krasnodar, MetroTimeZone.MOSCOW),
            // Vladivostok
            0x25 to Pair(R.string.russia_region_25_primorsky, MetroTimeZone.VLADIVOSTOK),
            // Khabarovsk
            0x27 to Pair(R.string.russia_region_27_khabarovsk, MetroTimeZone.VLADIVOSTOK),
            // Blagoveshchensk
            0x28 to Pair(R.string.russia_region_28_amur, MetroTimeZone.YAKUTSK),
            // Arkhangelsk
            0x29 to Pair(R.string.russia_region_29_arkhangelsk, MetroTimeZone.MOSCOW),
            // Petropavlovsk-Kamchatsky
            0x41 to Pair(R.string.russia_region_41_kamchatka, MetroTimeZone.KAMCHATKA),
            // Kemerovo and Novokuznetsk
            0x42 to Pair(R.string.russia_region_42_kemerovo, MetroTimeZone.NOVOKUZNETSK),
            0x43 to Pair(R.string.russia_region_43_kirov, MetroTimeZone.KIROV),
            // Kurgan
            0x45 to Pair(R.string.russia_region_45_kurgan, MetroTimeZone.YEKATERINBURG),
            0x52 to Pair(R.string.russia_region_52_nizhnij_novgorod, MetroTimeZone.MOSCOW),
            // Veliky Novgorod
            0x53 to Pair(R.string.russia_region_53_novgorod, MetroTimeZone.MOSCOW),
            // Novosibirsk
            0x54 to Pair(R.string.russia_region_54_novosibirsk, MetroTimeZone.NOVOSIBIRSK),
            // Omsk
            0x55 to Pair(R.string.russia_region_55_omsk, MetroTimeZone.OMSK),
            // Orenburg
            0x56 to Pair(R.string.russia_region_56_orenburg, MetroTimeZone.YEKATERINBURG),
            0x58 to Pair(R.string.russia_region_58_penza, MetroTimeZone.MOSCOW),
            // Pskov
            0x60 to Pair(R.string.russia_region_60_pskov, MetroTimeZone.MOSCOW),
            // Samara
            0x63 to Pair(R.string.russia_region_63_samara, MetroTimeZone.SAMARA),
            // Kholmsk
            0x65 to Pair(R.string.russia_region_65_sakhalin, MetroTimeZone.SAKHALIN),
            0x66 to Pair(R.string.russia_region_66_sverdlovsk, MetroTimeZone.YEKATERINBURG),
            0x74 to Pair(R.string.russia_region_74_chelyabinsk, MetroTimeZone.YEKATERINBURG),
            // Yaroslavl
            0x76 to Pair(R.string.russia_region_76_yaroslavl, MetroTimeZone.MOSCOW),
            // Birobidzhan
            0x79 to Pair(R.string.russia_region_79_jewish_autonomous, MetroTimeZone.VLADIVOSTOK),
            // 91 = Code used by Umarsh for Crimea
            0x91 to Pair(R.string.umarsh_91_crimea, MetroTimeZone.SIMFEROPOL)
    )
}