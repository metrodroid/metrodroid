/*
 * EZLinkCompatTest.java
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

import au.id.micolous.metrodroid.card.cepascompat.CEPASCard
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.serializers.XmlCardFormat
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.ezlinkcompat.EZLinkCompatTransitData
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contains tests for the old CEPAS XML format, before it was handled by an ISO7816 reader.
 */
class EZLinkCompatTest : CardReaderWithAssetDumpsTest(XmlCardFormat()) {
    @Test
    fun testCardInfo() {
        val c = loadCard<CEPASCard>("cepas/legacy.xml")
        Log.d("EZLinkCompatTest", "reserial = " + JsonKotlinFormat().writeCard(c))
        val p = parseCard<EZLinkCompatTransitData>(c)
        assertEquals(TransitCurrency.SGD(897), p.balance)
    }
}
