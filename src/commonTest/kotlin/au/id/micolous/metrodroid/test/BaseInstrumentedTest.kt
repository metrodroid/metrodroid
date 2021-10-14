package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.Input
import kotlin.test.assertNotNull

expect abstract class BaseInstrumentedTestPlatform() {
    fun setLocale(languageTag: String)
    fun loadAssetSafe(path: String) : Input?
    fun listAsset(path: String) : List <String>?
}

abstract class BaseInstrumentedTest : BaseInstrumentedTestPlatform() {
    fun loadSmallAssetBytesSafe(path: String): ByteArray? {
        val s = loadAssetSafe(path) ?: return null
        val out = s.readBytes(MAX_SMALL_SIZE + 1)
        if (out.size > MAX_SMALL_SIZE) {
            throw Exception("Expected 0 - $MAX_SMALL_SIZE bytes")
        }

        return out
    }

    fun loadSmallAssetBytes(path: String): ByteArray {
        val res = loadSmallAssetBytesSafe(path)
        assertNotNull(res, "File $path not found")
        return res
    }

    fun loadAsset(path: String) : Input {
        val stream = loadAssetSafe(path)
        assertNotNull(stream, "File $path not found")
        return stream
    }

    companion object {
        const val MAX_SMALL_SIZE = 1048576
    }
}