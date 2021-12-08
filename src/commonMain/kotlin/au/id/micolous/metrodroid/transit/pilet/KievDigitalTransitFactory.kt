package au.id.micolous.metrodroid.transit.pilet

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray

object KievDigitalTransitFactory: PiletTransitFactory() {
    private val CARD_INFO = CardInfo(
        name = R.string.card_name_kiev_digital,
        cardType = CardType.MifarePlus,
        imageId = R.drawable.kiev_digital,
        imageAlphaId = R.drawable.iso7810_id1_alpha,
        locationId = R.string.location_kiev,
        region = TransitRegion.UKRAINE,
        resourceExtraNote = R.string.card_note_card_number_only
    )

    override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
        val sector0 = sectors[0]
        if (sector0[1].data.byteArrayToInt(2, 4) != 0x563d563c)
            return false
        val sector1 = sectors[2]
        if (!sector1[0].data.sliceOffLen(7, 9)
                .contentEquals(ImmutableByteArray.fromASCII("pilet.ee:")))
            return false
        if (!sector1[1].data.sliceOffLen(0, 8)
                .contentEquals(ImmutableByteArray.fromASCII("ekaart:5")))
            return false
        return true
    }

    override val earlySectors get() = 3

    override val allCards get() = listOf(CARD_INFO)

    override val cardName get() = Localizer.localizeString(R.string.card_name_kiev_digital)

    override val ndefType: String
        get() = "pilet.ee:ekaart:5"

    override val ndefAid: Int
        get() = 0x563c

    override val reason: SerialOnlyTransitData.Reason
        get() = SerialOnlyTransitData.Reason.LOCKED

    override val serialPrefixLen: Int
        get() = 7
}