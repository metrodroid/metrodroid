/*
 * KeyFormat.java
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

package au.id.micolous.metrodroid.util;

/**
 * Used by {@link Utils#detectKeyFormat(byte[])} to return the format of a key contained within a
 * file.
 */
public enum KeyFormat {
    /** Format is unknown */
    UNKNOWN,
    /** Traditional raw (farebotkeys) binary format */
    RAW_MFC,
    /** JSON format */
    JSON
}
