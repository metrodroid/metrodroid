package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.util.Cmac
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CmacTest : BaseInstrumentedTest() {
    @Test
    fun cmacTest() {
        // Based on CAVS 11.0
        val cavs11 = Json.decodeFromString(ListSerializer(TestCase.serializer()),
            loadSmallAssetBytes("cmac/aescavs11.json").decodeToString())
        assertEquals(337, cavs11.size)
        for (testcase in cavs11) {
            assertEquals(
                expected = testcase.output,
                actual = Cmac.aesCmac(
                    macdata = testcase.input,
                    key = testcase.key,
                    tlen = if (testcase.nullTlen) null else testcase.output.size
                ),
                message="Wrong AES-CMAC for msg=${testcase.input}, key=${testcase.key}"
            )

            if (testcase.nullTlen)
                assertEquals(
                    expected = testcase.output,
                    actual = Cmac.aesCmac(
                        macdata = testcase.input,
                        key = testcase.key
                    ),
                    message = "Wrong AES-CMAC for msg=${testcase.input}, key=${testcase.key}"
                )
        }
    }

    @Serializable
    class TestCase(
        val input: ImmutableByteArray,
        val key: ImmutableByteArray,
        val output: ImmutableByteArray,
        val nullTlen: Boolean = false
    )
}