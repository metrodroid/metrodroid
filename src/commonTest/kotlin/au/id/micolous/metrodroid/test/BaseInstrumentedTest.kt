package au.id.micolous.metrodroid.test

import kotlinx.io.IOException
import kotlinx.io.InputStream
import kotlin.test.assertNotNull

expect fun <T> runAsync(block: suspend () -> T)

expect abstract class BaseInstrumentedTestPlatform() {
    fun setLocale(languageTag: String)
    fun showRawStationIds(state: Boolean)
    fun showLocalAndEnglish(state: Boolean)
    fun loadAssetSafe(path: String) : InputStream?
    fun listAsset(path: String) : List <String>?
}

abstract class BaseInstrumentedTest : BaseInstrumentedTestPlatform() {
    fun loadSmallAssetBytesSafe(path: String): ByteArray? {
        val s = loadAssetSafe(path) ?: return null
        val length = s.available()
        if (length > 1048576 || length <= 0) {
            throw IOException("Expected 0 - 1048576 bytes")
        }

        val out = ByteArray(length)
        val realLen = s.read(out)

        // Return truncated buffer
        return out.sliceArray(0 until realLen)
    }

    fun loadSmallAssetBytes(path: String): ByteArray {
        val res = loadSmallAssetBytesSafe(path)
        assertNotNull(res, "File $path not found")
        return res
    }

    fun loadAsset(path: String) : InputStream {
        val stream = loadAssetSafe(path)
        assertNotNull(stream, "File $path not found")
        return stream
    }
}