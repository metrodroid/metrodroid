package au.id.micolous.metrodroid.transit.ovc

import au.id.micolous.metrodroid.time.MetroTimeZone

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.StationTableReader

private const val OVCHIP_STR = "ovc"

object OvcLookup : En1545LookupSTR(OVCHIP_STR) {
    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override val timeZone get() = MetroTimeZone.AMSTERDAM

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        if (contractTariff == null)
            return null
        if (SUBSCRIPTIONS.containsKey(contractTariff)) {
            return Localizer.localizeString(SUBSCRIPTIONS[contractTariff]!!)
        }
        return Localizer.localizeString(R.string.unknown_format, "0x" + contractTariff.toLong().toString(16))
    }

    override fun getStation(station: Int, agency: Int?, transport: Int?): Station? {
        if (agency == null)
            return Station.unknown(station)
        val companyCodeShort = agency and 0xFFFF

        // TLS is the OVChip operator, and doesn't have any stations.
        if (companyCodeShort == 0) return null

        val stationId = (companyCodeShort - 1 shl 16) or (station and 0xFFFF)
        return if (stationId <= 0) null else StationTableReader.getStation(OVCHIP_STR, stationId)
    }

    private val SUBSCRIPTIONS = mapOf(
        /* It seems that all the IDs are unique, so why bother with the companies? */
        /* NS */
        0x0005 to R.string.ovc_sub_ov_jaarkaart,
        0x0007 to R.string.ovc_sub_ov_bijkaart_1e_klas,
        0x0011 to R.string.ovc_sub_ns_businesscard,
        0x0019 to R.string.ovc_sub_voordeelurenabonnement_twee_jaar,
        0x00af to R.string.ovc_sub_studenten_ov_chipkaart_week_2009,
        0x00b0 to R.string.ovc_sub_studenten_ov_chipkaart_weekend_2009,
        0x00b1 to R.string.ovc_sub_studentenkaart_korting_week_2009,
        0x00b2 to R.string.ovc_sub_studentenkaart_korting_weekend_2009,
        0x00c9 to R.string.ovc_sub_reizen_op_saldo_bij_ns_1e_klasse,
        0x00ca to R.string.ovc_sub_reizen_op_saldo_bij_ns_2de_klasse,
        0x00ce to R.string.ovc_sub_voordeelurenabonnement_reizen_op_saldo,
        0x00e5 to R.string.ovc_sub_reizen_op_saldo_tijdelijk_eerste_klas,
        0x00e6 to R.string.ovc_sub_reizen_op_saldo_tijdelijk_tweede_klas,
        0x00e7 to R.string.ovc_sub_reizen_op_saldo_tijdelijk_eerste_klas_korting,
        /* Arriva */
        0x059a to R.string.ovc_sub_dalkorting,
        /* Veolia */
        0x0626 to R.string.ovc_sub_dalu_dalkorting,
        /* Connexxion */
        0x0692 to R.string.ovc_sub_daluren_oost_nederland,
        0x069c to R.string.ovc_sub_daluren_oost_nederland,
        /* DUO */
        0x09c6 to R.string.ovc_sub_student_weekend_vrij,
        0x09c7 to R.string.ovc_sub_student_week_korting,
        0x09c9 to R.string.ovc_sub_student_week_vrij,
        0x09ca to R.string.ovc_sub_student_weekend_korting,
        /* GVB */
        0x0bbd to R.string.ovc_sub_fietssupplement)
}
