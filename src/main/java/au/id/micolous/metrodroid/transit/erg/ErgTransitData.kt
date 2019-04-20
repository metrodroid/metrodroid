/*
 * ErgTransitData.java
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.erg

import android.os.Parcel
import android.os.Parcelable
import android.util.Log

import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicBlock
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransactionTrip
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.erg.record.ErgBalanceRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgIndexRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgPreambleRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgRecord
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.TripObfuscator
import au.id.micolous.metrodroid.util.Utils

import java.util.ArrayList
import java.util.Collections
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Transit data type for ERG/Videlli/Vix MIFARE Classic cards.
 *
 * Wiki: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
open class ErgTransitData : TransitData, Parcelable {

    // Structures
    override var serialNumber: String? = null
        private set
    private var mEpochDate: Int = 0
    private var mAgencyID: Int = 0
    private var mBalance: Int = 0
    private val mTrips: List<Trip>?
    private val mCurrency: String?

    public override val balance: TransitBalance?
        get() = TransitCurrency(mBalance, mCurrency!!)

    override val trips: List<Trip>?
        get() = mTrips

    override val info: List<ListItem>?
        get() {
            val items = ArrayList<ListItem>()
            items.add(HeaderListItem(R.string.general))
            items.add(ListItem(R.string.card_epoch,
                    TimestampFormatter.longDateFormat(TripObfuscator.maybeObfuscateTS(
                            calendar2ts(ErgTransaction.convertTimestamp(mEpochDate,
                                    timezone, 0, 0))!!))))
            items.add(ListItem(R.string.erg_agency_id,
                    NumberUtils.longToHex(mAgencyID.toLong())))
            return items
        }

    override val cardName: String
        get() = NAME

    /**
     * Allows you to override the timezone used for all dates and times. Default timezone is the
     * current Android OS timezone.
     *
     * @return TimeZone for the card.
     */
    protected open// If we don't know the timezone, assume it is Android local timezone.
    val timezone: TimeZone
        get() = TimeZone.getDefault()

    protected constructor(parcel: Parcel) {
        serialNumber = parcel.readString()
        mEpochDate = parcel.readInt()

        mTrips = parcel.readArrayList(javaClass.classLoader)
        mCurrency = parcel.readString()
    }

    constructor(card: ClassicCard) : this(card, "XXX") {}

    // Decoder
    protected constructor(card: ClassicCard, currency: String) {
        val records = ArrayList<ErgRecord>()

        mCurrency = currency

        // Read the index data
        val index1 = ErgIndexRecord.recordFromSector(card.getSector(1))
        val index2 = ErgIndexRecord.recordFromSector(card.getSector(2))
        if (DEBUG) {
            Log.d(TAG, "Index 1: $index1")
            Log.d(TAG, "Index 2: $index2")
        }

        val activeIndex = if (index1.version > index2.version) index1 else index2

        var metadataRecord: ErgMetadataRecord? = null
        var preambleRecord: ErgPreambleRecord? = null

        // Iterate through blocks on the card and deserialize all the binary data.
        sectorLoop@ for ((sectorNum, sector) in card.sectors.withIndex()) {
            blockLoop@ for ((blockNum, block) in sector.blocks.withIndex()) {
                val data = block.data

                if (blockNum == 3) {
                    continue
                }

                when (sectorNum) {
                    0 -> {
                        when (blockNum) {
                            0 -> continue@blockLoop
                            1 -> {
                                preambleRecord = ErgPreambleRecord.recordFromBytes(data)
                                continue@blockLoop
                            }
                            2 -> {
                                metadataRecord = ErgMetadataRecord.recordFromBytes(data)
                                continue@blockLoop
                            }
                        }
                        // Skip indexes, we already read this.
                        continue@sectorLoop
                    }

                    1, 2 -> continue@sectorLoop
                }

                // Fallback to using indexes
                val record = activeIndex.readRecord(sectorNum, blockNum, data)

                if (record != null) {
                    Log.d(TAG, String.format(Locale.ENGLISH, "Sector %d, Block %d: %s",
                            sectorNum, blockNum,
                            if (DEBUG) record.toString() else record.javaClass.simpleName))
                    if (DEBUG) {
                        Log.d(TAG, data.getHexString())
                    }
                }

                if (record != null) {
                    records.add(record)
                }
            }
        }

        if (metadataRecord != null) {
            serialNumber = formatSerialNumber(metadataRecord)
            mEpochDate = metadataRecord.epochDate
            mAgencyID = metadataRecord.agencyID
        }

        val txns = ArrayList<ErgTransaction>()

        for (record in records) {
            if (record is ErgBalanceRecord) {
                mBalance = record.balance
            } else if (record is ErgPurseRecord) {
                txns.add(newTrip(record, mEpochDate))
            }
        }

        Collections.sort(txns, Transaction.Comparator())

        // Merge trips as appropriate
        mTrips = TransactionTrip.merge(txns)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serialNumber)
        parcel.writeInt(mEpochDate)
        parcel.writeList(mTrips)
        parcel.writeString(mCurrency)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Allows you to override the constructor for new trips, to hook in your own station ID code.
     *
     * @return Subclass of ErgTransaction.
     */
    protected open fun newTrip(purse: ErgPurseRecord, epoch: Int): ErgTransaction {
        return ErgTransaction(purse, epoch, mCurrency!!, timezone)
    }

    /**
     * Some cards format the serial number in decimal rather than hex. By default, this uses hex.
     *
     * This can be overridden in subclasses to format the serial number correctly.
     * @param metadataRecord Metadata record for this card.
     * @return Formatted serial number, as string.
     */
    protected open fun formatSerialNumber(metadataRecord: ErgMetadataRecord): String {
        return metadataRecord.cardSerialHex
    }

    companion object {
        // Flipping this to true shows more data from the records in Logcat.
        private val DEBUG = true
        private val TAG = ErgTransitData::class.java.simpleName

        internal val NAME = "ERG"
        val SIGNATURE = byteArrayOf(0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01)
        val FALLBACK_FACTORY: ClassicCardTransitFactory = ErgTransitFactory()

        internal fun getMetadataRecord(sector0: ClassicSector): ErgMetadataRecord? {
            val file2: ImmutableByteArray
            try {
                file2 = sector0.getBlock(2).data
            } catch (ex: UnauthorizedException) {
                // Can't be for us...
                return null
            }

            return ErgMetadataRecord.recordFromBytes(file2)
        }

        internal fun getMetadataRecord(card: ClassicCard): ErgMetadataRecord? {
            try {
                return getMetadataRecord(card.getSector(0))
            } catch (ex: UnauthorizedException) {
                // Can't be for us...
                return null
            }

        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<ErgTransitData> {
            override fun createFromParcel(parcel: Parcel): ErgTransitData {
                return ErgTransitData(parcel)
            }

            override fun newArray(size: Int): Array<ErgTransitData?> {
                return arrayOfNulls(size)
            }
        }
    }
}