package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector
import au.id.micolous.metrodroid.serializers.classic.MctCardImporter
import au.id.micolous.metrodroid.transit.unknown.BlankTransitData
import kotlin.test.Test
import kotlin.test.assertIs

class MctTest: CardReaderWithAssetDumpsTest<MctCardImporter>(MctCardImporter()) {
    @Test
    fun testExampleDumpFile() {
        val c = loadCard<ClassicCard>("mct/example-dump-file.txt")
        parseCard<BlankTransitData>(c)
        val c2K = loadCard<ClassicCard>("mct/example-dump-file-2K.txt")
        assertIs<UnauthorizedClassicSector>(c2K.mifareClassic?.getSector(31))
    }
}
