/*
 * NumericalStringComparator.java
 *
 * Copyright 2016-2017 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a string comparator that cares about numbers in strings, eg: z13 > z9
 * <p>
 * Note that in order to keep the implementation simple, this will only pick one of the groups of
 * numbers in a String to compare with. If there are multiple groups of numbers, this will pick
 * whichever number appears in each string that is the largest, and use that to split up the string.
 * <p>
 * In this case, the algorithm will fall back to standard character-based (non-lexicographic
 * ordering), and z9y300 > z100y6 (which is unexpected for lexicographic ordering).
 * <p>
 * If the non-numeric part of a string is different, this will fall back to standard character-based
 * ordering.
 * <p>
 * This doesn't attempt to handle dates, numbers as words (one, dos, drei, quattro), decimal points,
 * fractions, or negative numbers.  It is best on small strings.
 */
public class NumericalStringComparator implements Comparator<String> {
    private static final Pattern NUMBER_FINDER = Pattern.compile("^(.*\\D)?(\\d+)(\\D.*)?$");

    @Override
    public int compare(String str1, String str2) {
        Matcher m1 = NUMBER_FINDER.matcher(str1);
        Matcher m2 = NUMBER_FINDER.matcher(str2);
        //Log.d("NSC", "str1: " + str1 + ", str2: " + str2);

        if (m1.matches() && m2.matches() && (
                (m1.group(1) == null && m2.group(1) == null) || (
                        m1.group(1) != null && m2.group(1) != null && m1.group(1).equalsIgnoreCase(m2.group(1)))
        )) {
            // All other parts of the string match, lets do a numerical comparison
            Integer i1 = Integer.valueOf(m1.group(2));
            Integer i2 = Integer.valueOf(m2.group(2));
            int r = i1.compareTo(i2);
            //Log.d("NSC", "i1: " + i1.toString() + ", i2: " + i2.toString() + ", r = " + r);
            if (r == 0) {
                if (m1.group(3) == null && m2.group(3) == null) {
                    return 0;
                } else if (m1.group(3) == null) {
                    return -1;
                } else if (m2.group(3) == null) {
                    return 1;
                } else {
                    r = m1.group(3).compareTo(m2.group(3));
                }
            }

            if (r < 0) {
                return -1;
            } else if (r > 0) {
                return 1;
            } else {
                return 0;
            }
        } else {
            int r = str1.compareToIgnoreCase(str2);
            if (r < 0) {
                return -1;
            } else if (r > 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
