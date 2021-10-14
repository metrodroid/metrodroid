package au.id.micolous.metrodroid.util

interface Output {
    fun write(b: ByteArray, off: Int = 0, sz: Int = b.size - off)
}
