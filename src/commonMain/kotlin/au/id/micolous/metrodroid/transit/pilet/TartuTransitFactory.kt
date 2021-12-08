package au.id.micolous.metrodroid.transit.pilet

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.ndef.MifareClassicAccessDirectory
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Transit data type for Tartu bus card.
 *
 *
 * This is a very limited implementation of reading TartuBus, because only
 * little data is stored on the card
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/TartuBus
 */
object TartuTransitFactory : PiletTransitFactory() {
    private const val NAME = "Tartu Bus"

    private val CARD_INFO = CardInfo(
        name = NAME,
        cardType = CardType.MifareClassic,
        imageId = R.drawable.tartu,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        locationId = R.string.location_tartu,
        region = TransitRegion.ESTONIA,
        resourceExtraNote = R.string.card_note_card_number_only
    )

    override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
        val sector0 = sectors[0]
        if (sector0[1].data.byteArrayToInt(2, 4) != 0x03e103e1)
            return false
        val sector1 = sectors[1]
        if (!sector1[0].data.sliceOffLen(7, 9)
                        .contentEquals(ImmutableByteArray.fromASCII("pilet.ee:")))
            return false
        if (!sector1[1].data.sliceOffLen(0, 8)
                        .contentEquals(ImmutableByteArray.fromASCII("ekaart:2")))
            return false
        return true
    }

    override val earlySectors get() = 2

    override val allCards get() = listOf(CARD_INFO)

    override val cardName get() = NAME

    override val ndefType: String
        get() = "pilet.ee:ekaart:2"

    override val ndefAid: Int
        get() = MifareClassicAccessDirectory.NFC_AID

    override val reason: SerialOnlyTransitData.Reason
        get() = SerialOnlyTransitData.Reason.NOT_STORED

    override val serialPrefixLen: Int
        get() = 8
}