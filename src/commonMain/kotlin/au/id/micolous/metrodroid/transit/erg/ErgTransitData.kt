/*
 * ErgTransitData.kt
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

import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.XXX
import au.id.micolous.metrodroid.transit.erg.record.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.TripObfuscator

@Parcelize
class ErgTransitDataCapsule(
        // Structures
        internal val cardSerial: ImmutableByteArray?,
        internal val epochDate: Int,
        internal val agencyID: Int,
        internal val balance: Int,
        internal val trips: List<Trip>?
): Parcelable

@Parcelize
class ErgUnknownTransitData(
        override val capsule: ErgTransitDataCapsule): ErgTransitData() {
    override val currency: TransitCurrencyRef get() = ::XXX
}

/**
 * Transit data type for ERG/Videlli/Vix MIFARE Classic cards.
 *
 * Wiki: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
abstract class ErgTransitData : TransitData() {
    protected abstract val capsule: ErgTransitDataCapsule
    protected abstract val currency: TransitCurrencyRef

    /**
     * Some cards format the serial number in decimal rather than hex. By default, this uses hex.
     *
     * This can be overridden in subclasses to format the serial number correctly.
     * @return Formatted serial number, as string.
     */
    override val serialNumber: String?
        get() = capsule.cardSerial?.toHexString()

    public override val balance: TransitBalance?
        get() = currency(capsule.balance)

    override val trips: List<Trip>?
        get() = capsule.trips

    override val info: List<ListItem>?
        get() {
            val items = ArrayList<ListItem>()
            items.add(HeaderListItem(R.string.general))
            items.add(ListItem(R.string.card_epoch,
                    TimestampFormatter.longDateFormat(TripObfuscator.maybeObfuscateTS(
                            ErgTransaction.convertTimestamp(capsule.epochDate,
                                    timezone, 0, 0)))))
            items.add(ListItem(R.string.erg_agency_id,
                    NumberUtils.longToHex(capsule.agencyID.toLong())))
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
    protected open val timezone: MetroTimeZone
        get() = MetroTimeZone.UNKNOWN

    companion object {
        // Decoder
        fun parse(card: ClassicCard,
                  /**
                   * Allows you to override the constructor for new trips, to hook in your own station ID code.
                   *
                   * @return Subclass of ErgTransaction.
                   */
                  newTrip: (ErgPurseRecord, Int) -> ErgTransaction = ::ErgUnknownTransaction)
                : ErgTransitDataCapsule {
            val records = ArrayList<ErgRecord>()

            // Read the index data
            val index1 = ErgIndexRecord.recordFromSector(card.getSector(1))
            val index2 = ErgIndexRecord.recordFromSector(card.getSector(2))
            @Suppress("ConstantConditionIf")
            if (DEBUG) {
                Log.d(TAG, "Index 1: $index1")
                Log.d(TAG, "Index 2: $index2")
            }

            val activeIndex = if (index1.version > index2.version) index1 else index2

            val preambleRecord: ErgPreambleRecord = ErgPreambleRecord.recordFromBytes(card[0, 1].data)
            val metadataRecord: ErgMetadataRecord = ErgMetadataRecord.recordFromBytes(card[0, 2].data)

            // Iterate through blocks on the card and deserialize all the binary data.
            for ((sectorNum, sector) in card.sectors.withIndex().drop(3)) {
                for ((blockNum, block) in sector.blocks.dropLast(1).withIndex()) {
                    val data = block.data

                    // Fallback to using indexes
                    val record = activeIndex.readRecord(sectorNum, blockNum, data) ?: continue

                    Log.d(TAG, "Sector $sectorNum, Block $blockNum: $record")
                    @Suppress("ConstantConditionIf")
                    if (DEBUG) {
                        Log.d(TAG, data.getHexString())
                    }

                    records.add(record)
                }
            }

            val epochDate = metadataRecord.epochDate

            val txns = records.filterIsInstance<ErgPurseRecord>().map { newTrip(it, epochDate) }
            val balance = records.filterIsInstance<ErgBalanceRecord>().map { it.balance }.lastOrNull()

            return ErgTransitDataCapsule(
                    // Merge trips as appropriate
                    cardSerial = metadataRecord.cardSerial,
                    trips = TransactionTrip.merge(txns.sortedWith(Transaction.Comparator())),
                    balance = balance ?: 0,
                    epochDate = epochDate,
                    agencyID = metadataRecord.agencyID
            )
        }

        // Flipping this to true shows more data from the records in Logcat.
        private const val DEBUG = true
        private const val TAG = "ErgTransitData"

        internal const val NAME = "ERG"
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
    }
}
