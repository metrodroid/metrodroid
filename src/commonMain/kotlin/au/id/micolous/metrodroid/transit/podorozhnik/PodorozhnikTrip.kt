package au.id.micolous.metrodroid.transit.podorozhnik

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader

@Parcelize
internal class PodorozhnikTrip(private val mTimestamp: Int,
                               private val mFare: Int?,
                               private val mLastTransport: Int,
                               private val mLastValidator: Int): Trip() {

    override val startTimestamp: Timestamp?
        get() = PodorozhnikTransitData.convertDate(mTimestamp)

    override val fare: TransitCurrency?
        get() = if (mFare != null) {
            TransitCurrency.RUB(mFare)
        } else {
            null
        }

    // TODO: Handle trams
    override val mode: Mode
        get() {
            if (mLastTransport == TRANSPORT_METRO && mLastValidator == 0)
                return Mode.BUS
            return if (mLastTransport == TRANSPORT_METRO) Mode.METRO else Mode.BUS
        }

    // TODO: handle other transports better.
    override val startStation: Station?
        get() {
            var stationId = mLastValidator or (mLastTransport shl 16)
            if (mLastTransport == TRANSPORT_METRO && mLastValidator == 0)
                return null
            if (mLastTransport == TRANSPORT_METRO) {
                val gate = stationId and 0x3f
                stationId = stationId and 0x3f.inv()
                return StationTableReader.getStation(PODOROZHNIK_STR, stationId, (mLastValidator shr 6).toString()).addAttribute(Localizer.localizeString(R.string.podorozhnik_gate, gate))
            }
            return StationTableReader.getStation(PODOROZHNIK_STR, stationId,
                    "$mLastTransport/$mLastValidator")
        }

    override fun getAgencyName(isShort: Boolean) =
    // Always include "Saint Petersburg" in names here to distinguish from Troika (Moscow)
    // trips on hybrid cards
            when (mLastTransport) {
                // Some validators are misconfigured and show up as Metro, station 0, gate 0.
                // Assume bus.
                TRANSPORT_METRO -> if (mLastValidator == 0) Localizer.localizeFormatted(R.string.led_bus) else Localizer.localizeFormatted(R.string.led_metro)
                TRANSPORT_BUS, TRANSPORT_BUS_MOBILE -> Localizer.localizeFormatted(R.string.led_bus)
                TRANSPORT_SHARED_TAXI -> Localizer.localizeFormatted(R.string.led_shared_taxi)
// TODO: Handle trams
                else -> Localizer.localizeFormatted(R.string.unknown_format, mLastTransport)
            }

    companion object {
        const val PODOROZHNIK_STR = "podorozhnik"
        const val TRANSPORT_METRO = 1
        // Some buses use fixed validators while others
        // have a fixed validator and they have different codes
        private const val TRANSPORT_BUS_MOBILE = 3
        private const val TRANSPORT_BUS = 4
        private const val TRANSPORT_SHARED_TAXI = 7
    }
}
