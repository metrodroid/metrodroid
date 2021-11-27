/*
 * KievTrip.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.kiev

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class KievTrip (
        override val startTimestamp: Timestamp?,
        private val mTransactionType: String?,
        private val mCounter1: Int,
        private val mCounter2: Int,
        private val mValidator: Int): Trip() {
    override val startStation: Station?
        get() = Station.unknown(mValidator)

    override val fare: TransitCurrency?
        get() = null

    override val mode: Mode
        get() = if (mTransactionType == "84/04/40/53") Mode.METRO else Mode.OTHER

    internal constructor(data: ImmutableByteArray) : this(
        startTimestamp = parseTimestamp(data),
        // This is a shameless plug. We have no idea which field
        // means what. But metro transport is always 84/04/40/53
        mTransactionType = (data.getHexString(0, 1)
                + "/" + data.getHexString(6, 1)
                + "/" + data.getHexString(8, 1)
                + "/" + data.getBitsFromBuffer(88, 10).toString(16)),
        mValidator = data.getBitsFromBuffer(56, 8),
        mCounter1 = data.getBitsFromBuffer(72, 16),
        mCounter2 = data.getBitsFromBuffer(98, 16))

    override fun getAgencyName(isShort: Boolean): FormattedString? {
        return if (mTransactionType == "84/04/40/53") Localizer.localizeFormatted(R.string.mode_metro) else mTransactionType?.let { Localizer.localizeFormatted(R.string.unknown_format, it) }
    }

    override fun getRawFields(level: TransitData.RawLevel): String
        = "mCounter1=$mCounter1,mCounter2=$mCounter2"

    companion object {
        private val TZ = MetroTimeZone.KIEV

        private fun parseTimestamp(data: ImmutableByteArray): TimestampFull {
            return TimestampFull(TZ, data.getBitsFromBuffer(17, 5) + 2000,
                    data.getBitsFromBuffer(13, 4) - 1,
                    data.getBitsFromBuffer(8, 5),
                    data.getBitsFromBuffer(33, 5),
                    data.getBitsFromBuffer(27, 6),
                    data.getBitsFromBuffer(22, 5))
        }
    }
}
