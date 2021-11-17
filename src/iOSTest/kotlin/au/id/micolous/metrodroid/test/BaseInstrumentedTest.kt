package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.serializers.XmlFormat
import au.id.micolous.metrodroid.util.ConcurrentFileReader
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.Preferences
import platform.Foundation.*
import kotlin.test.assertNotNull

private fun fullPathForAsset(path: String): String = NSBundle.mainBundle.resourcePath + "/$path"

actual fun loadCardXml(path: String): Card {
    var ret: Card? = null
    println("path = $path")
    val fullPath = fullPathForAsset(path)
    println("fullPath = $fullPath")
    val url = NSURL(fileURLWithPath = fullPath)
    println("url = $url")
    XmlFormat.readXmlFromUrl(url) {
        card -> if(ret == null)
            ret = card
    }
    assertNotNull(ret)
    return ret!!
}

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

    companion object {
        private val languageMap = mapOf("in" to "id")
    }
}
