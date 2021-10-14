/*
 * YarGorTransitData.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.yargor

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class YarGorTransitData(private val mSerial: Long,
                        private val mLastTrip: YarGorTrip?,
                        private val mSub: YarGorSubscription) : TransitData() {

    override val serialNumber: String?
        get() = formatSerial(mSerial)

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_yargor)

    override val balance: TransitBalance?
        get() = null

    override val trips: List<YarGorTrip>
        get() = listOfNotNull(mLastTrip)

    override val subscriptions: List<YarGorSubscription>
        get() = listOf(mSub)

    companion object {
        val TZ = MetroTimeZone.MOSCOW

        fun parse(card: ClassicCard): YarGorTransitData {
            return YarGorTransitData(
                    mSub = YarGorSubscription.parse(card[10]),
                    mLastTrip = YarGorTrip.parse(card[12, 0].data),
                    mSerial = getSerial(card)
            )
        }

        fun getSerial(card: ClassicCard): Long = card.tagId.byteArrayToLongReversed()

        fun formatSerial(serial: Long): String = NumberUtils.groupString((serial + 90000000000L).toString(), ".", 4)
    }
}
