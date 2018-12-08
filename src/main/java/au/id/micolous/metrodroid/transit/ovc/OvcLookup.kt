package au.id.micolous.metrodroid.transit.ovc

import java.util.TimeZone

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.ImmutableMapBuilder
import au.id.micolous.metrodroid.util.StationTableReader

class OvcLookup private constructor() : En1545LookupSTR(OVCHIP_STR) {

    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)

    override fun getTimeZone() = TIME_ZONE

    override fun getSubscriptionName(agency: Int?, subscription: Int?): String? {
        if (subscription == null)
            return null
        if (SUBSCRIPTIONS.containsKey(subscription)) {
            return SUBSCRIPTIONS[subscription]
        }
        // FIXME: i18n
        return "Unknown Subscription (0x" + subscription.toLong().toString(16) + ")"
    }

    override fun getStation(stationCode: Int, companyCode: Int?, transport: Int?): Station? {
        if (companyCode == null)
            return Station.unknown(stationCode)
        val companyCodeShort = companyCode and 0xFFFF

        // TLS is the OVChip operator, and doesn't have any stations.
        if (companyCodeShort == 0) return null

        val stationId = (companyCodeShort - 1 shl 16) or (stationCode and 0xFFFF)
        return if (stationId <= 0) null else StationTableReader.getStation(OVCHIP_STR, stationId)
    }

    companion object {
        val instance = OvcLookup()

        private const val OVCHIP_STR = "ovc"

        // FIXME: i18n
        private val SUBSCRIPTIONS = ImmutableMapBuilder<Int, String>()
                /* It seems that all the IDs are unique, so why bother with the companies? */
                /* NS */
                .put(0x0005, "OV-jaarkaart")
                .put(0x0007, "OV-Bijkaart 1e klas")
                .put(0x0011, "NS Businesscard")
                .put(0x0019, "Voordeelurenabonnement (twee jaar)")
                .put(0x00AF, "Studenten OV-chipkaart week (2009)")
                .put(0x00B0, "Studenten OV-chipkaart weekend (2009)")
                .put(0x00B1, "Studentenkaart korting week (2009)")
                .put(0x00B2, "Studentenkaart korting weekend (2009)")
                .put(0x00C9, "Reizen op saldo bij NS, 1e klasse")
                .put(0x00CA, "Reizen op saldo bij NS, 2de klasse")
                .put(0x00CE, "Voordeelurenabonnement reizen op saldo")
                .put(0x00E5, "Reizen op saldo (tijdelijk eerste klas)")
                .put(0x00E6, "Reizen op saldo (tijdelijk tweede klas)")
                .put(0x00E7, "Reizen op saldo (tijdelijk eerste klas korting)")
                /* Arriva */
                .put(0x059A, "Dalkorting")
                /* Veolia */
                .put(0x0626, "DALU Dalkorting")
                /* Connexxion */
                .put(0x0692, "Daluren Oost-Nederland")
                .put(0x069C, "Daluren Oost-Nederland")
                /* DUO */
                .put(0x09C6, "Student weekend-vrij")
                .put(0x09C7, "Student week-korting")
                .put(0x09C9, "Student week-vrij")
                .put(0x09CA, "Student weekend-korting")
                /* GVB */
                .put(0x0BBD, "Fietssupplement")
                .build()

        val TIME_ZONE: TimeZone = TimeZone.getTimeZone("Europe/Amsterdam")
    }
}
