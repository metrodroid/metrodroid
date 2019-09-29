package au.id.micolous.metrodroid.transit.rkf

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData.Companion.ID_FIELD
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData.Companion.STATUS_FIELD

@Parcelize
data class RkfTicket(override val parsed: En1545Parsed, override val lookup: RkfLookup): En1545Subscription() {
    companion object {
        fun parse (record: RkfTctoRecord, lookup: RkfLookup): RkfTicket {
            Log.d("RkfTicket", "TCCO = ${record.chunks}")
            val version = record.chunks[0][0].getBitsFromBufferLeBits(8, 6)
            val maxTxn = record.chunks.filter { it[0][0] == 0x88.toByte() }.map { it[0].getBitsFromBufferLeBits(8, 12) }.max()
            val flat = record.chunks.filter { it[0][0] != 0x88.toByte() || it[0].getBitsFromBufferLeBits(8, 12) == maxTxn }.flatten()
            val parsed = En1545Parsed()
            for (tag in flat) {
                Log.d("RkfTicket", "TCCO tag $tag")
                val fields = getFields (tag[0], version) ?: continue
                parsed.appendLeBits(tag, fields)
            }
            return RkfTicket(parsed, lookup)
        }
        @Suppress("UNUSED_PARAMETER")
        private fun getFields(id: Byte, version: Int): En1545Field? = when (id.toInt() and 0xff) {
            0x87 -> En1545Container(
                    ID_FIELD, // verified
                    RkfTransitData.VERSION_FIELD // verified
            )
            0x88 -> En1545Container(
                    ID_FIELD, // verified
                    En1545FixedInteger("TransactionNumber", 12) // verified
            )
            0x89 -> En1545Container(
                    ID_FIELD, // verified
                    En1545FixedInteger(CONTRACT_PROVIDER, 12), // verified
                    En1545FixedInteger(CONTRACT_TARIFF, 12),
                    En1545FixedInteger(CONTRACT_SALE_DEVICE, 16),
                    En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                    STATUS_FIELD)
            0x96 -> En1545Container(
                    ID_FIELD, // verified
                    En1545FixedInteger.date(En1545Subscription.CONTRACT_START), // verified
                    En1545FixedInteger.timePacked16(En1545Subscription.CONTRACT_START), // verified
                    En1545FixedInteger.date(En1545Subscription.CONTRACT_END), // verified
                    En1545FixedInteger.timePacked16(En1545Subscription.CONTRACT_END), // verified
                    En1545FixedInteger(CONTRACT_DURATION, 8),  // verified, days
                    En1545FixedInteger.date("Limit"),
                    En1545FixedInteger("PeriodJourneys", 8),
                    En1545FixedInteger("RestrictDay", 8),
                    En1545FixedInteger("RestrictTimecode", 8)
            )
            else -> null
        }
    }
}
