package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.serializers.classic.MctCardImporter
import au.id.micolous.metrodroid.transit.unknown.BlankTransitData
import kotlin.test.Test

class MctTest: CardReaderWithAssetDumpsTest<MctCardImporter>(MctCardImporter()) {
    @Test
    fun testExampleDumpFile() {
        val c = loadCard<ClassicCard>("mct/example-dump-file.txt")
        parseCard<BlankTransitData>(c)
    }
}
