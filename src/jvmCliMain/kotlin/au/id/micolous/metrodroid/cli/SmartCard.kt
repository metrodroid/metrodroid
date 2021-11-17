/*
 * SmartCard.kt
 *
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
package au.id.micolous.metrodroid.cli

import au.id.micolous.kotlin.pcsc.Context
import au.id.micolous.kotlin.pcsc.ReaderState
import au.id.micolous.kotlin.pcsc.getAllReaderStatus
import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.card.desfire.DesfireCardReader
import au.id.micolous.metrodroid.card.felica.FelicaReader
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.printCard
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.util.JavaStreamOutput
import au.id.micolous.metrodroid.util.makeFilename
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.*


/**
 * Ignore certain CCID readers
 */
val ReaderState.ignored : Boolean
    get() {
        val iname = reader.lowercase(Locale.ENGLISH)

        // Yubikey CCID
        return iname.contains("yubi")
    }

/**
 * Communicates with a card using the PC/SC API.
 */
class SmartCard: CliktCommand(help="Communicates with a card using the PC/SC API") {

    private val reader: String? by option("-r", "--reader", metavar = "DEVICE",
        help="Name of the reader device to use. If not specified, tries to pick the first reader " +
            "which has a card inserted, and is not a security key. eg: -r \"ACS ACR122U\"")
    private val listReaders: Boolean by option("-l", "--list",
        help="Lists all connected reader devices.").flag(default=false)

    private val noParse: Boolean by option("-P", "--no-parse",
        help="Skip parsing the card. Useful if you only want to save a dump of the card " +
            "data.").flag(default=false)
    private val traces: Boolean by option("-t", "--trace",
        help="Emits raw APDU data for debugging.").flag(default=false)
    private val noUID: Boolean by option("-U", "--no-uid",
        help="Skip requesting the card's UID. Required for contact card readers. Breaks " +
            "communication with FeliCa and several contactless cards.").flag(default=false)
    private val felicaOnlyFirst: Boolean by option("--felica-only-first",
        help="Only read the first system code of FeliCa cards, simulating a work-around for an " +
            "iOS bug. This will result in incomplete data being read from the " +
            "card!").flag(default=false)
    private val waitForCard: Boolean by option(
        "--wait-for-card",
        help="If set, wait for a card to be in the field of a connected reader. Requires --reader."
    ).flag(default=false).validate {
        require(!it || reader != null) {
            "--wait-for-card requires --reader is also set."
        }
    }

    private val outFile: File? by option("-o", "--output", metavar = "FILE_OR_DIR",
        help="Specify a path that does not exist to create a new file with this name. " +
            "Specify a directory name that already exists to create a new dump file in " +
            "it, automatically generating a name.").file().validate {
        require(it.exists() == it.isDirectory) {
            "must be a directory that exists, or must be a file that doesn't exist: $it" }
        if (!it.isDirectory) {
            val parent = it.canonicalFile.parentFile
            require(parent.isDirectory && parent.exists()) {
                "parent must be a directory that exists: $it" }
        }
    }

    override fun run() {
        val o = Object()
        val context = Context.establish()
        val allTerminals = context.getAllReaderStatus()
        val outFile : File? = outFile

        if (listReaders) {
            printTerminals(allTerminals)
            return
        }

        var terminal = if (reader == null) {
            if (waitForCard) {
                printTerminals(allTerminals)
                println("Can only wait for a card if a specific reader is selected.")
                return
            }

            val usableTerminals = allTerminals.filter {
                !it.ignored && it.eventState.present }

            if (usableTerminals.count() != 1) {
                printTerminals(allTerminals)
                println("Expected 1 terminal, got ${usableTerminals.count()} instead.")
                return
            }

            usableTerminals.getOrNull(0)
        } else {
            allTerminals.filter { it.reader == reader }.getOrNull(0)
        }

        if (terminal == null) {
            printTerminals(allTerminals)
            println("Couldn't find terminal: $reader")
            return
        }

        println("Terminal: ${terminal.reader}")

        if (!terminal.eventState.present) {
            if (!waitForCard) {
                println("Card not present, insert into / move in range of ${terminal.reader}")
                return
            }

            println("Waiting for card insertion...")
            do {
                terminal = runBlocking{
                    context.getStatusChange(au.id.micolous.kotlin.pcsc.LONG_TIMEOUT,
                        listOf(terminal!!.update())).first()
                }
            } while (!terminal!!.eventState.present)
        }

        val card = runBlocking { dumpTag(context, terminal.reader) }

        if (outFile != null) {
            val fn = if (outFile.isDirectory) {
                Paths.get(outFile.path, makeFilename(card)).toFile()
            } else { outFile }

            if (!fn.createNewFile()) {
                println("File already exists: $fn")
                return
            }

            FileOutputStream(fn).use {
                JsonKotlinFormat.writeCard(JavaStreamOutput(it), card)
                println("Wrote card data to: ${fn.path}")
            }
        }

        if (!noParse) {
            println("Card info:")
            printCard(card)
        }
    }

    /** Prints a list of connected terminals. */
    private fun printTerminals(terminals: List<ReaderState>) {
        println("Found ${terminals.count()} card terminal(s):")
        terminals.forEachIndexed { index, r ->
            println(
                "#$index: ${r.reader} (card ${
                if (r.eventState.present) { "present" } else { "missing" } }) ${
                if (r.ignored) { "(ignored)" } else { "" }}")
        }
    }

    private val feedbackInterface = object : TagReaderFeedbackInterface {
        override fun updateStatusText(msg: String) {
            println(msg)
        }

        override fun updateProgressBar(progress: Int, max: Int) {
            println("Dumping: ($progress / $max)")
        }

        override fun showCardType(cardInfo: CardInfo?) {
            if (cardInfo == null) {
                println("Empty card type")
                return
            }

            println("Card type: ${cardInfo.name}")
        }
    }

    private fun dumpTag(context: Context, terminal: String) : Card {
        JavaCardTransceiver(context, terminal, traces, noUID).use {
            it.connect()

            // TODO
            val tagId = it.uid!!
            val scannedAt = TimestampFull.now()

            when (it.cardType) {
                CardType.ISO7816 -> {
                    val d = DesfireCardReader.dumpTag(it, feedbackInterface)
                    if (d != null) {
                        return Card(tagId = tagId, scannedAt = scannedAt, mifareDesfire = d)
                    }

                    val isoCard = ISO7816Card.dumpTag(it, feedbackInterface)
                    return Card(tagId = tagId, scannedAt = scannedAt, iso7816 = isoCard)
                }

                CardType.FeliCa -> {
                    val t = JavaFeliCaTransceiver.wrap(it)
                    val f = FelicaReader.dumpTag(t, feedbackInterface, onlyFirst = felicaOnlyFirst)
                    return Card(tagId = tagId, scannedAt = scannedAt, felica = f)
                }

                else -> throw Exception("Unhandled card type ${it.cardType}")
            }
        }
    }
}
