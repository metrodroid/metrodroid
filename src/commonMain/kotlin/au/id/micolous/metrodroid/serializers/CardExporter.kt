package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.util.Output

interface CardExporter {
    fun writeCard(s: Output, card: Card)
}
