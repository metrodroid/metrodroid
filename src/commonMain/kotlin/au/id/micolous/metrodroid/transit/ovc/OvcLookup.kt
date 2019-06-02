package au.id.micolous.metrodroid.transit.ovc

import au.id.micolous.metrodroid.time.MetroTimeZone

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
            return SUBSCRIPTIONS[contractTariff]
        }
        // FIXME: i18n
        return "Unknown Subscription (0x" + contractTariff.toLong().toString(16) + ")"
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

    // FIXME: i18n
    private val SUBSCRIPTIONS = mapOf(
            /* It seems that all the IDs are unique, so why bother with the companies? */
            /* NS */
            0x0005 to "OV-jaarkaart",
            0x0007 to "OV-Bijkaart 1e klas",
            0x0011 to "NS Businesscard",
            0x0019 to "Voordeelurenabonnement (twee jaar)",
            0x00AF to "Studenten OV-chipkaart week (2009)",
            0x00B0 to "Studenten OV-chipkaart weekend (2009)",
            0x00B1 to "Studentenkaart korting week (2009)",
            0x00B2 to "Studentenkaart korting weekend (2009)",
            0x00C9 to "Reizen op saldo bij NS, 1e klasse",
            0x00CA to "Reizen op saldo bij NS, 2de klasse",
            0x00CE to "Voordeelurenabonnement reizen op saldo",
            0x00E5 to "Reizen op saldo (tijdelijk eerste klas)",
            0x00E6 to "Reizen op saldo (tijdelijk tweede klas)",
            0x00E7 to "Reizen op saldo (tijdelijk eerste klas korting)",
            /* Arriva */
            0x059A to "Dalkorting",
            /* Veolia */
            0x0626 to "DALU Dalkorting",
            /* Connexxion */
            0x0692 to "Daluren Oost-Nederland",
            0x069C to "Daluren Oost-Nederland",
            /* DUO */
            0x09C6 to "Student weekend-vrij",
            0x09C7 to "Student week-korting",
            0x09C9 to "Student week-vrij",
            0x09CA to "Student weekend-korting",
            /* GVB */
            0x0BBD to "Fietssupplement")
}
