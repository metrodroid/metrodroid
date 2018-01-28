/*
 * CepasTest.java
 *
 * Copyright 2017 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.farebot.card.cepas.CEPASPurse;
import au.id.micolous.farebot.util.Utils;

import junit.framework.TestCase;

/**
 * Tests for CEPAS cards.
 */

public class CepasTest extends TestCase {

    /**
     * This tests for a crash bug which was reported through Google Play. It looks like it impacts
     * some French card (of unknown origin) which uses CEPAS.
     */
    public void testErrorMode() {
        CEPASPurse errorPurse = new CEPASPurse(0, "Sample Error");
        assertEquals(Utils.getHexString(errorPurse.getCAN(), "<Error>"), "<Error>");
        assertEquals(Utils.getHexString(errorPurse.getCSN(), "<Error>"), "<Error>");

    }
}
