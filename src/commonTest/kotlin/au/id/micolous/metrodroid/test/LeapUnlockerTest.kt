/*
* LeapUnlockerTest.kt
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

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.desfire.DesfireAuthLog
import au.id.micolous.metrodroid.card.desfire.DesfireProtocol
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.transit.tfi_leap.LeapUnlocker
import au.id.micolous.metrodroid.transit.tfi_leap.createUnlockerDispatch
import au.id.micolous.metrodroid.util.*
import kotlin.test.*

class MockDesfireCard (override val uid: ImmutableByteArray?,
                       private val expectedLog: List<Pair<ImmutableByteArray, ImmutableByteArray>>): CardTransceiver {
    private val counter = AtomicCounter()
    fun assertFinished() {
        assertEquals(expectedLog.size, counter.get())
    }

    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        val ctr = counter.getAndIncrement()
        assertEquals(expectedLog[ctr].first, data)
        return expectedLog[ctr].second
    }

}

class NetworkHelperMock (
    private val expectedLog: List<Pair<ImmutableByteArray, ImmutableByteArray>>,
    private val uuid: String,
    private val url: String
) : NetworkHelper {
    private val counter = AtomicCounter()
    fun assertFinished() {
        assertEquals(expectedLog.size, counter.get())
    }

    override fun sendPostRequest(urlString: String, request: ByteArray): ByteArray? {
        val ctr = counter.getAndIncrement()
        assertEquals(url, urlString)
        assertEquals(expectedLog[ctr].first, request.toImmutable())
        return expectedLog[ctr].second.dataCopy
    }

    override fun randomUUID(): String = uuid
}

class LeapUnlockerTest: BaseInstrumentedTest() {
    private fun decBin(input: Long) = ImmutableByteArray.fromASCII(input.toString())
    private fun headTag(type: Byte, size: Int) = ImmutableByteArray.of(type, size.toByte())
    private fun tag(type: Byte, contents: ImmutableByteArray) = headTag(type, contents.size) + contents
    private fun fileQuery(fileNo: Byte) = ImmutableByteArray.of(DesfireProtocol.READ_DATA, fileNo, 0, 0, 0, 0x20, 0, 0)
    @Test
    fun testUnlock() {
        // Test data. Completely fake
        val tagId = ImmutableByteArray.fromHex("04123456789a80")
        val file1Contents = ImmutableByteArray.fromHex("3984bae6a2282a33f830e1b9a08a1cbfa09bc835b80b5c0b1a53c4e1ce574945")
        val file1fContents = ImmutableByteArray.fromHex("f3fc6ecf04e10cff3b3073541e2b4e189c6060febe58aa391f7ef902735b290d58511d8b")
        val challenge0d = ImmutableByteArray.fromHex("ca84517530a7d857")
        val response0d = ImmutableByteArray.fromHex("ab37ceed5c5c88813078a2ffc1241fcd")
        val confirmation0d = ImmutableByteArray.fromHex("283fe489b0a648df")
        val challenge03 = ImmutableByteArray.fromHex("0f7dab0770f5c1aa")
        val response03 = ImmutableByteArray.fromHex("389906a0220b326bdfa5f62fc5435f06")
        val confirmation03 = ImmutableByteArray.fromHex("85d7a98fe642ab5c")
        val uuid1 = "fd8481eb-c0a8-4ed4-acb3-c4b246845f13"
        val uuid2 = "493ed93e-16b3-4a69-8df8-f64f46458ff4"
        val manufactureDataPrefix = ImmutableByteArray.fromHex("65033df72c4d8cdf869413872c36")
        val manufactureDataSuffix = ImmutableByteArray.fromHex("384ebcaaa2228a")
        val baseTime = 1333333333333
        val updateAuthenticate1Start = baseTime + 430
        val updateAuthenticate1End   = baseTime + 490
        val authenticate2Start       = baseTime
        val authenticate2End         = baseTime + 95
        val updateAuthenticate2Start = baseTime + 705
        val updateAuthenticate2End   = baseTime + 795

        // Consts
        val appId = 11473186
        val appIdEncoded = "a2a2bc05"
        val asyncReadsKeyHex = ImmutableByteArray.fromASCII("ASYNC_READS")
        val updateAuthenticate1KeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_1")
        val updateAuthenticate1StartKeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_1_START")
        val updateAuthenticate1EndKeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_1_END")
        val updateAuthenticate1MsKeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_1_MS")
        val updateAuthenticate2KeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_2")
        val updateAuthenticate2StartKeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_2_START")
        val updateAuthenticate2EndKeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_2_END")
        val updateAuthenticate2MsKeyHex = ImmutableByteArray.fromASCII("UPDATE_AUTHENTICATE_2_MS")
        val authenticate2StartKeyHex = ImmutableByteArray.fromASCII("AUTHENTICATE_2_START")
        val authenticate2EndKeyHex = ImmutableByteArray.fromASCII("AUTHENTICATE_2_END")
        val authenticate2MsKeyHex = ImmutableByteArray.fromASCII("AUTHENTICATE_2_MS")
        val datastructureReadHex = ImmutableByteArray.fromASCII("DATASTRUCTURE_READ")
        val trueHex = ImmutableByteArray.fromASCII("true")

        // Derived data
        val uuid1Hex = ImmutableByteArray.fromASCII(uuid1)
        val uuid2Hex = ImmutableByteArray.fromASCII(uuid2)
        val updateAuthenticate1Ms = updateAuthenticate1End - updateAuthenticate1Start
        val updateAuthenticate2Ms = updateAuthenticate2End - updateAuthenticate2Start
        val authenticate2Ms = authenticate2End - authenticate2Start
        val appIdTag = ImmutableByteArray.fromHex("08$appIdEncoded")

        val networkMock = NetworkHelperMock(
            listOf(
                Pair(appIdTag
                            + tag(0x12, uuid1Hex)
                            + tag(0x22,
                        tag(0x0a, ImmutableByteArray.of(0x60))
                        + tag(0x12, ImmutableByteArray.of(0) + manufactureDataPrefix + tagId + manufactureDataSuffix))
                            + tag(0x22,
                              tag(0xa, fileQuery(1))
                            + tag(0x12, ImmutableByteArray.of(0) + file1Contents))
                        + tag(0x22,
                            tag(0xa, ImmutableByteArray.fromHex("0a0d"))
                            + tag(0x12, ImmutableByteArray.ofB(0xaf) + challenge0d))
                        + tag (0x2a,
                            tag(0x1a, asyncReadsKeyHex)
                            + tag(0x22, trueHex)),
                    appIdTag + tag(0x12, uuid2Hex)
                            + tag(0x1a, updateAuthenticate1KeyHex)
                            + tag(0x22,
                            tag(0xa, ImmutableByteArray.ofB(0xaf) + response0d)
                            + tag(0x1a, ImmutableByteArray.of(0)))
                            + tag(0x22, tag(0xa, fileQuery(0x1f)))
                            + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, authenticate2StartKeyHex) + tag(0x22, decBin(authenticate2Start)))
                                + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, authenticate2EndKeyHex) + tag(0x22, decBin(authenticate2End)))
                                + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, authenticate2MsKeyHex) + tag(0x22, decBin(authenticate2Ms))
                    )),
                Pair(appIdTag
                        + tag(0x12, uuid2Hex)
                        + tag(0x1a, updateAuthenticate1KeyHex)
                        + tag(0x22,
                        tag(0xa, ImmutableByteArray.ofB(0xaf) + response0d)
                            + tag(0x12, ImmutableByteArray.of(0) + confirmation0d)
                            + tag(0x1a, ImmutableByteArray.of(0)))
                            + tag(0x22,
                        tag(0xa, fileQuery(0x1f))
                        + tag(0x12, ImmutableByteArray.of(0) + file1fContents))
                        + tag (0x2a,
                    tag(0x1a, asyncReadsKeyHex)
                            + tag(0x22, trueHex)),
                    appIdTag
                            + tag(0x12, uuid2Hex)
                            + tag(0x1a, updateAuthenticate2KeyHex)
                            + tag(0x22, tag(0xa, ImmutableByteArray.fromHex("0a03")) + tag(0x1a, ImmutableByteArray.ofB(0xaf)))
                            + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, updateAuthenticate1StartKeyHex)
                            + tag(0x22, decBin(updateAuthenticate1Start)))
                                 + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, updateAuthenticate1EndKeyHex) + tag(0x22, decBin(updateAuthenticate1End)))
                                 + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, updateAuthenticate1MsKeyHex) + tag(0x22, decBin(updateAuthenticate1Ms)))
                     ),
                Pair(appIdTag
                        + tag(0x12, uuid2Hex)
                        + tag(0x1a, updateAuthenticate2KeyHex)
                        + tag(0x22,
                        tag(0xa, ImmutableByteArray.fromHex("0a03"))
                        + tag(0x12, ImmutableByteArray.ofB(0xaf) + challenge03)
                        + tag(0x1a, ImmutableByteArray.ofB(0xaf)))
                        + tag(0x2a,
                        tag(0x1a, asyncReadsKeyHex)
                        + tag(0x22, trueHex)),
                    appIdTag
                            + tag(0x12, uuid2Hex)
                            + tag(0x1a, datastructureReadHex)
                            + tag(0x22,
                            tag(0xa, ImmutableByteArray.ofB(0xaf) + response03)
                            + tag(0x1a, ImmutableByteArray.of(0)))
                                + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, updateAuthenticate2StartKeyHex) + tag(0x22, decBin(updateAuthenticate2Start)))
                                + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, updateAuthenticate2EndKeyHex) + tag(0x22, decBin(updateAuthenticate2End)))
                                + tag(0x2a, ImmutableByteArray.fromHex("0800") + tag(0x1a, updateAuthenticate2MsKeyHex) + tag(0x22, decBin(updateAuthenticate2Ms)))
                )
            ),
            uuid1,
            "https://tnfc.leapcard.ie//ReadCard/V0"
        )
        networkMock.nativeFreeze()
        networkHelper = networkMock

        Preferences.retrieveLeapKeys = true

        val unlocker = createUnlockerDispatch(appId,
            manufactureDataPrefix + tagId + manufactureDataSuffix
        )
        val authLog = mutableListOf<DesfireAuthLog>()

        assertNotNull(unlocker)
        assertIs<LeapUnlocker>(unlocker)
        val fakeCard = MockDesfireCard(
            tagId,
            listOf(
                Pair(ImmutableByteArray.fromHex("900a0000010d00"),
                    challenge0d + ImmutableByteArray.fromHex("91af")),
                Pair(ImmutableByteArray.fromHex("90af000010") + response0d + ImmutableByteArray.fromHex("00"),
                    confirmation0d + ImmutableByteArray.fromHex("9100")),
                Pair(ImmutableByteArray.fromHex("900a0000010300"),
                    challenge03 + ImmutableByteArray.fromHex("91af")),
                Pair(ImmutableByteArray.fromHex("90af000010") + response03
                        + ImmutableByteArray.fromHex("00"),
                    confirmation03 + ImmutableByteArray.fromHex("9100"))
            ))
        val protocol = DesfireProtocol(fakeCard)
        val file1 = RawDesfireFile(null, file1Contents)
        val file1f = RawDesfireFile(null, file1fContents)
        assertContentEquals(intArrayOf(1, 0x1f, 2, 3),
            unlocker.getOrder(protocol, intArrayOf(1, 2, 3, 0x1f)))
        unlocker.unlock(protocol, mapOf(1 to file1),
            0x1f, authLog)
        unlocker.unlock(protocol, mapOf(1 to file1, 0x1f to file1f),
            2, authLog)
        unlocker.unlock(protocol, mapOf(1 to file1, 0x1f to file1f),
            3, authLog)
        networkMock.assertFinished()
        fakeCard.assertFinished()
    }
}
