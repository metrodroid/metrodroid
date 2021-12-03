package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.StationTableReader

@Parcelize
internal class TroikaTrip (override val startTimestamp: Timestamp?,
                           private val mTransportType: TroikaBlock.TroikaTransportType?,
                           private val mValidator: Int?,
                           private val mRawTransport: String?,
                           private val mFareDesc: StringResource?): Trip() {
    override val startStation: Station?
        get() = if (mValidator == null) null else StationTableReader.getStation(TROIKA_STR, mValidator)

    // Troika doesn't store monetary price of trip. Only a fare code. So show this fare
    // code to the user.
    override val fare: TransitCurrencyResource?
        get() = mFareDesc?.let { TransitCurrencyResource(it) }

    override val mode: Mode
        get() = when (mTransportType) {
            null -> Mode.OTHER
            TroikaBlock.TroikaTransportType.NONE, TroikaBlock.TroikaTransportType.UNKNOWN -> Mode.OTHER
            TroikaBlock.TroikaTransportType.SUBWAY -> Mode.METRO
            TroikaBlock.TroikaTransportType.MONORAIL -> Mode.MONORAIL
            TroikaBlock.TroikaTransportType.GROUND -> Mode.BUS
            TroikaBlock.TroikaTransportType.MCC -> Mode.TRAIN
        }

    override fun getAgencyName(isShort: Boolean) =
            when (mTransportType) {
                TroikaBlock.TroikaTransportType.UNKNOWN -> Localizer.localizeFormatted(R.string.unknown)
                null, TroikaBlock.TroikaTransportType.NONE -> mRawTransport?.let { FormattedString(it) }
                TroikaBlock.TroikaTransportType.SUBWAY -> Localizer.localizeFormatted(R.string.moscow_subway)
                TroikaBlock.TroikaTransportType.MONORAIL -> Localizer.localizeFormatted(R.string.moscow_monorail)
                TroikaBlock.TroikaTransportType.GROUND -> Localizer.localizeFormatted(R.string.moscow_ground_transport)
                TroikaBlock.TroikaTransportType.MCC -> Localizer.localizeFormatted(R.string.moscow_mcc)
            }

    companion object {
        private const val TROIKA_STR = "troika"
    }
}
