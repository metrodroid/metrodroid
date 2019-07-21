package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip

expect class StationTableReader {
    companion object {
        fun getStationNoFallback(reader: String?, id: Int,
                                 humanReadableId: String = NumberUtils.intToHex(id)): Station?

        fun getStation(reader: String?, id: Int, humanReadableId: String = NumberUtils.intToHex(id)): Station

        fun getOperatorDefaultMode(reader: String?, id: Int): Trip.Mode

        fun getLineName(reader: String?, id: Int, humanReadableId: String = NumberUtils.intToHex(id)): String?
        fun getLineMode(reader: String?, id: Int): Trip.Mode?

        fun getOperatorName(reader: String?, id: Int, isShort: Boolean,
                            humanReadableId: String = NumberUtils.intToHex(id)): String?
        fun getNotice(reader: String?): String?
    }
}
