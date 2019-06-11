/*
 * Localizer.kt
 *
 * Copyright 2019 Google
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

import org.w3c.dom.Node
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory

object LocalizeGenerator {
    val strings = mutableMapOf<String, String>()

    val plurals = mutableMapOf<String, Pair<String, String>>()

    val drawables = listOf("opus_card", "ravkav_card", "mobib_card", "iso7810_id1_alpha",
            "ezlink_card", "nets_card", "tmoney_card", "szt_card", "octopus_card", "octopus_card_alpha",
            "icoca_card", "suica_card", "pasmo_card", "istanbulkart_card", "myki_card", "strelka_card",
            "trimethop_card", "edy_card", "kmt_card", "ovchip_card", "orca_card", "opal_card", "clipper_card",
            "hsl_card", "leap_card", "tpe_easy_card", "charlie_card", "bilheteunicosp_card",
            "bilheteunicosp_card_alpha", "yvr_compass_card", "troika_card", "seqgo_card_alpha", "podorozhnik_card",
            "smartrider_card", "myway_card", "seqgo_card", "msp_goto_card", "laxtap_card",
            "manly_fast_ferry_card", "chc_metrocard", "athopcard", "cadizcard", "busitcard",
            "cartamobile", "gautrain", "komuterlink", "metroq", "metromoney", "navigo",
            "nol", "rejsekort", "slaccess", "ricaricami", "otagogocard", "pastel", "rotorua",
            "suncard", "tampere", "tartu", "krasnodar_etk", "samara_etk", "samara_school", "yaroslavl_etk",
            "beijing", "envibus", "envibus_alpha", "lisboaviva", "adelaide", "oura",
            "samara_student", "tunion", "touchngo", "veneziaunica", "zolotayakorona")
    
    const val pkg = "au.id.micolous.metrodroid.multi"
    const val androidR = "au.id.micolous.farebot.R"
    
    private fun makeRFile(outputDir: File, flavour: String) : OutputStreamWriter {
        val pkgDir = pkg.replace('.', '/')
        val dir = File(outputDir, "$flavour/kotlin/$pkgDir")
        dir.mkdirs()
        val r = File(dir, "R.kt")
        r.createNewFile()
        val os = r.outputStream()
        return os.writer(charset = Charset.forName("UTF-8"))
    }
    
    private fun writeRfile(outputDir: File, flavour: String,
                           keyword: String,
                           transform: (name: String, type: String) -> String) {
        val writer = makeRFile(outputDir, flavour)
        writer.write("package $pkg\n")
        writer.write("\n")
        writer.write("$keyword object Rstring {\n")
        for (string in strings.keys) {
            writer.write("    " + transform(string, "string") + "\n")
        }
        writer.write("}\n")
        writer.write("\n")
        writer.write("$keyword object Rplurals {\n")
        for (string in plurals.keys) {
            writer.write("    " + transform(string, "plurals") + "\n")
        }
        writer.write("}\n")
        writer.write("\n")
        writer.write("$keyword object Rdrawable {\n")
        for (string in drawables) {
            writer.write("    " + transform(string, "drawable") + "\n")
        }
        writer.write("}\n")
        if (keyword == "expect") {
            writer.write("\n")
            writer.write("\n" +
                    "object R {\n" +
                    "    val string = Rstring\n" +
                    "    val plurals = Rplurals\n" +
                    "    val drawable = Rdrawable\n" +
                    "}")
        }

        writer.close()
    }

    fun readStringsXml(stringsFile: File) {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(stringsFile)
        val root = doc.documentElement
        val elements = root.childNodes
        System.out.println("Root element : ${root.nodeName}")
        for (nodeNum in 0 until elements.length) {
            val node = elements.item(nodeNum)
            val resName = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            when (node.nodeName) {
                "string" -> {
                    strings[resName] = node.textContent
                }
                "plurals" -> {
                    plurals[resName] = getPlurals(node)
                }
            }
        }
    }

    private fun getPlurals(node: Node): Pair<String, String> {
        val elements = node.childNodes
        var one: String? = null
        var other: String? = null
        for (nodeNum in 0 until elements.length) {
            val itemNode = elements.item(nodeNum)
            if (itemNode.nodeName != "item")
                continue
            when (itemNode.attributes.getNamedItem("quantity").nodeValue) {
                "one" -> one = itemNode.textContent
                "other" -> other = itemNode.textContent
            }
        }
        return Pair (one!!, other!!)
    }

    private fun escape(input: String): String = input.fold("") { acc, c ->
        when(c) {
            '\n' -> "$acc\\n"
            '\\', '"', '\'', '$' -> "$acc\\$c"
            else -> "$acc$c"
        }
    }

    fun generateLocalize(outputDir: File, stringsFile: File) {
        readStringsXml(stringsFile)
        writeRfile(outputDir, "commonMain", "expect") {
            name, type -> "val $name: ${typeName(type)}" }
        writeRfile(outputDir, "androidMain", "actual") { name, type -> "actual val $name = $androidR.$type.$name" }
        writeFallbackRFile(outputDir, "chromeosMain")
        writeFallbackRFile(outputDir, "jvmCliMain")
    }

    private fun writeFallbackRFile(outputDir: File, flavour: String) {
        writeRfile(outputDir, flavour, "actual") { name, type ->
            when (type) {
                "string" -> "actual val $name = ${typeName(type)}(\"$name\", \"${escape(strings[name]!!)}\")"
                "plurals" -> "actual val $name = ${typeName(type)}(\"$name\", \"${escape(plurals[name]!!.first)}\", \"${escape(plurals[name]!!.second)}\")"
                else -> "actual val $name = ${typeName(type)}(\"$name\")"
            }
        }
    }

    private fun typeName(type: String): String = type[0].toUpperCase() + type.substring(1) + "Resource"
}
