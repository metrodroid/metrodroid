/*
 * ErgTransitData.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.erg.record.ErgBalanceRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgPurseRecord
import au.id.micolous.metrodroid.transit.erg.record.ErgRecord
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Transit data type for ERG/Videlli/Vix MIFARE Classic cards.
 *
 * Wiki: https://github.com/micolous/metrodroid/wiki/ERG-MFC
 */
abstract class ErgTransitData : TransitData() {
    abstract val metadata: ErgMetadataRecord
    abstract val mBalance: Int
    /**
     * Allows you to override the timezone used for all dates and times. Default timezone is the
     * current Android OS timezone.
     *
     * @return MetroTimeZone for the card.
     */
    abstract val timezone: MetroTimeZone
    abstract val currency: String

    // Structures

    public override val balance: TransitCurrency?
        get() = TransitCurrency(mBalance, currency)
    override val serialNumber get() = formatSerialNumber(metadata)

    override val info: List<ListItem>?
        get() = listOf (
                HeaderListItem(R.string.general),
                ListItem(R.string.card_epoch,
                        TimestampFormatter.longDateFormat(ErgTrip.convertTimestamp(metadata.epochDate, timezone, 0))),
                ListItem(R.string.erg_agency_id,
                        NumberUtils.longToHex(metadata.agency.toLong())))

    /**
     * Some cards format the serial number in decimal rather than hex. By default, this uses hex.
     *
     * This can be overridden in subclasses to format the serial number correctly.
     * @param metadataRecord Metadata record for this card.
     * @return Formatted serial number, as string.
     */
    protected open fun formatSerialNumber(metadataRecord: ErgMetadataRecord): String = metadataRecord.cardSerialHex

    companion object {
        val SIGNATURE = ImmutableByteArray.of(0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01)

        fun getBalance(records: List<ErgRecord>) = records.filterIsInstance<ErgBalanceRecord>().maxBy { it.version }?.balance ?: 0

        fun getMetadata(records: List<ErgRecord>) = records.filterIsInstance<ErgMetadataRecord>().lastOrNull()

        fun parseTrips(records: List<ErgRecord>,
                               newTrip: (ErgPurseRecord, Int) -> ErgTrip) =
            // Now generate a transaction list.  This has a 1:1 mapping with trips (there is no
            // "tap off").
            //
            // These need the Epoch to be known first.
            records.filterIsInstance<ErgPurseRecord>().map {
                newTrip(it, getMetadata(records)?.epochDate ?: 0) }.sortedWith(Trip.Comparator())

        fun readRecords(card: ClassicCard): List<ErgRecord> {
            // Iterate through blocks on the card and deserialize all the binary data.
            val allBlocks = card.sectors.mapIndexed { secidx, sector -> sector.blocks.subList(0, 3).mapIndexed { blockidx, block -> Triple(secidx, blockidx, block) } }.flatten()
            return allBlocks.filterNot { (secidx, blockidx, _) -> secidx == 0 && blockidx == 0 }
                    .mapNotNull { (secidx, blockidx, block) ->ErgRecord.recordFromBytes(block.data, secidx, blockidx) }
        }
    }
}

@Parcelize
class ErgTransitDataUnknown(override val metadata: ErgMetadataRecord,
                            override val mBalance: Int,
                            override val trips: List<ErgTrip>?) : ErgTransitData() {
    override val currency: String
        get() = "XXX"
    override val timezone: MetroTimeZone
        get() = MetroTimeZone.UNKNOWN
    override val cardName: String
        get() = NAME

    constructor(records: List<ErgRecord>) : this(
            trips = ErgTransitData.parseTrips(records
            ) { record, baseTime -> ErgTripUnknown(record, baseTime) },
            metadata = ErgTransitData.getMetadata(records)!!,
            mBalance = ErgTransitData.getBalance(records)
    )

    constructor(card: ClassicCard) : this(ErgTransitData.readRecords(card))

    companion object {
        const val NAME = "ERG"
    }
}

abstract class ErgTransitFactory : ClassicCardTransitFactory {

    override val earlySectors: Int
        get() = 1

    /**
     * Used for checks on the ERG agency ID. Subclasses must implement this, and return
     * a positive 16-bit integer value.
     *
     * @see .earlyCheck
     * @return An ERG agency ID for the card, or -1 to match any agency ID.
     */
    protected abstract val ergAgencyID: Int

    override val allCards: List<CardInfo>
        get() = emptyList()

    private fun getMetadataRecord(sector0: ClassicSector): ErgMetadataRecord? {
        val file2: ImmutableByteArray
        try {
            file2 = sector0.getBlock(2).data
        } catch (ex: UnauthorizedException) {
            // Can't be for us...
            return null
        }

        return ErgMetadataRecord.recordFromBytes(file2)
    }

    private fun getMetadataRecord(card: ClassicCard): ErgMetadataRecord? =
            try {
                getMetadataRecord(card.getSector(0))
            } catch (ex: UnauthorizedException) {
                // Can't be for us...
                null
            }

    /**
     * ERG cards have two identifying marks:
     *
     *
     * 1. A signature in Sector 0, Block 1
     * 2. The agency ID in Sector 0, Block 2 (Readable with ErgMetadataRecord)
     *
     *
     * This check only determines if there is a signature -- subclasses should call this and then
     * perform their own check of the agency ID.
     *
     * @param sectors MIFARE classic card sectors
     * @return True if this is an ERG card, false otherwise.
     */
    override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
        val file1 = sectors[0].getBlock(1).data

        // Check for signature
        if (!file1.sliceOffLen(0, ErgTransitData.SIGNATURE.size).contentEquals(ErgTransitData.SIGNATURE)) {
            return false
        }

        val metadataRecord = getMetadataRecord(sectors[0]) ?: return false
        return ergAgencyID == -1 ||  metadataRecord.agency == ergAgencyID
    }

    protected fun parseTransitIdentity(card: ClassicCard, name: String): TransitIdentity {
        val metadata = getMetadataRecord(card)!!
        return TransitIdentity(name, metadata.cardSerialHex)
    }
}

object ErgTransitFactoryUnknown : ErgTransitFactory() {
     override val ergAgencyID: Int
        get() = -1

    override fun parseTransitData(card: ClassicCard): TransitData? {
        return ErgTransitDataUnknown(card)
    }

    override fun parseTransitIdentity(card: ClassicCard): TransitIdentity? {
        return parseTransitIdentity(card, ErgTransitDataUnknown.NAME)
    }
}

