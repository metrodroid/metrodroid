package au.id.micolous.metrodroid.test

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.text.Spanned
import android.text.style.TtsSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.util.ImmutableMapBuilder
import junit.framework.TestCase.assertEquals
import org.apache.commons.lang3.ArrayUtils
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.runner.RunWith
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

@RunWith(AndroidJUnit4::class)
abstract class BaseInstrumentedTest {
    val context : Context
            get() = InstrumentationRegistry.getInstrumentation().context

    /**
     * Sets the Android and Java locales to a different language
     * and country. Does not clean up after execution, and should
     * only be used in tests.
     *
     * @param languageTag ITEF BCP-47 language tag string
     */
    fun setLocale(languageTag: String) {
        val l = compatLocaleForLanguageTag(languageTag)
        Locale.setDefault(l)
        val r = context.resources
        val c = r.configuration
        c.setLocale(l)
        r.updateConfiguration(c, r.displayMetrics)
    }

    /**
     * Sets a boolean preference.
     * @param preference Key to the preference
     * @param value Desired state of the preference.
     */
    private fun setBooleanPref(preference: String, value: Boolean) {
        val prefs = MetrodroidApplication.getSharedPreferences()
        prefs.edit()
                .putBoolean(preference, value)
                .apply()
    }

    fun showRawStationIds(state: Boolean) {
        setBooleanPref(MetrodroidApplication.PREF_SHOW_RAW_IDS, state)
    }

    fun showLocalAndEnglish(state: Boolean) {
        setBooleanPref(MetrodroidApplication.PREF_SHOW_LOCAL_AND_ENGLISH, state)
    }

    fun loadAsset(path: String) : InputStream {
        return DataInputStream(context.assets.open(path, AssetManager.ACCESS_RANDOM))
    }

    fun loadSmallAssetBytes(path: String): ByteArray {
        val s = loadAsset(path)
        val length = s.available()
        if (length > 10240 || length <= 0) {
            throw IOException("Expected 0 - 10240 bytes")
        }

        val out = ByteArray(length)
        val realLen = s.read(out)

        // Return truncated buffer
        return ArrayUtils.subarray(out, 0, realLen)
    }

    fun assertSpannedEquals(expected: String, actual: Spanned) {
        // nbsp -> sp
        val actualString = actual.toString().replace(' ', ' ')
        assertEquals(expected, actualString)
    }

    fun assertSpannedThat(actual: Spanned, matcher: Matcher<in String>) {
        // nbsp -> sp
        val actualString = actual.toString().replace(' ', ' ')
        assertThat<String>(actualString, matcher)
    }

    fun assertTtsMarkers(currencyCode: String, value: String, span: Spanned) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }

        val ttsSpans = span.getSpans(0, span.length, TtsSpan::class.java)
        assertEquals(1, ttsSpans.size)

        assertEquals(TtsSpan.TYPE_MONEY, ttsSpans[0].type)
        val bundle = ttsSpans[0].args
        assertEquals(currencyCode, bundle.getString(TtsSpan.ARG_CURRENCY))
        assertEquals(value, bundle.getString(TtsSpan.ARG_INTEGER_PART))
    }

    companion object {
        private val LOCALES = ImmutableMapBuilder<String, Locale>()
                .put("en", Locale.ENGLISH)
                .put("en-AU", Locale("en", "AU"))
                .put("en-CA", Locale.CANADA)
                .put("en-GB", Locale.UK)
                .put("en-US", Locale.US)
                .put("fr", Locale.FRENCH)
                .put("fr-CA", Locale.CANADA_FRENCH)
                .put("fr-FR", Locale.FRANCE)
                .put("ja", Locale.JAPANESE)
                .put("ja-JP", Locale.JAPAN)
                .put("zh-CN", Locale.SIMPLIFIED_CHINESE)
                .put("zh-TW", Locale.TRADITIONAL_CHINESE)
                .build()

        private fun compatLocaleForLanguageTag(languageTag: String): Locale {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Locale.forLanguageTag(languageTag)
            } else {
                LOCALES.get(languageTag)
                        ?: throw IllegalArgumentException("For API < 21, add entry to LOCALES for: $languageTag")
            }
        }
    }
}