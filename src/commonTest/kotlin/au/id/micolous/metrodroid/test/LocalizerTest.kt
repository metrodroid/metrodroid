package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizerTest : BaseInstrumentedTest() {
    @Test
    fun testRussian() {
        setLocale("ru-RU")
        assertEquals("Импортировано 0 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 0, 0))
        assertEquals("Импортирована 1 карта.",
            Localizer.localizePlural(R.plurals.cards_imported, 1, 1))
        assertEquals("Импортированы 2 карты.",
            Localizer.localizePlural(R.plurals.cards_imported, 2, 2))
        assertEquals("Импортировано 5 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 5, 5))
        assertEquals("Импортировано 10 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 10, 10))
        assertEquals("Импортировано 11 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 11, 11))
        assertEquals("Импортировано 12 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 12, 12))
        assertEquals("Импортировано 13 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 13, 13))
        assertEquals("Импортировано 14 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 14, 14))
        assertEquals("Импортировано 15 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 15, 15))
        assertEquals("Импортирована 21 карта.",
            Localizer.localizePlural(R.plurals.cards_imported, 21, 21))
        assertEquals("Импортированы 22 карты.",
            Localizer.localizePlural(R.plurals.cards_imported, 22, 22))
        assertEquals("Импортировано 25 карт.",
            Localizer.localizePlural(R.plurals.cards_imported, 25, 25))
    }

    @Test
    fun testFrench() {
        setLocale("fr-FR")
        assertEquals("0 carte importée.",
            Localizer.localizePlural(R.plurals.cards_imported, 0, 0))
        assertEquals("1 carte importée.",
            Localizer.localizePlural(R.plurals.cards_imported, 1, 1))
        assertEquals("2 cartes importées.",
            Localizer.localizePlural(R.plurals.cards_imported, 2, 2))
    }

    @Test
    fun testIndonesian() {
        setLocale("in-IN")
        assertEquals("0 card imported!",
            Localizer.localizePlural(R.plurals.cards_imported, 0, 0))
        assertEquals("1 card imported!",
            Localizer.localizePlural(R.plurals.cards_imported, 1, 1))
        assertEquals("2 card imported!",
            Localizer.localizePlural(R.plurals.cards_imported, 2, 2))
    }

    @Test
    fun testHebrew() {
        setLocale("he-IL")
        assertEquals("כרטיס אחד יובא.",
            Localizer.localizePlural(R.plurals.cards_imported, 1, 1))
        assertEquals("2 כרטיסים יובאו.",
            Localizer.localizePlural(R.plurals.cards_imported, 2, 2))
        assertEquals("3 כרטיסים יובאו.",
            Localizer.localizePlural(R.plurals.cards_imported, 3, 3))
        assertEquals("20 כרטיסים יובאו.",
            Localizer.localizePlural(R.plurals.cards_imported, 20, 20))

        assertEquals("תקף לחודש",
            Localizer.localizePlural(R.plurals.lisboaviva_valid_months, 1, 1))
        assertEquals("תקף לחודשיים",
            Localizer.localizePlural(R.plurals.lisboaviva_valid_months, 2, 2))
        assertEquals("תקף ל־3 חודשים",
            Localizer.localizePlural(R.plurals.lisboaviva_valid_months, 3, 3))
        assertEquals("תקף ל־10 חודשים",
            Localizer.localizePlural(R.plurals.lisboaviva_valid_months, 10, 10))
        assertEquals("תקף ל־20 חודשים",
            Localizer.localizePlural(R.plurals.lisboaviva_valid_months, 20, 20))
    }
}