/*
 * LuhnTest.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import org.junit.Test;

import static au.id.micolous.metrodroid.util.Utils.calculateLuhn;
import static au.id.micolous.metrodroid.util.Utils.validateLuhn;
import static junit.framework.TestCase.*;

/**
 * Testing the Luhn checksum calculator
 */
public class LuhnTest {
    @Test
    public void testValidation() {
        assertTrue(validateLuhn("14455833625"));
        assertTrue(validateLuhn("2132023611"));
        assertTrue(validateLuhn("22278878354"));
        assertTrue(validateLuhn("16955109885"));
        assertTrue(validateLuhn("20705769295"));
        assertTrue(validateLuhn("5141418763"));
        assertTrue(validateLuhn("13076501629"));
        assertTrue(validateLuhn("26625862995"));
        assertTrue(validateLuhn("13622972688"));
        assertTrue(validateLuhn("11981944561"));
        assertTrue(validateLuhn("7868205860"));
        assertTrue(validateLuhn("12769832796"));
        assertTrue(validateLuhn("13738153843"));
        assertTrue(validateLuhn("33032358864"));
        assertTrue(validateLuhn("17675980209"));
        assertTrue(validateLuhn("17992698740"));
        assertTrue(validateLuhn("23711490617"));
        assertTrue(validateLuhn("25099325414"));
        assertTrue(validateLuhn("32328053437"));
        assertTrue(validateLuhn("5468460836"));
        assertTrue(validateLuhn("7326462152"));
        assertTrue(validateLuhn("20546726827"));
        assertTrue(validateLuhn("900318908"));
        assertTrue(validateLuhn("28759945042"));
        assertTrue(validateLuhn("26024096005"));
        assertTrue(validateLuhn("32803807406"));
        assertTrue(validateLuhn("41950380174"));
        assertTrue(validateLuhn("7144685935"));
        assertTrue(validateLuhn("200247740"));
        assertTrue(validateLuhn("3580259228"));
        assertTrue(validateLuhn("35103155830"));
        assertTrue(validateLuhn("38832859524"));
        assertTrue(validateLuhn("15520499730"));
        assertTrue(validateLuhn("42895092221"));
        assertTrue(validateLuhn("42445712377"));
        assertTrue(validateLuhn("23589471772"));
        assertTrue(validateLuhn("24185368255"));
        assertTrue(validateLuhn("27584849593"));
        assertTrue(validateLuhn("14286020574"));
        assertTrue(validateLuhn("10209508851"));
        assertTrue(validateLuhn("12103634601"));
        assertTrue(validateLuhn("9882041909"));
        assertTrue(validateLuhn("21735085231"));
        assertTrue(validateLuhn("26734471720"));
        assertTrue(validateLuhn("660001215"));
        assertTrue(validateLuhn("34667618408"));
        assertTrue(validateLuhn("23145570083"));
        assertTrue(validateLuhn("9885843319"));
        assertTrue(validateLuhn("7579437711"));
        assertTrue(validateLuhn("32784123336"));
        assertTrue(validateLuhn("7847703084"));
        assertTrue(validateLuhn("21127514533"));
        assertTrue(validateLuhn("632990271"));
        assertTrue(validateLuhn("33021014510"));
        assertTrue(validateLuhn("11666056244"));
        assertTrue(validateLuhn("35440463616"));
        assertTrue(validateLuhn("15409942420"));
        assertTrue(validateLuhn("39828628881"));
        assertTrue(validateLuhn("16118274394"));
        assertTrue(validateLuhn("12211164111"));
        assertTrue(validateLuhn("9604520834"));
        assertTrue(validateLuhn("22614593253"));
        assertTrue(validateLuhn("25859215862"));
        assertTrue(validateLuhn("23067679268"));
        assertTrue(validateLuhn("28214834377"));
        assertTrue(validateLuhn("28781966271"));
        assertTrue(validateLuhn("3811009145"));
        assertTrue(validateLuhn("25973242313"));
        assertTrue(validateLuhn("14198135569"));
        assertTrue(validateLuhn("26997711937"));
        assertTrue(validateLuhn("24467620969"));
        assertTrue(validateLuhn("6556551593"));
        assertTrue(validateLuhn("1557591078"));
        assertTrue(validateLuhn("27628820907"));
        assertTrue(validateLuhn("5311479991"));
        assertTrue(validateLuhn("12002033574"));
        assertTrue(validateLuhn("32934191498"));
        assertTrue(validateLuhn("20720982733"));
        assertTrue(validateLuhn("38009252107"));
        assertTrue(validateLuhn("33292581635"));
        assertTrue(validateLuhn("7681531666"));
        assertTrue(validateLuhn("26341189681"));
        assertTrue(validateLuhn("22497297667"));
        assertTrue(validateLuhn("26097655984"));
        assertTrue(validateLuhn("15925093864"));
        assertTrue(validateLuhn("3645297643"));
        assertTrue(validateLuhn("37672018977"));
        assertTrue(validateLuhn("27585874590"));
        assertTrue(validateLuhn("5346444127"));
        assertTrue(validateLuhn("26083423199"));
        assertTrue(validateLuhn("19272674524"));
        assertTrue(validateLuhn("7431451645"));
        assertTrue(validateLuhn("9742753537"));
        assertTrue(validateLuhn("10462043414"));
        assertTrue(validateLuhn("8992851777"));
        assertTrue(validateLuhn("5384023908"));
        assertTrue(validateLuhn("7618265594"));
        assertTrue(validateLuhn("34876414250"));
        assertTrue(validateLuhn("29661424837"));
        assertTrue(validateLuhn("4531175455"));
    }

    @Test
    public void testInvalidation() {
        assertFalse(validateLuhn("4139648926"));
        assertFalse(validateLuhn("1694387920"));
        assertFalse(validateLuhn("258151280"));
        assertFalse(validateLuhn("314237730"));
        assertFalse(validateLuhn("423646643"));
        assertFalse(validateLuhn("4189231277"));
        assertFalse(validateLuhn("3941601643"));
        assertFalse(validateLuhn("3049254051"));
        assertFalse(validateLuhn("2324038570"));
        assertFalse(validateLuhn("2318610013"));
        assertFalse(validateLuhn("3424436428"));
        assertFalse(validateLuhn("2547597866"));
        assertFalse(validateLuhn("93214216"));
        assertFalse(validateLuhn("1118934985"));
        assertFalse(validateLuhn("2533600774"));
        assertFalse(validateLuhn("2773955884"));
        assertFalse(validateLuhn("2586548382"));
        assertFalse(validateLuhn("319313528"));
        assertFalse(validateLuhn("3788114908"));
        assertFalse(validateLuhn("3865367972"));
        assertFalse(validateLuhn("2379273829"));
        assertFalse(validateLuhn("1889557132"));
        assertFalse(validateLuhn("3740082978"));
        assertFalse(validateLuhn("477182936"));
        assertFalse(validateLuhn("4079410192"));
        assertFalse(validateLuhn("242136626"));
        assertFalse(validateLuhn("3654739564"));
        assertFalse(validateLuhn("2681152772"));
        assertFalse(validateLuhn("3543499891"));
        assertFalse(validateLuhn("2701898946"));
        assertFalse(validateLuhn("3064898346"));
        assertFalse(validateLuhn("2086310111"));
        assertFalse(validateLuhn("315035024"));
        assertFalse(validateLuhn("403593642"));
        assertFalse(validateLuhn("1066883963"));
        assertFalse(validateLuhn("2726445073"));
        assertFalse(validateLuhn("3937438646"));
        assertFalse(validateLuhn("2534677247"));
        assertFalse(validateLuhn("3387630627"));
        assertFalse(validateLuhn("2006818881"));
        assertFalse(validateLuhn("4032867810"));
        assertFalse(validateLuhn("1095257309"));
        assertFalse(validateLuhn("2841923898"));
        assertFalse(validateLuhn("1331063085"));
        assertFalse(validateLuhn("116236061"));
        assertFalse(validateLuhn("1967204659"));
        assertFalse(validateLuhn("416070218"));
        assertFalse(validateLuhn("1057178451"));
        assertFalse(validateLuhn("3319596230"));
        assertFalse(validateLuhn("2673774471"));
        assertFalse(validateLuhn("3963343113"));
        assertFalse(validateLuhn("936531716"));
        assertFalse(validateLuhn("382724971"));
        assertFalse(validateLuhn("904105927"));
        assertFalse(validateLuhn("1871391278"));
        assertFalse(validateLuhn("3130081581"));
        assertFalse(validateLuhn("4059361904"));
        assertFalse(validateLuhn("3714616229"));
        assertFalse(validateLuhn("4015708833"));
        assertFalse(validateLuhn("3519864641"));
        assertFalse(validateLuhn("2706248333"));
        assertFalse(validateLuhn("388265254"));
        assertFalse(validateLuhn("175583925"));
        assertFalse(validateLuhn("3272693851"));
        assertFalse(validateLuhn("3296821468"));
        assertFalse(validateLuhn("4057853413"));
        assertFalse(validateLuhn("1710156309"));
        assertFalse(validateLuhn("3823186111"));
        assertFalse(validateLuhn("3466869908"));
        assertFalse(validateLuhn("2321599513"));
        assertFalse(validateLuhn("3057128038"));
        assertFalse(validateLuhn("953972225"));
        assertFalse(validateLuhn("395188"));
        assertFalse(validateLuhn("2078905303"));
        assertFalse(validateLuhn("1276633190"));
        assertFalse(validateLuhn("2507894399"));
        assertFalse(validateLuhn("277038187"));
        assertFalse(validateLuhn("412128760"));
        assertFalse(validateLuhn("2943125634"));
        assertFalse(validateLuhn("776811136"));
        assertFalse(validateLuhn("3399817169"));
        assertFalse(validateLuhn("2611010924"));
        assertFalse(validateLuhn("661442521"));
        assertFalse(validateLuhn("1215280457"));
        assertFalse(validateLuhn("2815909804"));
        assertFalse(validateLuhn("1238511920"));
        assertFalse(validateLuhn("1308763876"));
    }

    @Test
    public void testCalculation() {
        assertEquals(4, calculateLuhn("3524280191"));
        assertEquals(7, calculateLuhn("2162879206"));
        assertEquals(9, calculateLuhn("468820099"));
        assertEquals(5, calculateLuhn("1841157647"));
        assertEquals(4, calculateLuhn("1545923558"));
        assertEquals(8, calculateLuhn("3505726769"));
        assertEquals(4, calculateLuhn("1270456073"));
        assertEquals(2, calculateLuhn("1350238745"));
        assertEquals(5, calculateLuhn("297648390"));
        assertEquals(6, calculateLuhn("1843301911"));
        assertEquals(3, calculateLuhn("855896294"));
        assertEquals(4, calculateLuhn("1339351812"));
        assertEquals(5, calculateLuhn("2931244069"));
        assertEquals(0, calculateLuhn("4293179176"));
        assertEquals(2, calculateLuhn("1039761808"));
        assertEquals(9, calculateLuhn("582144696"));
        assertEquals(0, calculateLuhn("191657718"));
        assertEquals(8, calculateLuhn("2577191480"));
        assertEquals(1, calculateLuhn("4272424725"));
        assertEquals(7, calculateLuhn("1347722771"));
        assertEquals(6, calculateLuhn("4291357200"));
        assertEquals(5, calculateLuhn("2367098207"));
        assertEquals(6, calculateLuhn("3267329712"));
        assertEquals(7, calculateLuhn("210530659"));
        assertEquals(9, calculateLuhn("2778144206"));
        assertEquals(9, calculateLuhn("2702657753"));
        assertEquals(1, calculateLuhn("1467634285"));
        assertEquals(3, calculateLuhn("10756416"));
        assertEquals(1, calculateLuhn("2018745132"));
        assertEquals(8, calculateLuhn("258813855"));
        assertEquals(0, calculateLuhn("2045829124"));
        assertEquals(1, calculateLuhn("2462276418"));
        assertEquals(1, calculateLuhn("2898416195"));
        assertEquals(8, calculateLuhn("1406469808"));
        assertEquals(5, calculateLuhn("485914030"));
        assertEquals(0, calculateLuhn("3349988592"));
        assertEquals(3, calculateLuhn("890535187"));
        assertEquals(4, calculateLuhn("464388418"));
        assertEquals(3, calculateLuhn("4110810463"));
        assertEquals(5, calculateLuhn("4089731496"));
        assertEquals(9, calculateLuhn("1323902639"));
        assertEquals(3, calculateLuhn("2710573885"));
        assertEquals(6, calculateLuhn("1902004343"));
        assertEquals(8, calculateLuhn("4037723041"));
        assertEquals(4, calculateLuhn("836953707"));
        assertEquals(9, calculateLuhn("2586413396"));
        assertEquals(9, calculateLuhn("3157553598"));
        assertEquals(0, calculateLuhn("4036721495"));
        assertEquals(6, calculateLuhn("829504720"));
        assertEquals(2, calculateLuhn("1825557101"));
        assertEquals(9, calculateLuhn("3195187675"));
        assertEquals(2, calculateLuhn("1853435002"));
        assertEquals(6, calculateLuhn("1201030091"));
        assertEquals(7, calculateLuhn("1549083952"));
        assertEquals(1, calculateLuhn("3600954721"));
        assertEquals(2, calculateLuhn("2228034841"));
        assertEquals(8, calculateLuhn("1846380485"));
        assertEquals(6, calculateLuhn("3299485817"));
        assertEquals(7, calculateLuhn("4266356531"));
        assertEquals(4, calculateLuhn("80494393"));
        assertEquals(1, calculateLuhn("3338502087"));
        assertEquals(4, calculateLuhn("1210755169"));
        assertEquals(8, calculateLuhn("4126449397"));
        assertEquals(0, calculateLuhn("1362375873"));
        assertEquals(0, calculateLuhn("3113577816"));
        assertEquals(5, calculateLuhn("1188635514"));
        assertEquals(1, calculateLuhn("2946063998"));
        assertEquals(0, calculateLuhn("1719371154"));
        assertEquals(3, calculateLuhn("1895514650"));
        assertEquals(4, calculateLuhn("2080829998"));
        assertEquals(3, calculateLuhn("3609894519"));
        assertEquals(2, calculateLuhn("3511856319"));
        assertEquals(5, calculateLuhn("1952932537"));
        assertEquals(4, calculateLuhn("1910620955"));
        assertEquals(1, calculateLuhn("935913671"));
        assertEquals(9, calculateLuhn("725760186"));
        assertEquals(4, calculateLuhn("233933984"));
        assertEquals(7, calculateLuhn("1968137531"));
        assertEquals(7, calculateLuhn("3437612629"));
        assertEquals(4, calculateLuhn("3516015717"));
        assertEquals(0, calculateLuhn("1945185765"));
        assertEquals(7, calculateLuhn("207931382"));
        assertEquals(3, calculateLuhn("2373789959"));
        assertEquals(1, calculateLuhn("3847636398"));
        assertEquals(1, calculateLuhn("1062556296"));
        assertEquals(1, calculateLuhn("4085951795"));
        assertEquals(5, calculateLuhn("2630252765"));
        assertEquals(0, calculateLuhn("3970196936"));
        assertEquals(4, calculateLuhn("21259608"));
        assertEquals(7, calculateLuhn("2238013911"));
        assertEquals(3, calculateLuhn("1319502209"));
        assertEquals(9, calculateLuhn("895861044"));
        assertEquals(9, calculateLuhn("1585306656"));
        assertEquals(9, calculateLuhn("3367246111"));
        assertEquals(8, calculateLuhn("903071289"));
        assertEquals(3, calculateLuhn("2430231960"));
        assertEquals(9, calculateLuhn("345922272"));
        assertEquals(8, calculateLuhn("1233909707"));
        assertEquals(5, calculateLuhn("2553083072"));
        assertEquals(8, calculateLuhn("3053346265"));
    }
}
