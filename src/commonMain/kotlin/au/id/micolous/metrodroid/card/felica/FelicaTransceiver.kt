package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.util.ImmutableByteArray

interface FelicaTransceiver : CardTransceiver {
    /**
     * Gets the default system code associated with the card.
     */
    val defaultSystemCode : Int?

    /**
     * Gets the Manufacture Parameters (PMm) of the card that is currently selected.
     *
     * Returns null otherwise.
     */
    val pmm : ImmutableByteArray? get() = null
}