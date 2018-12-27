/*
 * ISO7816Test.kt
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
package au.id.micolous.metrodroid.test

import android.util.Log
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.XmlCardFormat
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.mobib.MobibTransitData
import au.id.micolous.metrodroid.util.Utils
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ISO7816Test : CardReaderWithAssetDumpsTest<TransitData, Card>
    (TransitData::class.java, XmlCardFormat()) {

    companion object {
        val TAG = ISO7816Test::class.java.simpleName
    }

    @Test
    fun testIso7816Card() {
        // Load up a Mobib card that is basically empty
        val card = loadCard("iso7816/mobib_blank.xml")

        // Environment check
        assertEquals(MobibTransitData.NAME, card.parseTransitIdentity()?.name)

        // Load the card into the emulator
        val vcard = VirtualISO7816Card(card as ISO7816Card)

        // Try to dump the tag from the emulator
        val feedback = MockFeedbackInterface()
        val rcard = ISO7816Card.dumpTag(vcard, card.tagId, feedback)

        // Check that we got an expected number
        assertEquals(card.applications.size, rcard.applications.size)

        // FIXME: check in the depths
        // FIXME: Change to just using standard .equals when we understand what's broken
        for (i in 0 .. card.applications.size) {
            val app = card.applications[i]
            Log.d(TAG, "Checking application ${Utils.getHexString(app.appName!!)}")

            val rapp = rcard.applications[i]

            assertEquals(Utils.getHexString(app.appData!!), Utils.getHexString(rapp.appData!!))
            assertEquals(Utils.getHexString(app.appName!!), Utils.getHexString(rapp.appName!!))
            assertEquals(Utils.getHexString(app.tagId), Utils.getHexString(rapp.tagId))
            for (j in 0 .. app.mFiles.size) {
                val file = app.mFiles[j]
                Log.d(TAG, "Checking file ${file.selector.formatString()}")
                val rfile = rapp.mFiles[j]

                assertEquals(file.selector.formatString(), rfile.selector.formatString())
                assertEquals(Utils.getHexString(file.binaryData),
                        Utils.getHexString(rfile.binaryData))
                assertEquals(Utils.getHexString(file.fci),
                        Utils.getHexString(rfile.fci))
                assertEquals(file.records, rfile.records)
            }
            assertEquals(app.mFiles, rapp.mFiles)
            assertEquals(app.mSfiFiles, rapp.mSfiFiles)
            assertEquals(app, rapp)
        }

        assertEquals(card, rcard)

        val identity = rcard.parseTransitIdentity()
        assertEquals(MobibTransitData.NAME, identity?.name)
    }
}