/*
 * TransitCurrencyTest.java
 *
 * Copyright 2017-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.text.Spanned

import org.hamcrest.Matchers
import org.junit.Test

import au.id.micolous.metrodroid.transit.TransitCurrency

/**
 * Tests the currency formatter.
 */
class TransitCurrencyTest : BaseInstrumentedTest() {

    /**
     * In Australian English, AUD should come out as a bare "$", and USD should come out with some
     * different prefix.
     */
    @Test
    fun testEnglishAU() {
        // Note: en_AU data in Unicode CLDR currency data was broken in release
        // 28, Android 7.0+:
        // https://unicode.org/cldr/trac/changeset/11798/trunk/common/main/en_AU.xml
        // https://unicode.org/cldr/trac/ticket/10217
        // Only check to make sure AUD comes out correctly in en_AU.
        setLocale("en-AU")

        val aud = TransitCurrency.AUD(1234).formatCurrencyString(true).spanned
        assertSpannedEquals("$12.34", aud)
        assertTtsMarkers("AUD", "12", "34", aud)

        val aud00 = TransitCurrency.AUD(1200).formatCurrencyString(true).spanned
        assertSpannedEquals("$12.00", aud00)
        assertTtsMarkers("AUD", "12", "00", aud00)

        val aud02 = TransitCurrency.AUD(1202).formatCurrencyString(true).spanned
        assertSpannedEquals("$12.02", aud02)
        assertTtsMarkers("AUD", "12", "02", aud02)

        val audMinus = TransitCurrency.AUD(-1202).formatCurrencyString(true).spanned
        assertSpannedEquals("-$12.02", audMinus)
        assertTtsMarkers("AUD", "-12", "02", audMinus)

        // May be "USD12.34", "U$12.34" or "US$12.34".
        val usd = TransitCurrency.USD(1234).formatCurrencyString(true).spanned
        assertSpannedThat(usd, Matchers.startsWith("U"))
        assertSpannedThat(usd, Matchers.endsWith("12.34"))
        assertTtsMarkers("USD", "12", "34", usd)
    }

    /**
     * In British English, everything should come out pretty similar.
     *
     * It might clarify USD (US$ vs. $). but that isn't very important.
     */
    @Test
    fun testEnglishGB() {
        setLocale("en-GB")

        // May be "$12.34", "U$12.34" or "US$12.34".
        val usd = TransitCurrency.USD(1234).formatCurrencyString(true).spanned
        assertSpannedThat(usd, Matchers.endsWith("$12.34"))
        assertTtsMarkers("USD", "12", "34", usd)

        // May be "A$12.34" or "AU$12.34".
        val aud = TransitCurrency.AUD(1234).formatCurrencyString(true).spanned
        assertSpannedThat(aud, Matchers.startsWith("A"))
        assertSpannedThat(aud, Matchers.endsWith("$12.34"))
        assertTtsMarkers("AUD", "12", "34", aud)

        val gbp = TransitCurrency(1234, "GBP").formatCurrencyString(true).spanned
        assertSpannedEquals("£12.34", gbp)
        assertTtsMarkers("GBP", "12", "34", gbp)

        // May be "¥1,234" or "JP¥1,234".
        val jpy = TransitCurrency.JPY(1234).formatCurrencyString(true).spanned
        assertSpannedThat(jpy, Matchers.endsWith("¥1,234"))
        assertTtsMarkers("JPY", "1234", null, jpy)
    }

    /**
     * In American English, USD should come out as a bare "$", and AUD should come out with some
     * different prefix.
     */
    @Test
    fun testEnglishUS() {
        setLocale("en-US")

        val usd = TransitCurrency.USD(1234).formatCurrencyString(true).spanned
        assertSpannedEquals("$12.34", usd)
        assertTtsMarkers("USD", "12", "34", usd)

        // May be "A$12.34" or "AU$12.34".
        val aud = TransitCurrency.AUD(1234).formatCurrencyString(true).spanned
        assertSpannedThat(aud, Matchers.startsWith("A"))
        assertSpannedThat(aud, Matchers.endsWith("$12.34"))
        assertTtsMarkers("AUD", "12", "34", aud)

        val gbp = TransitCurrency(1234, "GBP").formatCurrencyString(true).spanned
        assertSpannedEquals("£12.34", gbp)
        assertTtsMarkers("GBP", "12", "34", gbp)

        // May be "¥1,234" or "JP¥1,234".
        val jpy = TransitCurrency.JPY(1234).formatCurrencyString(true).spanned
        assertSpannedThat(jpy, Matchers.endsWith("¥1,234"))
        assertTtsMarkers("JPY", "1234", null, jpy)
    }

    /**
     * In Japanese, everything should come out pretty similar.  But the Yen character is probably
     * full-width.
     *
     * It might clarify USD (US$ vs. $). but that isn't very important.
     */
    @Test
    fun testJapanese() {
        setLocale("ja-JP")

        // May be "$12.34", "U$12.34" or "US$12.34".
        val usd = TransitCurrency.USD(1234).formatCurrencyString(true).spanned
        assertSpannedThat(usd, Matchers.endsWith("$12.34"))
        assertTtsMarkers("USD", "12", "34", usd)

        // May be "A$12.34" or "AU$12.34".
        val aud = TransitCurrency.AUD(1234).formatCurrencyString(true).spanned
        assertSpannedThat(aud, Matchers.startsWith("A"))
        assertSpannedThat(aud, Matchers.endsWith("$12.34"))
        assertTtsMarkers("AUD", "12", "34", aud)

        val gbp = TransitCurrency(1234, "GBP").formatCurrencyString(true).spanned
        assertSpannedEquals("£12.34", gbp)
        assertTtsMarkers("GBP", "12", "34", gbp)


        // Note: this is the full-width yen character
        val jpy = TransitCurrency.JPY(1234).formatCurrencyString(true).spanned
        assertSpannedEquals("￥1,234", jpy)
        assertTtsMarkers("JPY", "1234", null, jpy)
    }

    /**
     * In French, comma is used as a decimal separator, spaces are used for grouping, and currency
     * symbols are after the amount. TTS data must have an English formatting style.
     */
    @Test
    fun testFrench() {
        setLocale("fr-FR")

        val usd = TransitCurrency.USD(1234).formatCurrencyString(true).spanned
        assertSpannedEquals("12,34 \$US", usd)
        assertTtsMarkers("USD", "12", "34", usd)

        val aud = TransitCurrency.AUD(1234).formatCurrencyString(true).spanned
        assertSpannedEquals("12,34 \$AU", aud)
        assertTtsMarkers("AUD", "12", "34", aud)

        // Allow not qualifying the country code.
        val gbp = TransitCurrency(1234, "GBP").formatCurrencyString(true).spanned
        assertSpannedThat(gbp, Matchers.startsWith("12,34 £"))
        assertTtsMarkers("GBP", "12", "34", gbp)

        // This may not have a proper symbol
        val jpy = TransitCurrency.JPY(1234).formatCurrencyString(true).spanned
        // Accept either ordinary or narrow non-break space
        assertSpannedThat(jpy, Matchers.anyOf(Matchers.startsWith("1 234"), Matchers.startsWith("1 234")))

        val eur = TransitCurrency(1234, "EUR").formatCurrencyString(true).spanned
        assertSpannedEquals("12,34 €", eur)
        assertTtsMarkers("EUR", "12", "34", eur)
    }
}
