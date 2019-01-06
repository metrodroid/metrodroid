/*
 * Base64String.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.xml

class Base64String (data: ImmutableByteArray): ImmutableByteArray(data) {
    val data
      get() = dataCopy
    constructor(data: ByteArray) : this(fromByteArray(data))

    class Transform : org.simpleframework.xml.transform.Transform<Base64String> {
        override fun read(value: String): Base64String {
            return Base64String(data = ImmutableByteArray.fromBase64(value))
        }

        override fun write(value: Base64String): String {
            return value.toBase64()
        }
    }
}
