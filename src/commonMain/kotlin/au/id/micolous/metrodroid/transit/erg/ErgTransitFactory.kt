/*
 * ErgTransitFactory.kt
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

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.erg.record.ErgMetadataRecord

internal open class ErgTransitFactory : ClassicCardTransitFactory {

    /**
     * Used for checks on the ERG agencyID ID. Subclasses must implement this, and return
     * a positive 16-bit integer value.
     *
     * @see .earlyCheck
     * @return An ERG agencyID ID for the card, or -1 to match any agencyID ID.
     */
    protected open val ergAgencyID
        get() = -1

    /**
     * ERG cards have two identifying marks:
     *
     *
     * 1. A signature in Sector 0, Block 1
     * 2. The agencyID ID in Sector 0, Block 2 (Readable with ErgMetadataRecord)
     *
     *
     * This check only determines if there is a signature -- subclasses should call this and then
     * perform their own check of the agencyID ID.
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

        val agencyID = ergAgencyID
        return if (agencyID == -1) {
            true
        } else {
            val metadataRecord = ErgTransitData.getMetadataRecord(sectors[0])
            metadataRecord != null && metadataRecord.agencyID == agencyID
        }
    }

    override fun parseTransitIdentity(card: ClassicCard) =
            parseTransitIdentity(card, ErgTransitData.NAME)

    protected fun parseTransitIdentity(card: ClassicCard, name: String): TransitIdentity? {
        val metadata = ErgTransitData.getMetadataRecord(card) ?: return null
        return TransitIdentity(name, getSerialNumber(metadata))
    }

    override fun parseTransitData(card: ClassicCard) : TransitData? {
        val capsule = ErgTransitData.parse(card)
        return ErgUnknownTransitData(capsule)
    }

    override val earlySectors get() = 1

    protected open fun getSerialNumber(metadata: ErgMetadataRecord): String {
        return metadata.cardSerial.toHexString()
    }
}
