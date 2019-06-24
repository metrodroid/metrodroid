package au.id.micolous.metrodroid

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.transit.CardInfoRegistry
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
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
        "parse" -> parseCards(args[1])
        "unrecognized" -> unrecognizedCards(args[1])
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
        } catch (e: Exception) {
            System.out.println("   exception = $e")
            null
        }
        System.out.println("   name = ${ti?.name}")
        System.out.println("   serial = ${ti?.serialNumber}")
    }
}

fun unrecognizedCards(fname: String) {
    val allUIDs = mutableSetOf<Pair<ImmutableByteArray, CardType>>()
    val goodUIDs = mutableSetOf<Pair<ImmutableByteArray, CardType>>()
    for (card in loadCards(fname) ?: return) {
        val pid = Pair(card.tagId, card.cardType)
        allUIDs += pid
        val ti = try {
            card.parseTransitIdentity()
        } catch (e: Exception) {
            null
        }
        if (ti != null && ti.name !in listOf(Localizer.localizeString(R.string.locked_mfc_card),
                        Localizer.localizeString(R.string.locked_mfd_card)))
            goodUIDs += pid
    }
    for (uid in allUIDs - goodUIDs)
        System.out.println(uid)
}

private fun printBalance(balance: TransitBalance, idx: Int?) {
    val str = StringBuilder("   balance")
    if (idx != null)
        str.append(" $idx")
    str.append(" = ")
    str.append(balance.balance.formatCurrencyString(true).unformatted)
    balance.validFrom?.let { str.append(" from ${it.format().unformatted}")}
    balance.validTo?.let { str.append(" to ${it.format().unformatted}")}
    balance.name?.let { str.append(", \"$it\"") }
    System.out.println(str)
}

fun parseCards(fname: String) {
    for (card in loadCards(fname) ?: return) {
        System.out.println("card UID = ${card.tagId}")
        val td = try {
            card.parseTransitData()
        } catch (e: Exception) {
            System.out.println("   exception = $e")
            null
        }
        System.out.println("   name = ${td?.cardName}")
        System.out.println("   serial = ${td?.serialNumber}")
        val balances = td?.balances
        when (balances?.size) {
            0, null -> {}
            1 -> printBalance(balances[0], null)
            else -> balances.forEachIndexed { idx, balance -> printBalance(balance, idx) }
        }
        for ((idx, sub) in td?.subscriptions.orEmpty().withIndex()) {
            System.out.println("   subscription $idx: ${sub.subscriptionName}")
            sub.validFrom?.let { System.out.println("      from ${it.format().unformatted}") }
            sub.validTo?.let { System.out.println("      to ${it.format().unformatted}") }
            val infos = sub.info.orEmpty()
            if (!infos.isEmpty()) {
                System.out.println("      info")
            }
        
            for (info in infos) {
                System.out.println("         ${info.text1?.unformatted}: ${info.text2?.unformatted}")
            }
        }
        for ((idx, trip) in td?.trips.orEmpty().withIndex()) {
            System.out.println("   trip $idx")
            trip.startTimestamp?.let { System.out.println("      departure ${it.format().unformatted}") }
            trip.endTimestamp?.let { System.out.println("      arrival ${it.format().unformatted}") }
            System.out.println("      mode ${trip.mode}")
            trip.fare?.let { System.out.println("      fare ${it.formatCurrencyString(false).unformatted}") }
        }
        val infos = td?.info.orEmpty()
        if (!infos.isEmpty()) {
            System.out.println("   info")
        }
        
        for (info in infos) {
            System.out.println("      ${info.text1?.unformatted}: ${info.text2?.unformatted}")
        }

        val raw = td?.getRawFields(TransitData.RawLevel.ALL).orEmpty()
        if (!raw.isEmpty()) {
            System.out.println("   raw")
        }

        for (info in raw) {
            System.out.println("      ${info.text1?.unformatted}: ${info.text2?.unformatted}")
        }

    }
}
