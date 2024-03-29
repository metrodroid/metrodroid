package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.JavaStreamInput
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
        Locale.setDefault(Locale.forLanguageTag(languageTag))
        Localizer.loadDefaultLocale()
    }

    actual fun loadAssetSafe(path: String) : Input? =
        loadAssetStream(path)?.let {
            JavaStreamInput(it)
        }
}
