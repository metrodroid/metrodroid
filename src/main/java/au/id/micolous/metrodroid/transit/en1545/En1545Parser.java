/*
 * En1545Parser.java
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
package au.id.micolous.metrodroid.transit.en1545;

public class En1545Parser {
    public static En1545Parsed parse(byte[] data, int off, En1545Field field) {
        return new En1545Parsed().append(data, off, field);
    }
    public static En1545Parsed parse(byte[] data, En1545Field field) {
        return parse(data, 0, field);
    }
}
