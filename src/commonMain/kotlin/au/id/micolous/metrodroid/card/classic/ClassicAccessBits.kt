package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.util.ImmutableByteArray

class ClassicAccessBits private constructor(private val C1: Int, private val C2: Int, private val C3: Int) {
    private fun getSlot(slot: Int) =
            (((C1 shr slot) and 0x1) shl 2) or
                    (((C2 shr slot) and 0x1) shl 1) or
                    ((C3 shr slot) and 0x1)

    constructor(raw: ImmutableByteArray) : this(
            C1 = (raw[1].toInt() and 0xf0) shr 4,
            C2 = raw[2].toInt() and 0xf,
            C3 = (raw[2].toInt() and 0xf0) shr 4
    )

    fun isDataBlockReadable(slot: Int, keyType: ClassicSectorKey.KeyType) =
            when (getSlot(slot)) {
                0, 1, 2, 4, 6 -> true
                3, 5 -> keyType == ClassicSectorKey.KeyType.B && !isKeyBReadable
                7 -> false
                else -> throw IllegalArgumentException()
            }

    val isKeyBReadable get() = getSlot(3) in listOf(0, 1, 2)

    companion object {
        fun isAccBitsValid(accBits: ImmutableByteArray): Boolean {
            val c123inv = (accBits[0].toInt() and 0xff) or ((accBits[1].toInt() and 0xf) shl 8)
            val c123 = ((accBits[1].toInt() and 0xf0) shr 4) or ((accBits[2].toInt() and 0xff) shl 4)
            return c123inv == c123.inv() and 0xfff
        }
    }
}
