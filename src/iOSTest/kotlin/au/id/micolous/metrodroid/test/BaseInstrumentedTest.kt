package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.ConcurrentFileReader
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.Preferences
import platform.Foundation.*

actual abstract class BaseInstrumentedTestPlatform actual constructor() {
    actual fun setLocale(languageTag: String) {
        val mapppedLang = languageTag.substringBefore('-').let {
            languageMap[it] ?: it
        }
        Preferences.languageOverrideForTest.value = mapppedLang
        Preferences.localeOverrideForTest.value = NSLocale(localeIdentifier = languageTag)
        NSUserDefaults.standardUserDefaults.setObject(
                listOf(mapppedLang) as NSArray,
                forKey = "AppleLanguages")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun loadAssetSafe(path: String): Input? =
        ConcurrentFileReader.openFile(fullPathForAsset(path))?.makeInput()

    actual fun listAsset(path: String): List<String>? =
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(fullPathForAsset(path), null)
            ?.filterIsInstance<String>()

    private fun fullPathForAsset(path: String): String = NSBundle.mainBundle.resourcePath + "/$path"

    companion object {
        val languageMap = mapOf("in" to "id")
    }
}
