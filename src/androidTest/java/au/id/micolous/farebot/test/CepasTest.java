package au.id.micolous.farebot.test;

import com.codebutler.farebot.card.cepas.CEPASPurse;
import com.codebutler.farebot.util.Utils;

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
