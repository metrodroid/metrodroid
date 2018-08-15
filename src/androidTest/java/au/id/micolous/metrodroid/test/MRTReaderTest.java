/*
 * MTRReaderTest.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.test;

import android.test.AndroidTestCase;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;

/**
 * Tests StationTableReader (MdST). This uses the ezlink stop database.
 */

public class MRTReaderTest extends AndroidTestCase {
    public void testGetStation() {
        TestUtils.setLocale(getContext(), "en-US");
        Station s = EZLinkTransitData.getStation("CGA");
        assertEquals("Changi Airport", s.getStationName());
        assertEquals(1.357372, Float.valueOf(s.getLatitude()), 0.00001);
        assertEquals(103.988836, Float.valueOf(s.getLongitude()), 0.00001);
    }
}
