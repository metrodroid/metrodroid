package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.util.ImmutableByteArray

data class NdefHead(
    val me: Boolean,
    val cf: Boolean,
    val tnf: Int,
    val typeLen: Int,
    val payloadLen: Int,
    val idLen: Int?,
    val headLen: Int
) {
    companion object {
        fun parse(data: ImmutableByteArray, ptrStart: Int): NdefHead? {
            var ptr = ptrStart
            val head = data[ptr]
            val mb = (head.toInt() and 0x80) != 0
            if (mb != (ptr == 0))
                return null
            val me = (head.toInt() and 0x40) != 0
            val cf = (head.toInt() and 0x20) != 0
            val sr = (head.toInt() and 0x10) != 0
            val il = (head.toInt() and 0x08) != 0
            val tnf = (head.toInt() and 0x07)
            ptr++
            val typeLen = data[ptr++].toInt() and 0xff
            val payloadLenSize = if (sr) 1 else 4
            val payloadLen = data.byteArrayToInt(ptr, payloadLenSize)
            ptr += payloadLenSize
            val idLen = if (il) data[ptr++].toInt() and 0xff else null
            return NdefHead(
                me = me,
                cf = cf,
                tnf = tnf,
                typeLen = typeLen,
                payloadLen = payloadLen,
                idLen = idLen,
                headLen = ptr - ptrStart
            )
        }
    }
}