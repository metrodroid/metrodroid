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
internal class PodorozhnikTopup (private val mTimestamp: Int,
                                 private val mFare: Int,
                                 private val mAgency: Int,
                                 private val mTopupMachine: Int): Trip() {

    override val startTimestamp: Timestamp?
        get() = PodorozhnikTransitData.convertDate(mTimestamp)

    override val fare: TransitCurrency?
        get() = TransitCurrency.RUB(-mFare)

    override val mode: Mode
        get() = Mode.TICKET_MACHINE

    override val machineID: String?
        get() = mTopupMachine.toString()

    override// TODO: handle other transports better.
    val startStation: Station?
        get() {
            if (mAgency == PodorozhnikTrip.TRANSPORT_METRO) {
                val station = mTopupMachine / 10
                val stationId = (PodorozhnikTrip.TRANSPORT_METRO shl 16) or (station shl 6)
                return StationTableReader.getStation(PodorozhnikTrip.PODOROZHNIK_STR,
                        stationId, station.toString())
            }
            return Station.unknown(mAgency.toString(16) + "/" + mTopupMachine.toString(16))
        }


    override fun getAgencyName(isShort: Boolean) = when (mAgency) {
            1 -> Localizer.localizeFormatted(R.string.podorozhnik_topup)
            else -> Localizer.localizeFormatted(R.string.unknown_format, mAgency)
        }
}
