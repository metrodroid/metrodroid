package au.id.micolous.metrodroid.transit.trans_semarang_e_card

import au.id.micolous.metrodroid.util.ImmutableByteArray

object TransSemarangECardUtils {
    // Size of a MIFARE Classic block in bytes.
    private const val BLOCK_SIZE = 16

    // Length of the 32-bit value in a value block.
    private const val VALUE_LEN = 4

    // Offset of the bitwise-inverted copy of the value.
    private const val VALUE_INVERTED_OFFSET = 4

    // Offset of the second plain copy of the value.
    private const val VALUE_COPY_OFFSET = 8

    // First printable ASCII character (space).
    private const val ASCII_PRINTABLE_FIRST = 0x20

    // Last printable ASCII character (tilde).
    private const val ASCII_PRINTABLE_LAST = 0x7E

    /**
     * Standard MIFARE Classic "value block" format:
     * bytes 0-3: value (LE), 4-7: ~value, 8-11: value again, 12-15: addr/~addr/addr/~addr.
     *
     * Only the three copies of the value are checked; the address bytes are ignored.
     */
    fun isValidValueBlock(data: ImmutableByteArray): Boolean {
        if (data.size != BLOCK_SIZE) return false
        for (i in 0 until VALUE_LEN) {
            val value = data[i].toInt() and 0xFF
            val inverted = data[i + VALUE_INVERTED_OFFSET].toInt() and 0xFF
            val copy = data[i + VALUE_COPY_OFFSET].toInt() and 0xFF
            if (value != copy || (value xor inverted) != 0xFF) return false
        }
        return true
    }

    /** Extracts printable ASCII from a raw block, trimming padding bytes. */
    fun parseAscii(data: ImmutableByteArray): String {
        val sb = StringBuilder()
        for (i in 0 until data.size) {
            val b = data[i].toInt() and 0xFF
            if (b in ASCII_PRINTABLE_FIRST..ASCII_PRINTABLE_LAST) sb.append(b.toChar())
        }
        return sb.toString().trim()
    }
}
