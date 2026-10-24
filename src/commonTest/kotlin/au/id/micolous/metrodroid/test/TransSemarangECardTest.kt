/*
 * TransSemarangECardTest.kt
 *
 * Tests for the Trans Semarang E-Card parser.
 */

package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.trans_semarang_e_card.TransSemarangECardTransitData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransSemarangECardTest :
    CardReaderWithAssetDumpsTest<JsonKotlinFormat>(JsonKotlinFormat) {

    private fun checkCard(
        path: String,
        expectedBalance: Int,
        expectedSerial: String,
    ) {
        val card = loadCard<ClassicCard>(path)

        // Check that the MIFARE Classic data was read
        assertNotNull(
            card.mifareClassic,
            "MIFARE Classic data should exist"
        )

        // Check the card identity
        val identity = card.parseTransitIdentity()

        assertNotNull(
            identity,
            "Transit identity should not be null"
        )

        assertEquals(
            expected = "Trans Semarang E-Card",
            actual = identity.name
        )

        // The history list must show the same number as the card detail
        assertEquals(
            expected = expectedSerial,
            actual = identity.serialNumber
        )

        // Check the main parser
        val data = card.parseTransitData()

        assertTrue(
            actual = data is TransSemarangECardTransitData,
            message = "Card must be parsed as TransSemarangECardTransitData"
        )

        // Check the balance
        assertEquals(
            expected = TransitCurrency.IDR(expectedBalance),
            actual = data.balances?.first()?.balance
        )

        // Check the card number
        assertEquals(
            expected = expectedSerial,
            actual = data.serialNumber
        )

        // Check the cardholder info
        assertNotNull(
            data.info
        )
    }

    @Test
    fun testStudentCard() {
        checkCard(
            path = "trans_semarang_e_card/trans_semarang_student.json",
            expectedBalance = 13000,
            expectedSerial = "773320402",
        )
    }

    @Test
    fun testGeneralCard1() {
        checkCard(
            path = "trans_semarang_e_card/trans_semarang_general_card1.json",
            expectedBalance = 3500,
            expectedSerial = "203654833",
        )
    }

    @Test
    fun testGeneralCard2() {
        checkCard(
            path = "trans_semarang_e_card/trans_semarang_general_card2.json",
            expectedBalance = 10000,
            expectedSerial = "3514619691",
        )
    }
}
