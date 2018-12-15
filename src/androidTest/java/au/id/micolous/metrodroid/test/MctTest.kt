package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.MctCardImporter
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData
import org.junit.Test

class MctTest: CardReaderWithAssetDumpsTest<BlankClassicTransitData, ClassicCard>(
        BlankClassicTransitData::class.java, MctCardImporter()) {

    @Test
    fun testExampleDumpFile() {
        loadCard("mct/example-dump-file.txt")
    }
}

