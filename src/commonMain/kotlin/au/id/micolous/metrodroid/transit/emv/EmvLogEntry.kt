/*
 * EmvLogEntry.kt
 *
 * Copyright 2019 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.emv

import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.emv.EmvData.TAGMAP
import au.id.micolous.metrodroid.transit.emv.EmvData.TAG_AMOUNT_AUTHORISED
import au.id.micolous.metrodroid.transit.emv.EmvData.TAG_TRANSACTION_CURRENCY_CODE
import au.id.micolous.metrodroid.transit.emv.EmvData.TAG_TRANSACTION_DATE
import au.id.micolous.metrodroid.transit.emv.EmvData.TAG_TRANSACTION_TIME
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences


@Parcelize
data class EmvLogEntry(private val values: Map<String, ImmutableByteArray>) : Trip() {
    override val startTimestamp get(): Timestamp? {
        val dateBin = values[TAG_TRANSACTION_DATE] ?: return null
        val timeBin = values[TAG_TRANSACTION_TIME]
        if (timeBin != null)
            return TimestampFull(tz = MetroTimeZone.UNKNOWN,
                    year = 2000 + NumberUtils.convertBCDtoInteger(dateBin[0].toInt()),
                    month = NumberUtils.convertBCDtoInteger(dateBin[1].toInt()) - 1,
                    day = NumberUtils.convertBCDtoInteger(dateBin[2].toInt()),
                    hour = NumberUtils.convertBCDtoInteger(timeBin[0].toInt()),
                    min = NumberUtils.convertBCDtoInteger(timeBin[1].toInt()),
                    sec = NumberUtils.convertBCDtoInteger(timeBin[2].toInt()))
        return Daystamp(year = 2000 + NumberUtils.convertBCDtoInteger(dateBin[0].toInt()),
                month = NumberUtils.convertBCDtoInteger(dateBin[1].toInt()) - 1,
                day = NumberUtils.convertBCDtoInteger(dateBin[2].toInt()))
    }

    override val fare get(): TransitCurrency? {
        val amountBin = values[TAG_AMOUNT_AUTHORISED] ?: return null
        val amount = amountBin.fold(0L) { acc, b ->
            acc * 100 + NumberUtils.convertBCDtoInteger(b.toInt() and 0xff)
        }

        val codeBin = values[TAG_TRANSACTION_CURRENCY_CODE] ?: return TransitCurrency.XXX(amount.toInt())
        val code = NumberUtils.convertBCDtoInteger(codeBin.byteArrayToInt())

        return TransitCurrency(amount.toInt(), code)
    }

    override val mode get() = Mode.POS

    override val routeName get() = FormattedString(values.entries.filter {
        !HANDLED_TAGS.contains(it.key)
    }.joinToString {
        when (val tag = TAGMAP[it.key]) {
            // Unknown tag info
            null -> if (Preferences.hideCardNumbers || Preferences.obfuscateTripDates) {
                ""
            } else {
                it.key + "=" + it.value.toHexString()
            }

            // Unknown tag info
            else -> {
                val v = tag.interpretTag(it.value)
                if (v.isEmpty()) {
                    ""
                } else {
                    Localizer.localizeString(tag.name) + "=" + v
                }
            }
        }
    })

    companion object {
        private val HANDLED_TAGS = listOf(
                TAG_AMOUNT_AUTHORISED,
                TAG_TRANSACTION_CURRENCY_CODE,
                TAG_TRANSACTION_TIME,
                TAG_TRANSACTION_DATE)

        fun parseEmvTrip(record: ImmutableByteArray, format: ImmutableByteArray): EmvLogEntry? {
            // EMV transactions consist of the PDOL (format) which references offsets in the
            // record data.
            val values = mutableMapOf<String, ImmutableByteArray>()
            var p = 0
            val dol = ISO7816TLV.removeTlvHeader(format)
            ISO7816TLV.pdolIterate(dol) { id, len ->
                if (p + len <= record.size)
                    values[id.toHexString()] = record.sliceArray(p until p + len)
                p += len
            }
            return EmvLogEntry(values = values)
        }
    }
}
