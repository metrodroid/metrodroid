/*
 * KMTUtil.java
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package au.id.micolous.metrodroid.transit.kmt;

import net.kazzz.felica.lib.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

final class KMTUtil {
    private KMTUtil() {
    }

    static Calendar extractDate(byte[] data) {
        int fulloffset = Util.toInt(data[0], data[1], data[2], data[3]);
        if (fulloffset == 0) {
            return null;
        }
        Calendar c = new GregorianCalendar(KMTTransitData.TIME_ZONE);
        c.setTime(new Date((long) (fulloffset + 946684758) * 1000));
        return c;
    }
}
