/*
 * YarGorSubscription.kt
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

import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

@Parcelize
data class YarGorSubscription(override val validFrom: Daystamp,
                              override val validTo: Daystamp,
                              override val purchaseTimestamp: TimestampFull,
                              private val mType: Int,
                              private val mL: ImmutableByteArray,
                              private val mM: Byte,
                              private val mTransports: Byte,
                              private val mN: ImmutableByteArray,
                              private val mChecksum: ImmutableByteArray
) : Subscription() {

    override val subscriptionName: String?
        get() = when (mType) {
            0x9613 -> Localizer.localizeString(R.string.yargor_sub_weekday_tram)
            0x9615 -> Localizer.localizeString(R.string.yargor_sub_weekday_trolley)
            0x9621 -> Localizer.localizeString(R.string.yargor_sub_allday_all)
            else -> Localizer.localizeString(R.string.unknown_format, mType.toString(16))
        }

    private val transportsDesc: String
        get() {
            val t = mutableListOf<String>()
            for (i in 0..7) {
                if ((mTransports.toInt() and (0x1 shl i)) != 0)
                    t += when (i) {
                        0 -> Localizer.localizeString(R.string.mode_bus)
                        1 -> Localizer.localizeString(R.string.mode_tram)
                        2 -> Localizer.localizeString(R.string.mode_trolleybus)
                        else -> Localizer.localizeString(R.string.unknown_format, i)
                    }
            }
            return t.joinToString()
        }

    override val info: List<ListItem>?
        get() = listOf(
                ListItem(R.string.yargor_transports_valid, transportsDesc)
        )

    override fun getRawFields(level: TransitData.RawLevel): List<ListItem> =
            listOf(
                    ListItem(FormattedString("L"), mL.toHexDump()),
                    ListItem("M", mM.hexString),
                    ListItem(FormattedString("N"), mN.toHexDump())
                ) + if (level == TransitData.RawLevel.ALL)
                listOf(
                        ListItem("Type", mType.toString(16)),
                        ListItem(FormattedString("Checksum"), mChecksum.toHexDump())
            ) else listOf()

    companion object {
        private fun parseDate(data: ImmutableByteArray, off: Int): Daystamp =
                Daystamp(year = 2000 + (data[off].toInt() and 0xff),
                        month = (data[off + 1].toInt() and 0xff) - 1,
                        day = (data[off + 2].toInt() and 0xff))

        private fun parseTimestamp(data: ImmutableByteArray, off: Int): TimestampFull =
                TimestampFull(tz = YarGorTransitData.TZ,
                        year = 2000 + (data[off].toInt() and 0xff),
                        month = (data[off + 1].toInt() and 0xff) - 1,
                        day = data[off + 2].toInt() and 0xff,
                        hour = data[off + 3].toInt() and 0xff,
                        min = data[off + 4].toInt() and 0xff)

        fun parse(sector: ClassicSector): YarGorSubscription {
            val block0 = sector[0].data
            val block1 = sector[1].data
            return YarGorSubscription(
                    mType = block0.byteArrayToInt(0, 2),
                    validFrom = parseDate(block0, 2),
                    validTo = parseDate(block0, 5),
                    mL = block0.sliceOffLen(8, 6),
                    mTransports = block0[14],
                    mM = block0[15],
                    purchaseTimestamp = parseTimestamp(block1, 0),
                    mN = block1.sliceOffLen(5, 5),
                    mChecksum = block1.sliceOffLen(10, 6)
            )
        }
    }
}