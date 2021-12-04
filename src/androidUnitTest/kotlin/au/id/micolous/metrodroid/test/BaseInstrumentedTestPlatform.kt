/*
 * BaseInstrumentedTestPlatform.kt
 *
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.test

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.KITKAT
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.multi.Plurals
import au.id.micolous.metrodroid.util.Input
import au.id.micolous.metrodroid.util.JavaStreamInput
import org.junit.runner.RunWith
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.FrameworkMethod
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.*
import java.util.concurrent.ConcurrentHashMap


actual fun loadAssetStream(path: String): InputStream? {
    val uri = BaseInstrumentedTest::class.java.getResource("/$path")?.toURI() ?: return null
    val file = File(uri)
    return file.inputStream()
}

class ConditionalTestRunner(val klass: Class<*>?) : RobolectricTestRunner(klass) {
    override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
        val conditionClass: AndroidMinSdk? = klass?.getAnnotation(AndroidMinSdk::class.java)
        val condition: AndroidMinSdk? = method.getAnnotation(AndroidMinSdk::class.java)
        val curSdk = (method as? RobolectricFrameworkMethod)?.sdk?.apiLevel
        val minSdk = listOfNotNull(condition?.minSdk, conditionClass?.minSdk).maxOrNull()

        if (minSdk != null && curSdk != null && curSdk < minSdk) {
            notifier.fireTestIgnored(describeChild(method))
        } else {
            super.runChild(method, notifier)
        }
    }
}

// Robolectric supports only english plurals on pre-M. Replace it withour implementation
@Implements(className = "libcore.icu.NativePluralRules", isInAndroidSdk = false,
    maxSdk = Build.VERSION_CODES.M)
class ShadowNativePluralRules {
    companion object {
        private val langs = ConcurrentHashMap<Int, String>()
        private val langRev = ConcurrentHashMap<String, Int>()
        private var ctr = 0

        @JvmStatic
        @Implementation(minSdk = KITKAT)
        fun quantityForIntImpl(address: Long, quantity: Int): Int =
            quantityForIntImplReal(address.toInt(), quantity)

        private fun quantityForIntImplReal(address: Int, quantity: Int): Int =
            mp[Plurals.getQuantityString(langs[address]!!, quantity)]!!

        @Implementation(maxSdk = JELLY_BEAN_MR2)
        @JvmStatic
        fun quantityForIntImpl(address: Int, quantity: Int): Int =
            quantityForIntImplReal(address, quantity)

        @JvmStatic
        @JvmName("forLocaleImpl")
        fun forLocaleImplInt(localeName: String): Int =
            forLocaleImplReal(localeName)

        private fun forLocaleImplReal(localeName: String): Int {
            val lang = localeName.substringBefore('-').substringBefore('_')
            synchronized(langs) {
                var cand = langRev[lang]
                if (cand != null)
                    return cand
                cand = ctr++
                langRev[lang] = cand
                langs[cand] = lang
                return cand
            }
        }
        private val mp = mapOf(
                "zero" to 0,
                "one" to 1,
                "two" to 2,
                "few" to 3,
                "many" to 4,
                "other" to 5
            )
    }
}


@RunWith(ConditionalTestRunner::class)
@Config(sdk = [16, 17, 19, 21, 23, 24, 28],
    shadows = [ShadowNativePluralRules::class])
actual abstract class BaseInstrumentedTestPlatform {
    val context : Context
        get() = MetrodroidApplication.instance

    actual fun setLocale(languageTag: String) {
        LocaleTools.setLocale(languageTag, context.resources)
    }

    actual fun loadAssetSafe(path: String) : Input? = loadAssetStream(path)?.let {
        JavaStreamInput(it)
    }

    actual fun listAsset(path: String) : List <String>? = null
}
