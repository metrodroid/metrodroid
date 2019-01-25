package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import kotlinx.io.OutputStream

interface CardExporter {
    fun writeCard(s: OutputStream, card: Card)
}
