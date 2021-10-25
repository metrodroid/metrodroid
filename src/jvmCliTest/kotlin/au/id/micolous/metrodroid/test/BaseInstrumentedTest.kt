package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.JavaStreamInput
import au.id.micolous.metrodroid.util.Preferences
import java.io.File
import java.io.InputStream
import java.util.Locale

actual fun loadAssetStream(path: String): InputStream? {
    val uri = BaseInstrumentedTest::class.java.getResource("/$path")?.toURI() ?: return null
    val file = File(uri)
    return file.inputStream()
}

actual abstract class BaseInstrumentedTestPlatform actual constructor() {
    actual fun setLocale(languageTag: String) {
        Preferences.languageActual = languageTag.substringBefore("-")
        Locale.setDefault(Locale.forLanguageTag(languageTag))
    }

    actual fun loadAssetSafe(path: String) : Input? =
        loadAssetStream(path)?.let {
            JavaStreamInput(it)
        }

    actual fun listAsset(path: String) : List <String>? {
        val uri = BaseInstrumentedTest::class.java.getResource("/$path")?.toURI() ?: return null
        val file = File(uri)
        return file.list()?.toList()
    }
}
