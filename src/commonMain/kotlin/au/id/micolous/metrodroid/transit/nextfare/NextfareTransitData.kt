/*
 * NextfareTransitData.kt
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
package au.id.micolous.metrodroid.transit.nextfare

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.XXX
import au.id.micolous.metrodroid.transit.nextfare.record.*
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord.Companion.Type.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class NextfareTransitDataCapsule(
        internal val mConfig: NextfareConfigRecord? = null,
        internal val hasUnknownStations: Boolean = false,
        internal val mSerialNumber: Long,
        internal val mSystemCode: ImmutableByteArray,
        internal val mBlock2: ImmutableByteArray,
        internal val mBalance: Int,
        internal val trips: List<NextfareTrip>?,
        internal val subscriptions: List<NextfareSubscription>): Parcelable

@Parcelize
class NextfareUnknownTransitData (
        override val capsule: NextfareTransitDataCapsule): NextfareTransitData() {
    override val currency: TransitCurrencyRef
        get() = ::XXX
}

/**
 * Generic transit data type for Cubic Nextfare.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 *
 * @author Michael Farrell
 */
abstract class NextfareTransitData : TransitData() {
    abstract val capsule: NextfareTransitDataCapsule
    abstract val currency: TransitCurrencyRef

    /**
     * Allows you to override the timezone used for all dates and times. Default timezone is the
     * current Android OS timezone.
     *
     * @return MetroTimeZone for the card.
     */
    protected open val timezone: MetroTimeZone
        get() = MetroTimeZone.UNKNOWN

    override val serialNumber: String?
        get() = formatSerialNumber(capsule.mSerialNumber)

    override val trips: List<NextfareTrip>?
        get() = capsule.trips

    public override val balance: TransitBalance?
        get() = if (capsule.mConfig != null) {
            TransitBalanceStored(currency(capsule.mBalance), ticketClass,
                    capsule.mConfig!!.expiry)
        } else
            currency(capsule.mBalance)

    open val ticketClass: String?
        get() = if (capsule.mConfig != null) {
            Localizer.localizeString(R.string.nextfare_ticket_class, capsule.mConfig!!.ticketType)
        } else null

    override val subscriptions: List<NextfareSubscription>?
        get() = capsule.subscriptions

    override val cardName: String
        get() = NAME

    /**
     * If true, then the unknown stations banner should be shown.
     *
     *
     * In the base Nextfare implementation, this is meaningless (all stations are unknown), so this
     * always returns false. But in subclasses, this should return mHasUnknownStations.
     *
     * @return always false - do not show unknown stations UI
     */
    override val hasUnknownStations: Boolean
        get() = false

    // The Los Angeles Tap and Minneapolis Go-To cards have the same system code, but different
    // data in Block 2.
    override val info: List<ListItem>?
        get() = listOf(
                HeaderListItem(R.string.nextfare),
                ListItem(R.string.nextfare_system_code, capsule.mSystemCode.toHexDump()),
                ListItem(
                    FormattedString(Localizer.localizeString(R.string.block_title_format, 2)),
                    capsule.mBlock2.toHexDump()))

    protected open class NextFareTransitFactory : ClassicCardTransitFactory {

        override val earlySectors: Int
            get() = 1

        override val allCards: List<CardInfo>
            get() = emptyList()

        override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
            val blockData = sectors[0].getBlock(1).data
            return blockData.copyOfRange(1, MANUFACTURER.size + 1).contentEquals(MANUFACTURER)
        }

        override fun parseTransitIdentity(card: ClassicCard): TransitIdentity? {
            return parseTransitIdentity(card, NAME)
        }

        protected fun parseTransitIdentity(card: ClassicCard, name: String): TransitIdentity {
            val serialData = card.getSector(0).getBlock(0).data
            val serialNumber = serialData.byteArrayToLongReversed(0, 4)
            return TransitIdentity(name, formatSerialNumber(serialNumber))
        }

        override fun parseTransitData(card: ClassicCard): TransitData? {
            val capsule = parse(card = card,
                    timeZone = MetroTimeZone.UNKNOWN,
                    newTrip = ::NextfareUnknownTrip,
                    newRefill = { NextfareUnknownTrip(NextfareTripCapsule(it))}
            )
            return NextfareUnknownTransitData(capsule)
        }
    }

    companion object {

        /**
         * Called when it needs to be determined if two TapRecords are part of the same journey.
         *
         *
         * Normally this should never need to be overwritten, except in the case that the Journey ID and
         * travel mode is not enough to break up the two journeys.
         *
         *
         * If the agency NEVER records tap-off events, this should always return false.
         *
         * @param tap1 The first tap to compare.
         * @param tap2 The second tap to compare.
         * @return true if the journeys should be merged.
         */
        private fun tapsMergeable(tap1: NextfareTransactionRecord, tap2: NextfareTransactionRecord): Boolean {
            return when {
                tap1.type.isSale || tap2.type.isSale -> false
                else -> tap1.journey == tap2.journey && tap1.mode == tap2.mode
            }
        }

        /**
         * Allows you to override the constructor for new subscriptions, to hook in your own code.
         *
         *
         * This method is used for existing / past travel passes.
         *
         * @param record Record to parse
         * @return Subclass of NextfareSubscription
         */
        private fun newSubscription(record: NextfareTravelPassRecord): NextfareSubscription {
            return NextfareSubscription(record)
        }

        /**
         * Allows you to override the constructor for new subscriptions, to hook in your own code.
         *
         *
         * This method is used for new, unused travel passes.
         *
         * @param record Record to parse
         * @return Subclass of NextfareSubscription
         */
        private fun newSubscription(record: NextfareBalanceRecord): NextfareSubscription {
            return NextfareSubscription(record)
        }

        fun parse(card: ClassicCard, timeZone: MetroTimeZone,
                  newTrip: (NextfareTripCapsule) -> NextfareTrip,
                  newRefill: (NextfareTopupRecord) -> NextfareTrip,
                  shouldMergeJourneys: Boolean = true): NextfareTransitDataCapsule {
            val serialData = card.getSector(0).getBlock(0).data
            val mSerialNumber = serialData.byteArrayToLongReversed(0, 4)

            val magicData = card.getSector(0).getBlock(1).data
            val mSystemCode = magicData.copyOfRange(9, 15)
            Log.d(TAG, "SystemCode = $mSystemCode")
            val mBlock2 = card.getSector(0).getBlock(2).data
            Log.d(TAG, "Block2 = $mBlock2")

            // Ignore sector 0 (preamble) and block 3 (mifare keys/ACL)
            val allBlocks = card.sectors.mapIndexed { secidx, sector -> sector.blocks.subList(0, 3).mapIndexed { blockidx, block -> Triple(secidx, blockidx, block) } }
                    .flatten().filterNot { (secidx, _, _) -> secidx == 0 }

            val records = allBlocks.mapNotNull {(secidx, blockidx, block) ->
                Log.d(TAG, "Sector $secidx / Block $blockidx")
                NextfareRecord.recordFromBytes(
                        block.data, secidx, blockidx, timeZone)
            }

            // Now do a first pass for metadata and balance information.
            val balances = records.filterIsInstance<NextfareBalanceRecord>().sorted()
            val trips = mutableListOf<NextfareTrip>()
            val subscriptions = mutableListOf<NextfareSubscription>()
            val taps = records.filterIsInstance<NextfareTransactionRecord>().sorted()
            val passes = records.filterIsInstance<NextfareTravelPassRecord>().sorted()

            trips += records.filterIsInstance<NextfareTopupRecord>().map { newRefill(it) }
            val mConfig: NextfareConfigRecord? = records.filterIsInstance<NextfareConfigRecord>().lastOrNull()

            val mBalance: Int

            if (balances.isNotEmpty()) {
                var balance = balances[0]
                if (balances.size == 2) {
                    // If the version number overflowed, we need to swap these around.
                    if (balances[0].version >= 240 && balances[1].version <= 10) {
                        balance = balances[1]
                    }

                }

                mBalance = balance.balance
                if (balance.hasTravelPassAvailable) {
                    subscriptions.add(newSubscription(balance))
                }
            } else
                mBalance = 0

            if (taps.isNotEmpty()) {
                // Lets figure out the trips.
                var i = 0

                while (taps.size > i) {
                    val tapOn = taps[i]

                    //Log.d(TAG, "TapOn @" + Utils.isoDateTimeFormat(tapOn.getTimestamp()));
                    // Start by creating an empty trip

                    val trip = NextfareTripCapsule(

                            // Put in the metadatas
                            mJourneyId = tapOn.journey,
                            startTimestamp = tapOn.timestamp,
                            mStartStation = tapOn.station,
                            mModeInt = tapOn.mode,
                            isTransfer = tapOn.isContinuation,
                            mCost = -tapOn.value)


                    // Peek at the next record and see if it is part of
                    // this journey
                    if (shouldMergeJourneys && taps.size > i + 1 && tapsMergeable(tapOn, taps[i + 1])) {
                        // There is a tap off.  Lets put that data in
                        val tapOff = taps[i + 1]
                        //Log.d(TAG, "TapOff @" + Utils.isoDateTimeFormat(tapOff.getTimestamp()));

                        trip.endTimestamp = tapOff.timestamp
                        trip.mEndStation = tapOff.station
                        trip.mCost -= tapOff.value

                        // Increment to skip the next record
                        i++
                    } else {
                        // There is no tap off. Journey is probably in progress, or the agency doesn't
                        // do tap offs.
                    }

                    trips.add(newTrip(trip))

                    // Increment to go to the next record
                    i++
                }

                // Now sort the trips array
                trips.sortWith(Trip.Comparator())

                // Trips are normally in reverse order, put them in forward order
                trips.reverse()

                /*
                // Check if the oldest trip was negative. That indicates that we probably got a tap-off
                // without a matching tap-on, and we should handle differently.
                //
                // Normally we silently drop the extra top-up record contained in the "tap" array, so
                // negative things shouldn't pop up here at all.
                NextfareTrip lastTrip = trips.get(trips.size() - 1);
                if (lastTrip.mCost < 0) {
                    // We have a negative cost.  We should clean up...
                    lastTrip.mEndTime = lastTrip.mStartTime;
                    lastTrip.mEndStation = lastTrip.mStartStation;
                    lastTrip.mStartTime = null;
                    lastTrip.mStartStation = 0;
                }
                */

            }

            val hasUnknownStations = trips.any { it.startStation?.isUnknown ?: false
                    || it.endStation?.isUnknown ?: false}

            if (passes.isNotEmpty()) {
                subscriptions.add(newSubscription(passes[0]))
            }

            return NextfareTransitDataCapsule(trips = trips,
                    subscriptions = subscriptions,
                    hasUnknownStations = hasUnknownStations, mBalance = mBalance,
                    mBlock2 = mBlock2, mSerialNumber = mSerialNumber,
                    mSystemCode = mSystemCode, mConfig = mConfig)
        }

        private const val NAME = "Nextfare"
        val FALLBACK_FACTORY: ClassicCardTransitFactory = NextFareTransitFactory()
        val MANUFACTURER = ImmutableByteArray.fromHex(
                "16181A1B1C1D1E1F"
        )
        private const val TAG = "NextfareTransitData"

        protected fun formatSerialNumber(serialNumber: Long): String {
            var s = "0160 " + NumberUtils.formatNumber(serialNumber, " ", 4, 4, 3)
            s += NumberUtils.calculateLuhn(s.replace(" ".toRegex(), ""))
            return s
        }
    }
}
