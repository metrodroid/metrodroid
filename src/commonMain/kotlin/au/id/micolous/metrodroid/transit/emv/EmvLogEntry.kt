package au.id.micolous.metrodroid.transit.emv

import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.card.iso7816.TagDesc.Companion.interpretTag
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.emv.EmvData.TAGMAP
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils


@Parcelize
data class EmvLogEntry(private val values: Map<String, ImmutableByteArray>) : Trip() {
    override val startTimestamp get(): Timestamp? {
        val dateBin = values["9a"] ?: return null
        val timeBin = values["9f21"]
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
        val amountBin = values["9f02"] ?: return null
        val amount = amountBin.fold(0L) { acc, b ->
            acc * 100 + NumberUtils.convertBCDtoInteger(b.toInt() and 0xff)
        }

        val codeBin = values["5f2a"] ?: return TransitCurrency.XXX(amount.toInt())
        val code = NumberUtils.convertBCDtoInteger(codeBin.byteArrayToInt())

        return TransitCurrency(amount.toInt(), code)
    }

    override val mode get() = Mode.POS

    override val routeName get() = values.entries.filter {
        it.key != "9f02" && it.key != "5f2a"
                && it.key != "9a" && it.key != "9f21"
    }.joinToString {
        val tag = TAGMAP[it.key]
        if (tag == null)
            it.key + "=" + it.value.toHexString()
        else
            tag.name + "=" + interpretTag(tag.contents, it.value)
    }

    companion object {
        fun parseEmvTrip(record: ImmutableByteArray, format: ImmutableByteArray): EmvLogEntry? {
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
