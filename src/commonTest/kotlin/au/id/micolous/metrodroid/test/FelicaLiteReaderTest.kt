package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.felica.FelicaReader
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.ui.ListItemInterface
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FelicaLiteReaderTest : BaseInstrumentedTest() {
    @Test
    fun testFelicaLiteReader() {
        val card = JsonKotlinFormat.readCard(loadAsset("ndef/felicalitendef.json"))
        val injson = Json.parseToJsonElement(loadSmallAssetBytes("ndef/felicalitendef.json").decodeToString())
        val protocol = VirtualFelicaLite(card)
        val reread = FelicaReader.dumpTag(protocol, MockFeedbackInterface())
        val recard = Card(felica = reread, tagId = protocol.uid, scannedAt = card.scannedAt)
        val outjson = JsonKotlinFormat.makeCardElement(recard)
        assertEquals(injson, outjson)
        val manuf = Json.encodeToJsonElement(ListSerializer(ListItemInterface.serializer()),
            recard.manufacturingInfo!!)
        assertEquals(Json.parseToJsonElement(loadSmallAssetBytes("ndef/felicalitendefmanuf.json").decodeToString()), manuf)
        val raw = Json.encodeToJsonElement(ListSerializer(ListItemInterface.serializer()),
            recard.rawData!!)
        assertEquals(Json.parseToJsonElement(loadSmallAssetBytes("ndef/felicalitendefraw.json").decodeToString()), raw)
    }
}