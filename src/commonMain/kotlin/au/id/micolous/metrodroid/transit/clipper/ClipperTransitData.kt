/*
 * ClipperTransitData.kt
 *
 * Copyright 2011 "an anonymous contributor"
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

package au.id.micolous.metrodroid.transit.clipper

import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.*

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class ClipperTransitData (private val mSerialNumber: Long?,
                          private val mBalance: Int?,
                          private val mExpiry: Int,
                          private val mTrips: List<ClipperTrip>,
                          private val mRefills: List<ClipperRefill>): TransitData() {

    override val cardName: String
        get() = "Clipper"

    public override val balance: TransitBalance?
        get() = if (mBalance != null)
            TransitBalanceStored(TransitCurrency.USD(mBalance), null, clipperTimestampToCalendar(mExpiry * 86400L))
        else
            null

    override val serialNumber: String?
        get() = mSerialNumber?.toString()

    // This is done in a roundabout way, as the base type used is the first parameter. Adding it
    // to an empty Trip[] first, coerces types correctly from the start.
    override val trips get(): List<Trip> = mTrips + mRefills

    companion object {
        private fun parseTrips(card: DesfireCard): List<ClipperTrip> {
            val file = card.getApplication(APP_ID)?.getFile(0x0e)

            /*
             *  This file reads very much like a record file but it professes to
             *  be only a regular file.  As such, we'll need to extract the records
             *  manually.
             */
            val data = file?.data ?: return emptyList()
            val result = mutableListOf<ClipperTrip>()
            var pos = data.size - RECORD_LENGTH
            while (pos >= 0) {
                if (data.byteArrayToInt(pos + 0x2, 2) == 0) {
                    pos -= RECORD_LENGTH
                    continue
                }

                val trip = ClipperTrip(data.sliceOffLen(pos, RECORD_LENGTH))

                // Some transaction types are temporary -- remove previous trip with the same timestamp.
                val existingTrip = result.find { otherTrip -> trip.startTimestamp == otherTrip.startTimestamp }

                if (existingTrip != null) {
                    if (existingTrip.endTimestamp != null) {
                        // Old trip has exit timestamp, and is therefore better.
                        pos -= RECORD_LENGTH
                        continue
                    } else {
                        result.remove(existingTrip)
                    }
                }
                result.add(trip)
                pos -= RECORD_LENGTH
            }
            return result
        }

        private fun parseRefills(card: DesfireCard): List<ClipperRefill> {
            /*
             *  This file reads very much like a record file but it professes to
             *  be only a regular file.  As such, we'll need to extract the records
             *  manually.
             */
            val data = card.getApplication(APP_ID)?.getFile(0x04)?.data ?: return emptyList()
            return (data.indices step RECORD_LENGTH).reversed().
                    map { pos -> data.sliceOffLen(pos, RECORD_LENGTH) }.
                    mapNotNull { slice -> createRefill(slice) }
        }

        private fun createRefill(useData: ImmutableByteArray): ClipperRefill? {
            val agency: Int = useData.byteArrayToInt(0x2, 2)
            val timestamp: Long = useData.byteArrayToLong(0x4, 4)
            val machineid: String = useData.getHexString(0x8, 4)
            val amount: Int = useData.byteArrayToInt(0xe, 2)

            if (timestamp == 0L) return null
            return ClipperRefill(clipperTimestampToCalendar(timestamp), amount, agency, machineid)
        }

        private fun parse(desfireCard: DesfireCard) =
                ClipperTransitData(
                        mExpiry = desfireCard.getApplication(APP_ID)?.getFile(0x01)?.data?.byteArrayToInt(8, 2) ?: 0,
                        mSerialNumber = desfireCard.getApplication(APP_ID)?.getFile(0x08)?.data?.byteArrayToLong(1, 4),
                        mRefills = parseRefills(desfireCard),
                        mBalance = desfireCard.getApplication(APP_ID)?.getFile(0x02)?.data?.byteArrayToInt(18, 2)?.toShort()?.toInt(),
                        mTrips = parseTrips(desfireCard))

        private const val RECORD_LENGTH = 32
        private val CLIPPER_EPOCH = Epoch.utc(1900, MetroTimeZone.LOS_ANGELES)

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.clipper_card,
                name = "Clipper",
                locationId = R.string.location_san_francisco,
                cardType = CardType.MifareDesfire,
                region = TransitRegion.USA,
                resourceExtraNote = R.string.card_note_clipper)

        const val APP_ID = 0x9011f2

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard) =
                    TransitIdentity("Clipper",
                card.getApplication(APP_ID)?.getFile(0x08)?.data?.byteArrayToLong(1, 4)?.toString())
        }

        internal fun clipperTimestampToCalendar(timestamp: Long): TimestampFull? {
            return if (timestamp == 0L) null else CLIPPER_EPOCH.seconds(timestamp)
        }
    }
}
