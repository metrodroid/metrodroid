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
import au.id.micolous.metrodroid.transit.TransitName
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReaderImpl.InvalidHeaderException
import platform.Foundation.NSBundle

import au.id.micolous.metrodroid.proto.stations.*
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf

actual internal fun StationTableReaderGetSTR(name: String): StationTableReader? =
    StationTableReaderRegistry.fetch(name)

private const val TAG = "StationTableReaderIOS"

class StationTableReaderWrapper (val name: String) {
    val wrapped: StationTableReaderImpl? by lazy {
        try {
            val ret = StationTableReaderImpl(name)
            Log.d(TAG, "Opened DB $name")
            ret
        } catch (e: Exception) {
            Log.w(TAG, "DB $name doesn't exist: $e")
            null
        }
    }
}

object StationTableReaderRegistry {
    private val allList: List<String> get() {
        val res = NSBundle.mainBundle.pathsForResourcesOfType("mdst", inDirectory = "mdst").toList().filterIsInstance<String>().map { it.substringAfterLast("/").substringBefore(".mdst") }
        Log.d(TAG, "mdsts = $res")
        return res
    }
    private val registry: Map<String, StationTableReaderWrapper> = allList.map { it to StationTableReaderWrapper(it) }.toMap()
    fun fetch(name: String) = registry[name]?.wrapped
    fun path(name: String) = NSBundle.mainBundle.pathForResource(name, ofType = "mdst", inDirectory = "mdst")
}

operator fun GPBUInt32UInt32Dictionary.get(key: UInt): UInt? {
    val buffer = uintArrayOf(0.toUInt())
    var found = false
    buffer.usePinned { pin ->
       found = this.getUInt32(pin.addressOf(0), forKey = key)
    }
    if (found)
       return buffer[0]
    return null
}

fun GPBUInt32Array.toList(): List<UInt> = List(this.count.toInt()) { this.valueAtIndex(it.toULong()) }

private const val MAX_VARINT = 10

fun <T : GPBMessage> parseDelimited(file: ConcurrentFileReader, off: Long, dest: T): Pair<T?, Int> {
    val headBuf = file.read(off, MAX_VARINT)
    val headCi = GPBCodedInputStream.streamWithData(headBuf.toNSData())
    val payloadLen = headCi.readInt64().toInt()
    val headLen = headCi.position().toInt()
    val payloadBuf = file.read(off, payloadLen + headLen)
    val payloadCi = GPBCodedInputStream.streamWithData(payloadBuf.toNSData())
    try {
        payloadCi.readMessage(dest, null)
    } catch (e: Exception) {
        Log.e(TAG, "error reading mesaage: $e")
        return Pair(null, headLen + payloadLen)
    }
    return Pair(dest, headLen + payloadLen)
}

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
 * @throws InvalidHeaderException If the file is not a MdST file.
 */
internal constructor(dbName: String) : StationTableReader {

    private val mStationDb: StationDb
    private val mStationIndex: StationIndex? by lazy {
        try {
            parseDelimited(mTable, mStationsStart + mStationsLength, StationIndex()).first
        } catch (e: Exception) {
            Log.e(TAG, "error reading index", e)
            null
        }
    }
    private val mTable: ConcurrentFileReader
    private val mStationsLength: Long
    private val mStationsStart: Long

    override val notice: String?
        get() =  mStationDb.licenseNotice.ifEmpty { null }

    class InvalidHeaderException (msg: String): Exception(msg)
    init {
        val path = StationTableReaderRegistry.path(dbName) ?: throw InvalidHeaderException("Resource not found")
        mTable = ConcurrentFileReader.openFile(path) ?: throw InvalidHeaderException("Open failed")

        val header = mTable.read(0, 12)
        if (header.size != 12)
            throw InvalidHeaderException("Failed reading header")

        val imm = header.toImmutable()

        // Read the Magic, and validate it.
        if (imm.sliceOffLen(0, 4) != MAGIC) {
            throw InvalidHeaderException("Header mismatch")
        }

        // Check the version
        if (imm.byteArrayToInt(4, 4) != VERSION) {
            throw InvalidHeaderException("Version")
        }

        mStationsLength = imm.byteArrayToLong(8, 4)

        // Read out the header
        val (stationDb, len) = parseDelimited(mTable, 12, StationDb())

        mStationDb = stationDb ?: throw InvalidHeaderException("stationDb is nutll")

        mStationsStart = 12 + len.toLong()
    }

    /**
     * Gets a Station object, according to the MdST buf definition.
     * @param id Stop ID
     * @return Station object, or null if it could not be found.
     */
    private fun getStationById(id: Int): Station? {
        val offset: Int
        try {
            offset = mStationIndex?.stationMap?.get(id.toUInt())?.toInt() ?: return null
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Unknown station $id")
            return null
        }

        return parseDelimited(mTable, mStationsStart + offset.toLong(), Station()).first
    }

    override fun getOperatorDefaultMode(oper: Int): Trip.Mode? {
        val po = mStationDb.operators.objectForKey(oper.toUInt()) as? Operator ?: return null
        return convertTransportType(po.defaultTransport)
    }

    override fun getOperatorName(oper: Int): TransitName? {
        val po = mStationDb.operators.objectForKey(oper.toUInt()) as? Operator ?: return null
        return makeTransitName(po.name ?: return null)
    }

    override fun getLineName(id: Int): TransitName? {
        val pl = mStationDb.lines.objectForKey(id.toUInt()) as? Line ?: return null
        return makeTransitName(pl.name)
    }

    override fun getLineMode(id: Int): Trip.Mode? {
        val pl = mStationDb.lines.objectForKey(id.toUInt()) as? Line ?: return null
        return convertTransportType(pl.transport)
    }

    /**
     * Gets a Metrodroid-native Station object for a given stop ID.
     * @param id Stop ID.
     * @return Station object, or null if it could not be found.
     */
    override fun getStationById(id: Int, humanReadableID: String): ProtoStation? {
        val ps = getStationById(id) ?: return null
        return ProtoStation(name = makeTransitName(ps.name), latitude = ps.latitude, longitude = ps.longitude,
                            lineIdList = ps.lineIdArray.toList().map { it.toInt() }, operatorId = ps.operatorId.toInt())
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun makeTransitName(name: Names) =
        TransitName(englishFull = name.english,
                    englishShort = name.englishShort,
                    localFull = name.local,
                    localShort = name.localShort,
                    localLanguagesList = mStationDb.localLanguagesArray.toList().filterIsInstance<String>(),
                    ttsHintLanguage = mStationDb.ttsHintLanguage)

    companion object {
        private val MAGIC = ImmutableByteArray.of(0x4d, 0x64, 0x53, 0x54)
        private const val VERSION = 1

        private fun squash(input: String): String = input.substringAfter("TransportType").replace("_", "", ignoreCase = true).toLowerCase()

        private fun convertTransportType(input : TransportType): Trip.Mode? {
            if (input == TransportType_Unknown)
                return null
            val name = TransportType_EnumDescriptor().enumNameForValue(input)
            if (name == null) {
                Log.w(TAG, "Unknown transport type $input")
                return null
            }
            return Trip.Mode.values().find { squash (it.name) == squash (name) }
        }
    }
}
