package au.id.micolous.farebot.test;

import com.codebutler.farebot.transit.seq_go.SeqGoFareCalculator;
import com.codebutler.farebot.transit.seq_go.SeqGoTrip;

import junit.framework.TestCase;

import java.util.GregorianCalendar;

/**
 * Implements tests for Go card fares.
 */
public class SeqGoFareTest extends TestCase {
    private SeqGoFareCalculator fareCalculator = new SeqGoFareCalculator();

    /**
     * Tests which handle trips exclusively in zone 1.
     * @throws Exception
     */
    public void testOneZone() throws Exception {
        SeqGoTrip trip;

        // Off-peak time (02:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 2, 0), // 2016-03-15 02:00
                new GregorianCalendar(2016, 2, 15, 2, 5), // 2016-03-15 02:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (03:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 3, 0), // 2016-03-15 03:00
                new GregorianCalendar(2016, 2, 15, 3, 5), // 2016-03-15 03:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (07:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 7, 0), // 2016-03-15 07:00
                new GregorianCalendar(2016, 2, 15, 7, 5), // 2016-03-15 07:05
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (08:15)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 8, 15), // 2016-03-15 08:15
                new GregorianCalendar(2016, 2, 15, 8, 20), // 2016-03-15 08:20
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (08:45)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 8, 45), // 2016-03-15 08:45
                new GregorianCalendar(2016, 2, 15, 8, 50), // 2016-03-15 08:50
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (10:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 10, 0), // 2016-03-15 10:00
                new GregorianCalendar(2016, 2, 15, 10, 5), // 2016-03-15 10:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (15:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 15, 0), // 2016-03-15 15:00
                new GregorianCalendar(2016, 2, 15, 15, 5), // 2016-03-15 15:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (15:45)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 15, 45), // 2016-03-15 15:45
                new GregorianCalendar(2016, 2, 15, 15, 50), // 2016-03-15 15:50
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (18:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 18, 0), // 2016-03-15 18:00
                new GregorianCalendar(2016, 2, 15, 18, 5), // 2016-03-15 18:05
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (22:00)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 15, 22, 0), // 2016-03-15 22:00
                new GregorianCalendar(2016, 2, 15, 22, 5), // 2016-03-15 22:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));
    }

    /**
     * Tests which handle a trip in zone 1 and 2.
     * @throws Exception
     */
    public void testTwoZone() throws Exception {
        SeqGoTrip trip;

        // Off-peak time (02:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 2, 0), // 2016-03-15 02:00
                new GregorianCalendar(2016, 2, 15, 2, 5), // 2016-03-15 02:05
                1,
                false
        );

        assertEquals(314, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (03:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 3, 0), // 2016-03-15 03:00
                new GregorianCalendar(2016, 2, 15, 3, 5), // 2016-03-15 03:05
                1,
                false
        );

        assertEquals(314, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (07:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 7, 0), // 2016-03-15 07:00
                new GregorianCalendar(2016, 2, 15, 7, 5), // 2016-03-15 07:05
                1,
                false
        );

        assertEquals(393, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (08:15)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 8, 15), // 2016-03-15 08:15
                new GregorianCalendar(2016, 2, 15, 8, 20), // 2016-03-15 08:20
                1,
                false
        );

        assertEquals(393, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (08:45)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 8, 45), // 2016-03-15 08:45
                new GregorianCalendar(2016, 2, 15, 8, 50), // 2016-03-15 08:50
                1,
                false
        );

        assertEquals(314, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (10:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 10, 0), // 2016-03-15 10:00
                new GregorianCalendar(2016, 2, 15, 10, 5), // 2016-03-15 10:05
                1,
                false
        );

        assertEquals(314, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (15:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 15, 0), // 2016-03-15 15:00
                new GregorianCalendar(2016, 2, 15, 15, 5), // 2016-03-15 15:05
                1,
                false
        );

        assertEquals(314, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (15:45)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 15, 45), // 2016-03-15 15:45
                new GregorianCalendar(2016, 2, 15, 15, 50), // 2016-03-15 15:50
                1,
                false
        );

        assertEquals(393, fareCalculator.calculateFareForTrip(trip, null));

        // Peak time (18:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 18, 0), // 2016-03-15 18:00
                new GregorianCalendar(2016, 2, 15, 18, 5), // 2016-03-15 18:05
                1,
                false
        );

        assertEquals(393, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak time (22:00)
        trip = new SeqGoTrip(
                5, // Central
                63, // Nundah
                new GregorianCalendar(2016, 2, 15, 22, 0), // 2016-03-15 22:00
                new GregorianCalendar(2016, 2, 15, 22, 5), // 2016-03-15 22:05
                1,
                false
        );

        assertEquals(314, fareCalculator.calculateFareForTrip(trip, null));

    }

    /**
     * Tests which handle a trip taken on a weekend.
     * @throws Exception
     */
    public void testWeekendAndPublicHolidays() throws Exception {
        SeqGoTrip trip;

        // Off-peak (Saturday)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 19, 7, 0), // 2016-03-19 07:00
                new GregorianCalendar(2016, 2, 19, 7, 5), // 2016-03-19 07:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak (Sunday)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 2, 20, 7, 0), // 2016-03-20 07:00
                new GregorianCalendar(2016, 2, 20, 7, 5), // 2016-03-20 07:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));

        // Off-peak (public holiday, ANZAC day)
        trip = new SeqGoTrip(
                5, // Central
                20, // Roma Street
                new GregorianCalendar(2016, 3, 25, 7, 0), // 2016-04-25 07:00
                new GregorianCalendar(2016, 3, 25, 7, 5), // 2016-04-25 07:05
                1,
                false
        );

        assertEquals(268, fareCalculator.calculateFareForTrip(trip, null));
    }

    /**
     * Tests which handle a stop that belongs to multiple zones.
     * @throws Exception
     */
    public void testDoubleZone() throws Exception {
        SeqGoTrip trip;

        // Zone 1
        trip = new SeqGoTrip(
                4, // Fortitude Valley (zone 1)
                999, // Vulture St at State High, stop 8 (zone 1/2)
                new GregorianCalendar(2016, 2, 15, 7, 0), // 2016-03-15 07:00
                new GregorianCalendar(2016, 2, 15, 7, 5), // 2016-03-15 07:05
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        trip = new SeqGoTrip(
                999, // Vulture St at State High, stop 8 (zone 1/2)
                4, // Fortitude Valley (zone 1)
                new GregorianCalendar(2016, 2, 15, 7, 0), // 2016-03-15 07:00
                new GregorianCalendar(2016, 2, 15, 7, 5), // 2016-03-15 07:05
                1,
                false
        );

        // Zone 2
        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        trip = new SeqGoTrip(
                2423, // Racecourse Rd at Beatrice Street, stop 22 (zone 2)
                999, // Vulture St at State High, stop 8 (zone 1/2)
                new GregorianCalendar(2016, 2, 15, 7, 0), // 2016-03-15 07:00
                new GregorianCalendar(2016, 2, 15, 7, 5), // 2016-03-15 07:05
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

        trip = new SeqGoTrip(
                999, // Vulture St at State High, stop 8 (zone 1/2)
                2423, // Racecourse Rd at Beatrice Street, stop 22 (zone 2)
                new GregorianCalendar(2016, 2, 15, 7, 0), // 2016-03-15 07:00
                new GregorianCalendar(2016, 2, 15, 7, 5), // 2016-03-15 07:05
                1,
                false
        );

        assertEquals(335, fareCalculator.calculateFareForTrip(trip, null));

    }

    public void testAirtrain() throws Exception {
        SeqGoTrip trip;

        trip = new SeqGoTrip(
                5, // Central
                9, // Domestic Airport
                new GregorianCalendar(2016, 2, 15, 18, 0), // 2016-03-15 18:00
                new GregorianCalendar(2016, 2, 15, 18, 30), // 2016-03-15 18:30
                1,
                false
        );

        assertEquals(1750, fareCalculator.calculateFareForTrip(trip, null));

        trip = new SeqGoTrip(
                9, // Domestic Airport
                5, // Central
                new GregorianCalendar(2016, 2, 15, 18, 0), // 2016-03-15 18:00
                new GregorianCalendar(2016, 2, 15, 18, 30), // 2016-03-15 18:30
                1,
                false
        );

        assertEquals(1750, fareCalculator.calculateFareForTrip(trip, null));

        // Zone 2, on airport line
        trip = new SeqGoTrip(
                60, // Wooloowin
                9, // Domestic Airport
                new GregorianCalendar(2016, 2, 15, 18, 10), // 2016-03-15 18:10
                new GregorianCalendar(2016, 2, 15, 18, 30), // 2016-03-15 18:30
                1,
                false
        );

        assertEquals(1750, fareCalculator.calculateFareForTrip(trip, null));

        trip = new SeqGoTrip(
                9, // Domestic Airport
                60, // Wooloowin
                new GregorianCalendar(2016, 2, 15, 18, 10), // 2016-03-15 18:10
                new GregorianCalendar(2016, 2, 15, 18, 30), // 2016-03-15 18:30
                1,
                false
        );

        assertEquals(1750, fareCalculator.calculateFareForTrip(trip, null));

        // Transfer from Zone 2
        trip = new SeqGoTrip(
                88, // Fairfield
                9, // Domestic Airport
                new GregorianCalendar(2016, 2, 15, 18, 10), // 2016-03-15 18:10
                new GregorianCalendar(2016, 2, 15, 18, 30), // 2016-03-15 18:30
                1,
                false
        );

        assertEquals(2143, fareCalculator.calculateFareForTrip(trip, null));

        trip = new SeqGoTrip(
                9, // Domestic Airport
                88, // Fairfield
                new GregorianCalendar(2016, 2, 15, 18, 10), // 2016-03-15 18:10
                new GregorianCalendar(2016, 2, 15, 18, 30), // 2016-03-15 18:30
                1,
                false
        );

        assertEquals(2143, fareCalculator.calculateFareForTrip(trip, null));


    }
}
