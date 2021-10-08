package au.id.micolous.metrodroid.util

interface Input {
    fun readBytes(sz: Int): ByteArray
    fun readToString(): String
}
