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

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.XmlCardFormat
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.mobib.MobibTransitData
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

        assertEquals(card, rcard)

        val identity = rcard.parseTransitIdentity()
        assertEquals(MobibTransitData.NAME, identity?.name)
    }
}