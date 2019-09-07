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

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.proto.Stations
import au.id.micolous.metrodroid.transit.TransitName
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReaderImpl.InvalidHeaderException
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.io.InputStream
import java.util.*

expect fun openMdstFile(dbName: String): InputStream?
internal actual fun StationTableReaderGetSTR(name: String): StationTableReader? =
    StationTableReaderImpl.getSTR(name)

/**
 * Metrodroid Station Table (MdST) file reader.
 *
 * For more information about the file format, see extras/mdst/README.md in the Metrodroid source
 * repository.
 */
class StationTableReaderImpl
/**
 * Initialises a "connection" to a Metrodroid Station Table kept in the `assets/` directory.
 * @param dbName MdST filename
 * @throws IOException On read errors
 * @throws InvalidHeaderException If the file is not a MdST file.
 */
private constructor(dbName: String) : StationTableReader {

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
    private val mTable: InputStream = openMdstFile(dbName)!!
    private val mStationsLength: Int

    override val notice: String?
        get() =  mStationDb.licenseNotice.ifEmpty { null }

    class InvalidHeaderException : Exception()
    init {

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
            Log.d(TAG, "Unknown station $id")
            return null
        }

        mTable.reset()
        mTable.skip(offset.toLong())
        return Stations.Station.parseDelimitedFrom(mTable)
    }

    override fun getOperatorDefaultMode(oper: Int): Trip.Mode? {
        val po = mStationDb.getOperatorsOrDefault(oper, null) ?: return null
        return if (po.defaultTransport == Stations.TransportType.UNKNOWN) null else Trip.Mode.valueOf(po.defaultTransport.toString())
    }

    override fun getOperatorName(oper: Int): TransitName? {
        val po = mStationDb.getOperatorsOrDefault(oper, null) ?: return null
        return makeTransitName(po.name ?: return null)
    }

    override fun getLineName(id: Int): TransitName? {
        val pl = mStationDb.getLinesOrDefault(id, null) ?: return null
        return makeTransitName(pl.name)
    }

    override fun getLineMode(id: Int): Trip.Mode? {
        val pl = mStationDb.getLinesOrDefault(id, null) ?: return null
        return if (pl.transport == Stations.TransportType.UNKNOWN) null else Trip.Mode.valueOf(pl.transport.toString())
    }

    /**
     * Gets a Metrodroid-native Station object for a given stop ID.
     * @param id Stop ID.
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    override fun getStationById(id: Int, humanReadableID: String): ProtoStation? {
        val ps = getProtoStationById(id) ?: return null
        return ProtoStation(name = makeTransitName(ps.name), latitude = ps.latitude, longitude = ps.longitude,
                            lineIdList = ps.lineIdList, operatorId = ps.operatorId)
    }

    private fun makeTransitName(name: Stations.Names) =
        TransitName(englishFull = name.english,
                    englishShort = name.englishShort,
                    localFull = name.local,
                    localShort = name.localShort,
                    localLanguagesList = mStationDb.localLanguagesList,
                    ttsHintLanguage = mStationDb.ttsHintLanguage)

    companion object {
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

        internal fun getSTR(@NonNls name: String?): StationTableReader? {
            if (name == null) {
                return null
            }

            synchronized(mSTRs) {
                if (mSTRs.containsKey(name))
                    return mSTRs[name]
            }

            try {
                val str = StationTableReaderImpl(name)
                synchronized(mSTRs) {
                    mSTRs.put(name, str)
                }
                return str
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't open DB $name", e)
                return null
            }
        }
    }
}
