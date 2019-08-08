/*
 * StationTableReader.kt
 * Reader for Metrodroid Station Table (MdST) files.
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import org.jetbrains.annotations.NonNls

import java.io.InputStream
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Locale

import au.id.micolous.metrodroid.proto.Stations
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip

expect fun openMdstFile(dbName: String): InputStream?

/**
 * Metrodroid Station Table (MdST) file reader.
 *
 * For more information about the file format, see extras/mdst/README.md in the Metrodroid source
 * repository.
 */
actual class StationTableReader
/**
 * Initialises a "connection" to a Metrodroid Station Table kept in the `assets/` directory.
 * @param dbName MdST filename
 * @throws IOException On read errors
 * @throws InvalidHeaderException If the file is not a MdST file.
 */
@Throws(IOException::class, StationTableReader.InvalidHeaderException::class)
private constructor(dbName: String) {

    private val mStationDb: Stations.StationDb
    private val mStationIndex: Stations.StationIndex? by lazy {
        try {
            // Reset back to the start of the station list.
            mTable.reset()

            // Skip over the station list
            mTable.skip(mStationsLength.toLong())

            // Read out the index
            Stations.StationIndex.parseDelimitedFrom(mTable)
        } catch (e: IOException) {
            Log.e(TAG, "error reading index", e)
            null
        }
    }
    private val mTable: InputStream
    private val mStationsLength: Int

    val notice: String?
        get() {
            if (mStationDb.licenseNotice.isEmpty()) {
                Log.d(TAG, "Notice does not exist")
                return null
            }

            return mStationDb.licenseNotice
        }

    class InvalidHeaderException : Exception()
    init {
        mTable = openMdstFile(dbName)!!

        // Read the Magic, and validate it.
        val header = ByteArray(4)
        if (mTable.read(header) != 4) {
            throw InvalidHeaderException()
        }

        if (!Arrays.equals(header, MAGIC)) {
            throw InvalidHeaderException()
        }

        // Check the version
        val version = readInt(mTable)
        if (version != VERSION) {
            throw InvalidHeaderException()
        }

        mStationsLength = readInt(mTable)

        // Read out the header
        mStationDb = Stations.StationDb.parseDelimitedFrom(mTable)

        // Mark where the start of the station list is.
        // AssetInputStream allows unlimited seeking, no need to specify a readlimit.
        mTable.mark(0)
    }

    private fun useEnglishName(): Boolean {
        val locale = Locale.getDefault().language
        return !mStationDb.localLanguagesList.contains(locale)
    }

    fun selectBestName(name: Stations.Names, isShort: Boolean): String? {
        val englishFull = name.english
        val englishShort = name.englishShort
        val english: String?
        val hasEnglishFull = englishFull != null && !englishFull.isEmpty()
        val hasEnglishShort = englishShort != null && !englishShort.isEmpty()

        if (hasEnglishFull && !hasEnglishShort)
            english = englishFull
        else if (!hasEnglishFull && hasEnglishShort)
            english = englishShort
        else
            english = if (isShort) englishShort else englishFull

        val localFull = name.local
        val localShort = name.localShort
        val local: String?
        val hasLocalFull = localFull != null && !localFull.isEmpty()
        val hasLocalShort = localShort != null && !localShort.isEmpty()

        if (hasLocalFull && !hasLocalShort)
            local = localFull
        else if (!hasLocalFull && hasLocalShort)
            local = localShort
        else
            local = if (isShort) localShort else localFull

        if (showBoth() && english != null && !english.isEmpty()
                && local != null && !local.isEmpty()) {
            if (english == local)
                return local
            return if (useEnglishName()) "$english ($local)" else "$local ($english)"
        }
        if (useEnglishName() && english != null && !english.isEmpty()) {
            return english
        }

        return if (local != null && !local.isEmpty()) {
            // Local preferred, or English not available
            local
        } else {
            // Local unavailable, use English
            english
        }
    }

    private fun showBoth(): Boolean {
        return Preferences.showBothLocalAndEnglish
    }

    /**
     * Gets a Station object, according to the MdST Protobuf definition.
     * @param id Stop ID
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    @Throws(IOException::class)
    private fun getProtoStationById(id: Int): Stations.Station? {
        val offset: Int
        try {
            offset = mStationIndex?.getStationMapOrThrow(id) ?: return null
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, String.format(Locale.ENGLISH, "Unknown station %d", id))
            return null
        }

        mTable.reset()
        mTable.skip(offset.toLong())
        return Stations.Station.parseDelimitedFrom(mTable)
    }

    private fun getOperatorDefaultMode(oper: Int): Trip.Mode? {
        val po = mStationDb.getOperatorsOrDefault(oper, null) ?: return null
        return if (po.defaultTransport == Stations.TransportType.UNKNOWN) null else Trip.Mode.valueOf(po.defaultTransport.toString())
    }

    /**
     * Gets a Metrodroid-native Station object for a given stop ID.
     * @param id Stop ID.
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    @Throws(IOException::class)
    private fun getStationById(id: Int, humanReadableID: String): Station? {
        val ps = getProtoStationById(id) ?: return null
        val lines = mutableMapOf<Int, Stations.Line>()
        for (lineId in ps.lineIdList) {
            val l = mStationDb.getLinesOrDefault(lineId, null)
            if (l != null) {
                lines[lineId] = l
            }
        }

        return fromProto(humanReadableID, ps,
                mStationDb.getOperatorsOrDefault(ps.operatorId, null),
                lines,
                mStationDb.ttsHintLanguage, this)
    }

    actual companion object {
        private val MAGIC = byteArrayOf(0x4d, 0x64, 0x53, 0x54)
        private const val VERSION = 1
        private const val TAG = "StationTableReader"

        private fun readInt(input: InputStream): Int {
            val b = ByteArray(4)
            input.read(b)
            // TODO: avoid copying? Probably not worth it
            return b.toImmutable().byteArrayToInt()
        }

        private val mSTRs: MutableMap<String, StationTableReader> = HashMap()

        private fun getSTR(@NonNls name: String?): StationTableReader? {
            if (name == null) {
                return null
            }

            synchronized(mSTRs) {
                if (mSTRs.containsKey(name))
                    return mSTRs[name]
            }

            try {
                val str = StationTableReader(name)
                synchronized(mSTRs) {
                    mSTRs.put(name, str)
                }
                return str
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't open DB $name", e)
                return null
            }
        }

        @JvmOverloads
        actual fun getStationNoFallback(reader: String?, id: Int, humanReadableId: String): Station? {
            val str = StationTableReader.getSTR(reader) ?: return null
            try {
                return str.getStationById(id, humanReadableId)
            } catch (e: IOException) {
                return null
            }
        }

        @JvmOverloads
        actual fun getStation(reader: String?, id: Int, humanReadableId: String): Station {
            val s = getStationNoFallback(reader, id, humanReadableId) ?: return Station.unknown(humanReadableId)
            return s
        }

        private fun fallbackName(id: Int): String {
            return Localizer.localizeString(R.string.unknown_format, NumberUtils.intToHex(id))
        }

        private fun fallbackName(humanReadableId: String): String {
            return Localizer.localizeString(R.string.unknown_format, humanReadableId)
        }

        actual fun getOperatorDefaultMode(reader: String?, id: Int): Trip.Mode {
            if (reader == null)
                return Trip.Mode.OTHER
            val str = StationTableReader.getSTR(reader) ?: return Trip.Mode.OTHER
            val m = str.getOperatorDefaultMode(id) ?: return Trip.Mode.OTHER
            return m
        }

        @JvmOverloads
        actual fun getLineName(reader: String?, id: Int, humanReadableId: String): String? {
            if (reader == null)
                return fallbackName(humanReadableId)

            val str = getSTR(reader) ?: return fallbackName(humanReadableId)
            val pl = str.mStationDb.getLinesOrDefault(id, null) ?: return fallbackName(humanReadableId)
            return str.selectBestName(pl.name, false)
        }

        actual fun getLineMode(reader: String?, id: Int): Trip.Mode? {
            val str = getSTR(reader) ?: return null
            val pl = str.mStationDb.getLinesOrDefault(id, null) ?: return null
            return if (pl.transport == Stations.TransportType.UNKNOWN) null else Trip.Mode.valueOf(pl.transport.toString())
        }

        actual fun getOperatorName(reader: String?, id: Int, isShort: Boolean, humanReadableId: String): String? {
            val str = StationTableReader.getSTR(reader) ?: return fallbackName(humanReadableId)
            val po = str.mStationDb.getOperatorsOrDefault(id, null) ?: return fallbackName(humanReadableId)
            return str.selectBestName(po.name, isShort)
        }

        private fun fromProto(humanReadableID: String, ps: Stations.Station,
                              po: Stations.Operator?, pl: Map<Int, Stations.Line>?,
                              ttsHintLanguage: String, str: StationTableReader): Station {
            val hasLocation = ps.latitude != 0f && ps.longitude != 0f

            var lines: MutableList<String>? = null
            var lineIds: MutableList<String>? = null

            if (pl != null) {
                lines = ArrayList()
                lineIds = ArrayList()
                for ((first, second) in pl) {
                    lines.addAll(listOfNotNull(str.selectBestName(second.getName(), true)))
                    lineIds.add(NumberUtils.intToHex(first))
                }
            }

            return Station(
                    humanReadableID,
                    if (po == null) null else str.selectBestName(po.name, true),
                    lines,
                    str.selectBestName(ps.name, false),
                    str.selectBestName(ps.name, true),
                    if (hasLocation) ps.latitude else null,
                    if (hasLocation) ps.longitude else null,
                    ttsHintLanguage, false, lineIds.orEmpty())
        }

        /**
         * Gets a licensing notice that applies to a particular MdST file.
         * @param reader Station database to read from.
         * @return String containing license notice, or null if not available.
         */
        actual fun getNotice(reader: String?): String? = StationTableReader.getSTR(reader)?.notice
    }
}
