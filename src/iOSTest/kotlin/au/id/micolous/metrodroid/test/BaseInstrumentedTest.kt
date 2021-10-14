package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.ConcurrentFileReader
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.Preferences
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSFileManager

actual abstract class BaseInstrumentedTestPlatform actual constructor() {
    actual fun setLocale(languageTag: String) {
        // Preferences.languageActual = languageTag.substringBefore("-")
        //Locale.setDefault(Locale.forLanguageTag(languageTag)) TODO
    }

    actual fun loadAssetSafe(path: String): Input? =
        ConcurrentFileReader.openFile(fullPathForAsset(path))?.makeInput()

    actual fun listAsset(path: String): List<String>? =
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(fullPathForAsset(path), null)
            ?.filterIsInstance<String>()

    private fun fullPathForAsset(path: String): String = NSBundle.mainBundle.resourcePath + "/$path"
}
