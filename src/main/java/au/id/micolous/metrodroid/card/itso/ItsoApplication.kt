/*
 * ItsoApplication.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.itso

import android.nfc.TagLostException
import android.util.Log
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.itso.Itso7816TransitData
import au.id.micolous.metrodroid.util.Utils
import java.io.IOException

/**
 * Implements ITSO application on "Generic Microprocessor" (ie: ISO7816)
 *
 * Reference: ITSO TS Part 10 Section 3
 * https://www.itso.org.uk/services/specification-resources/the-itso-specification/itso-technical-specification/
 *
 * FIXME: This is untested.
 */
class ItsoApplication : ISO7816Application {
    private constructor(appData: ISO7816Application.ISO7816Info) : super(appData)

    @Suppress("unused")
    private constructor() : super() /* For XML Serializer */

    override fun nameFile(selector: ISO7816Selector): String? {
        val selStr = selector.formatString()
        return if (NAME_MAP.containsKey(selStr)) NAME_MAP.get(selStr) else null
    }

    override fun parseTransitData(): TransitData? {
        return Itso7816TransitData.FACTORY.parseTransitData(this)
    }

    override fun parseTransitIdentity(): TransitIdentity? {
        return Itso7816TransitData.FACTORY.parseTransitIdentity(this)
    }

    companion object {
        // A000000216 = ITSO Ltd.
        // 4954534F2D31 = "ITSO-1"
        val APP_NAME = Utils.hexStringToByteArray("A0000002164954534F2D31")
        const val TYPE = "itso"
        const val TAG = "ItsoApplication"

        var NAME_MAP : Map<String, String>

        init {
            val h = HashMap<String, String>()
            File.all.forEach { h[it.selector.formatString()] = it.name }
            NAME_MAP = h
        }

        @Throws(IOException::class)
        fun dumpTag(protocol: ISO7816Protocol, appData: ISO7816Application.ISO7816Info,
                    feedbackInterface: TagReaderFeedbackInterface): ItsoApplication {
            // At this point, the connection is already open, we just need to dump the right things...

            feedbackInterface.updateStatusText(Utils.localizeString(R.string.itso_reading))
            val size = File.all.size
            feedbackInterface.updateProgressBar(0, size)
            var counter = 0
            var partialRead = false

            for (f in File.all) {
                feedbackInterface.updateProgressBar(counter++, size)
                try {
                    // Section 3.11:
                    // "The storage EFs shall be accessed by use of READ BINARY [...] commands, with
                    // implicit selection using the short EF identifier."
                    appData.dumpBinaryWithImplicitEF(protocol, f.selector, 1)
                } catch (e: TagLostException) {
                    Log.w(TAG, "tag lost", e)
                    partialRead = true
                    break
                } catch (e: IOException) {
                    Log.e(TAG, "couldn't select file", e)
                }

            }

            return ItsoApplication(appData)
        }
    }

    enum class File {
        PARAMETERS(0x000F),
        SHELL(0x0100),

        // TODO: Handle correct number of IPE
        IPE1(0x0101),
        IPE2(0x0102),
        IPE3(0x0103),
        IPE4(0x0104),
        IPE5(0x0105),
        IPE6(0x0106),
        IPE7(0x0107),
        IPE8(0x0108),
        IPE9(0x0109),
        IPE10(0x010A),
        IPE11(0x010B),
        IPE12(0x010C),
        IPE13(0x010D),
        IPE14(0x010E),
        IPE15(0x010F),
        IPE16(0x0110),
        IPE17(0x0111),
        IPE18(0x0112),
        IPE19(0x0113),
        IPE20(0x0114),
        IPE21(0x0115),
        IPE22(0x0116),
        IPE23(0x0117),
        IPE24(0x0118),
        IPE25(0x0119),
        IPE26(0x011A),
        IPE27(0x011B),
        IPE28(0x011C),
        IPE29(0x011D),
        DIR_A(0x011E),
        DIR_B(0x011F);

        var selector: ISO7816Selector
            private set

        constructor(file: Int) {
            selector = ISO7816Selector.makeSelector(file)
        }

        constructor(folder: Int, file: Int) {
            selector = ISO7816Selector.makeSelector(folder, file)
        }

        companion object {
            val all: Array<File>
                get() = File::class.java.enumConstants
        }
    }
}