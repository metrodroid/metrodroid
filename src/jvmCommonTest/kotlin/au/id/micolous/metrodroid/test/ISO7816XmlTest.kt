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

import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.serializers.XmlCardFormat
import au.id.micolous.metrodroid.transit.mobib.MobibTransitData
import kotlin.test.Test
import kotlin.test.assertEquals

class ISO7816XmlTest : CardReaderWithAssetDumpsTest(XmlCardFormat()) {
    @Test
    fun testIso7816Card() = runAsync {
        // Load up a Mobib card that is basically empty
        val card = loadCard<ISO7816Card>("iso7816/mobib_blank.xml")
        val cardIso7816 = card.iso7816!!

        // Environment check
        assertEquals(MobibTransitData.NAME, card.parseTransitIdentity()?.name)

        // Load the card into the emulator
        val vcard = VirtualISO7816Card(card)

        // Try to dump the tag from the emulator
        val rcard = ISO7816Card.dumpTag(vcard, MockFeedbackInterface.get())

        // Check that we got an expected number of applications
        assertEquals(cardIso7816.applications.size, rcard.applications.size)
        assertEquals(cardIso7816, rcard)

        val identity = rcard.parseTransitIdentity()
        assertEquals(MobibTransitData.NAME, identity?.name)
    }
}
