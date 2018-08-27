/*
 * IntercodeTrip.java
 *
 * Copyright 2009 by 'L1L1'
 * Copyright 2013-2014 by 'kalon33'
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it &&/or modify
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

package au.id.micolous.metrodroid.transit.intercode;

class IntercodeLookupGironde extends IntercodeLookupSTR {
        private final int TRANSGIRONDE = 16;

        IntercodeLookupGironde() {
            super("gironde");
        }

        @Override
        public String getRouteName(Integer routeNumber, Integer routeVariant, int agency, int transport) {
            if (routeNumber == null)
                return null;
            if (agency == TRANSGIRONDE)
                return "Ligne " + routeNumber;
            return super.getRouteName(routeNumber, routeNumber, agency, transport);
        }
}
