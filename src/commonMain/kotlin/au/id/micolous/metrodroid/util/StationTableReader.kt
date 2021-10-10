package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitName
import au.id.micolous.metrodroid.transit.Trip

internal expect fun stationTableReaderGet(name: String): StationTableReader?

data class ProtoStation(val operatorId: Int?, val name: TransitName, val latitude: Float, val longitude: Float, val lineIdList: List<Int>)

interface StationTableReader {
    val notice: String?

    fun getOperatorDefaultMode(oper: Int): Trip.Mode?
    
    fun getOperatorName(oper: Int): TransitName?

    fun getStationById(id: Int, humanReadableID: String): ProtoStation?

    fun getLineName(id: Int): TransitName?

    fun getLineMode(id: Int): Trip.Mode?

    companion object {
        private fun fallbackName(humanReadableId: String): FormattedString =
            Localizer.localizeFormatted(R.string.unknown_format, humanReadableId)

        private fun getSTR(reader: String?): StationTableReader? = reader?.let { stationTableReaderGet(it) }

        private fun fromProto(humanReadableID: String, ps: ProtoStation,
                              operatorName: TransitName?, pl: Map<Int, TransitName?>?): Station {
            val hasLocation = ps.latitude != 0f && ps.longitude != 0f

            var lines: MutableList<FormattedString>? = null
            var lineIds: MutableList<String>? = null

            if (pl != null) {
                lines = ArrayList()
                lineIds = ArrayList()
                for ((first, second) in pl) {
                    lines.addAll(listOfNotNull(second?.selectBestName(true)))
                    lineIds.add(NumberUtils.intToHex(first))
                }
            }

            return Station(
                    humanReadableID,
                    operatorName?.selectBestName(true),
                    lines,
                    ps.name.selectBestName(false),
                    ps.name.selectBestName(true),
                    if (hasLocation) ps.latitude else null,
                    if (hasLocation) ps.longitude else null,
                    false, lineIds.orEmpty())
        }

        fun getStationNoFallback(reader: String?, id: Int,
                                 humanReadableId: String = NumberUtils.intToHex(id)): Station? {
            val str = getSTR(reader) ?: return null
            try {
                val ps = str.getStationById(id, humanReadableId) ?: return null
                val lines = mutableMapOf<Int, TransitName?>()
                for (lineId in ps.lineIdList) {
                    lines[lineId] = str.getLineName(lineId)
                }
                return fromProto(humanReadableId, ps,
                     ps.operatorId?.let { str.getOperatorName(it) },
                     lines)
            } catch (e: Exception) {
                return null
            }
        }

        fun getStation(reader: String?, id: Int, humanReadableId: String = NumberUtils.intToHex(id)): Station =
                getStationNoFallback(reader, id, humanReadableId) ?: Station.unknown(humanReadableId)

        fun getOperatorDefaultMode(reader: String?, id: Int): Trip.Mode =
                getSTR(reader)?.getOperatorDefaultMode(id) ?: Trip.Mode.OTHER

        fun getLineNameNoFallback(reader: String?, id: Int): FormattedString? =
                getSTR(reader)?.getLineName (id)?.selectBestName(false)

        fun getLineName(reader: String?, id: Int, humanReadableId: String = NumberUtils.intToHex(id)): FormattedString =
                getLineNameNoFallback(reader, id) ?: fallbackName(humanReadableId)

        fun getLineMode(reader: String?, id: Int): Trip.Mode?
                = getSTR(reader)?.getLineMode(id)

        fun getOperatorName(reader: String?, id: Int, isShort: Boolean,
                            humanReadableId: String = NumberUtils.intToHex(id)): FormattedString? =
                getSTR(reader)?.getOperatorName(id)?.selectBestName(isShort) ?: fallbackName(humanReadableId)

        /**
         * Gets a licensing notice that applies to a particular MdST file.
         * @param reader Station database to read from.
         * @return String containing license notice, or null if not available.
         */
        fun getNotice(reader: String?): String? = getSTR(reader)?.notice
    }
}
