package au.id.micolous.metrodroid

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.transit.CardInfoRegistry
import au.id.micolous.metrodroid.card.Card
import java.io.File
import java.util.*

fun listCards() {
    for (card in CardInfoRegistry.allCardsAlphabetical) {
        System.out.println("card name = ${card.name}")
        System.out.println("     type = ${card.cardType}")
        if (card.locationId != null)
            System.out.println("     location = ${Localizer.localizeString(card.locationId)}")
        System.out.println("     keysRequired = ${card.keysRequired}")
        System.out.println("     preview = ${card.preview}")
        if (card.resourceExtraNote != null) {
            System.out.println("     note = ${Localizer.localizeString(card.resourceExtraNote)}")
        }
    }
}

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "supported" -> listCards()
        "identify" -> identifyCards(args[1])
        else -> System.out.println("Unknown command ${args.getOrNull(0)}")
    }
}

fun loadCards(fname: String): Iterator<Card>? {
    val by = File(fname).inputStream()
    val cards = XmlOrJsonCardFormat().readCards(by)
    if (cards == null) {
        System.out.println("No cards found")
    }
    return cards
}    

fun identifyCards(fname: String) {
    for (card in loadCards(fname) ?: return) {
        System.out.println("card UID = ${card.tagId}")
        val ti = try {
            card.parseTransitIdentity()
        } catch (e: Exception) { null }
        System.out.println("   name = ${ti?.name}")
        System.out.println("   serial = ${ti?.serialNumber}")
    }
}
