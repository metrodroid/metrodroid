/*
 * AesTest.kt
 *
 * Copyright 2021 Google
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

import au.id.micolous.metrodroid.util.Aes
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class AesTest : BaseInstrumentedTest() {
    data class TestVector(
        val key: ImmutableByteArray,
        val iv: ImmutableByteArray,
        val plainText: ImmutableByteArray,
        val cipherText: ImmutableByteArray
    )
    @Test
    fun testAes() {
        // Test vectors from rfc3602
        val vectors = listOf(
            TestVector(
                ImmutableByteArray.fromHex("06a9214036b8a15b512e03d534120006"),
                ImmutableByteArray.fromHex("3dafba429d9eb430b422da802c9fac41"),
                ImmutableByteArray.fromASCII( "Single block msg"),
                ImmutableByteArray.fromHex("e353779c1079aeb82708942dbe77181a")
            ),
            TestVector(
                ImmutableByteArray.fromHex("c286696d887c9aa0611bbb3e2025a45a"),
                ImmutableByteArray.fromHex("562e17996d093d28ddb3ba695a2e6f58"),
                ImmutableByteArray.fromHex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"),
                ImmutableByteArray.fromHex("d296cd94c2cccf8a3a863028b5e1dc0a7586602d253cfff91b8266bea6d61ab1")
            ),
            TestVector(
                ImmutableByteArray.fromHex("6c3ea0477630ce21a2ce334aa746c2cd"),
                ImmutableByteArray.fromHex("c782dc4c098c66cbd9cd27d825682c81"),
                ImmutableByteArray.fromASCII("This is a 48-byte message (exactly 3 AES blocks)"),
                ImmutableByteArray.fromHex("d0a02b3836451753d493665d33f0e8862dea54cdb293abc7506939276772f8d5021c19216bad525c8579695d83ba2684")
            ),
            TestVector(
                ImmutableByteArray.fromHex("56e47a38c5598974bc46903dba290349"),
                ImmutableByteArray.fromHex("8ce82eefbea0da3c44699ed7db51b7d9"),
                ImmutableByteArray.fromHex("a0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedf"),
                ImmutableByteArray.fromHex("c30e32ffedc0774e6aff6af0869f71aa0f3af07a9a31a9c684db207eb0ef8e4e35907aa632c3ffdf868bb7b29d3d46ad83ce9f9a102ee99d49a53e87f4c3da55")
            )
        )
        for (vector in vectors) {
            assertEquals(
                actual = Aes.encryptCbc(
                    decrypted = vector.plainText,
                    key = vector.key,
                    iv = vector.iv
                ), expected = vector.cipherText
            )
            assertEquals(
                actual = Aes.decryptCbc(
                    encrypted = vector.cipherText,
                    key = vector.key,
                    iv = vector.iv
                ), expected = vector.plainText
            )
        }
    }
}