/*
 * Cli.kt
 *
 * Copyright 2019 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.cli.SmartCard
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.format
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.transit.CardInfoRegistry
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.File

class Cli: CliktCommand() {
    init {
        subcommands(
            Identify(),
            Parse(),
            Unrecognized(),
            Supported(),
            MakeJson(),
            SmartCard(),
            Notices(),
        )
    }

    override fun run() = Unit
}

class Identify: CliktCommand(
        help="Identifies the card(s) in a Metrodroid JSON file") {
    private val fname by argument()

    override fun run() {
        for (card in loadCards(fname) ?: return) {
            println("card UID = ${card.tagId}")
            val ti = try {
                card.parseTransitIdentity()
            } catch (e: Exception) {
                println("   exception = $e")
                null
            }
            println("   name = ${ti?.name}")
            println("   serial = ${ti?.serialNumber}")
        }
    }
}

class MakeJson: CliktCommand(
        help="Converts Metrodroid XML/JSON into latest version of JSON") {
    private val fname by argument()
    private val output by argument()

    override fun run() {
        for (card in loadCards(fname) ?: return) {
            val json = CardSerializer.toJson(card)
            val by = File(output).outputStream()
            by.write(json.toString().encodeToByteArray())
            by.close()
        }
    }
}

class Parse: CliktCommand(
        help="Parses the card(s) in a Metrodroid JSON file") {
    private val fname by argument()

    override fun run() {
        for (card in loadCards(fname) ?: return) {
            printCard(card)
        }
    }
}

fun printCard(card: Card) {
    println("card UID = ${card.tagId}")
    val td = try {
        card.parseTransitData()
    } catch (e: Exception) {
        println("   exception = $e")
        null
    }
    println("   name = ${td?.cardName}")
    println("   serial = ${td?.serialNumber}")
    val balances = td?.balances
    when (balances?.size) {
        0, null -> {}
        1 -> printBalance(balances[0], null)
        else -> balances.forEachIndexed { idx, balance -> printBalance(balance, idx) }
    }
    for ((idx, sub) in td?.subscriptions.orEmpty().withIndex()) {
        println("   subscription $idx: ${sub.subscriptionName}")
        sub.validFrom?.let { println("      from ${it.format().unformatted}") }
        sub.validTo?.let { println("      to ${it.format().unformatted}") }
        val infos = sub.info.orEmpty()
        if (infos.isNotEmpty()) {
            println("      info")
        }

        for (info in infos) {
            println("         ${info.text1?.unformatted}: ${info.text2?.unformatted}")
        }

        val raw = sub.getRawFields(TransitData.RawLevel.ALL).orEmpty()
        if (raw.isNotEmpty()) {
            println("      raw")
        }

        for (info in raw) {
            println("         ${info.text1?.unformatted}: ${info.text2?.unformatted}")
        }
    }
    for ((idx, trip) in td?.trips.orEmpty().withIndex()) {
        println("   trip $idx")
        trip.startTimestamp?.let { println("      departure ${it.format().unformatted}") }
        trip.endTimestamp?.let { println("      arrival ${it.format().unformatted}") }
        println("      mode ${trip.mode}")
        trip.getAgencyName(false)?.let { println("      agency ${it.format().unformatted}") }
        trip.routeName?.let { println("      route $it") }
        trip.startStation?.let { println("      from ${it.stationName}") }
        trip.endStation?.let { println("      to ${it.stationName}") }
        trip.fare?.let { println("      fare ${it.formatCurrencyString(false).unformatted}") }
        trip.vehicleID?.let { println("      vehicle $it") }
        trip.getRawFields(TransitData.RawLevel.ALL)?.let { println("      raw $it") }
    }
    val infos = td?.info.orEmpty()
    if (infos.isNotEmpty()) {
        println("   info")
    }

    for (info in infos) {
        println("      ${info.text1?.unformatted}: ${info.text2?.unformatted}")
    }

    val raw = td?.getRawFields(TransitData.RawLevel.ALL).orEmpty()
    if (raw.isNotEmpty()) {
        println("   raw")
    }

    for (info in raw) {
        println("      ${info.text1?.unformatted}: ${info.text2?.unformatted}")
    }
}

class Unrecognized: CliktCommand(
        help="Lists unrecognized card(s) in a Metrodroid JSON file") {
    private val fname by argument()

    override fun run() {
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
            println(uid)
    }
}

class Supported: CliktCommand(
        help="Lists all cards supported by Metrodroid") {
    override fun run() {
        for (card in CardInfoRegistry.allCardsAlphabetical) {
            println("card name = ${card.name}")
            println("     type = ${card.cardType}")
            if (card.locationId != null)
                println("     location = ${Localizer.localizeString(card.locationId)}")
            println("     keysRequired = ${card.keysRequired}")
            println("     preview = ${card.preview}")
            if (card.resourceExtraNote != null) {
                println("     note = ${Localizer.localizeString(card.resourceExtraNote)}")
            }
        }
    }
}

class Notices: TranslatedCommand(
    help="List license notices") {

    fun readLicenseTextFromAsset(path: String) {
        val s = Notices::class.java.getResourceAsStream("/$path")?.readBytes()
            ?.decodeToString() ?: return
        println(s)
        println("")
    }

    override fun run() {
        readLicenseTextFromAsset("Metrodroid-NOTICE.txt")
        readLicenseTextFromAsset("third_party/NOTICE.AOSP.txt")
        readLicenseTextFromAsset("third_party/NOTICE.protobuf.txt")

        for (notice in StationTableReader.allNotices) {
            println(notice)
            println("")
        }
    }
}

fun main(args: Array<String>) = Cli().main(args)

private fun loadCards(fname: String): Iterator<Card>? {
    val by = File(fname).inputStream()
    val cards = XmlOrJsonCardFormat().readCards(by)
    if (cards == null) {
        println("No cards found")
    }
    return cards
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
    println(str)
}
