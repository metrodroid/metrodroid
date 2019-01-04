/*
 * HexString.java
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

class HexString (data: ImmutableByteArray): ImmutableByteArray(data) {
    val data
        get() = dataCopy
    constructor(data: ByteArray) : this(fromByteArray(data))
    constructor(hexString: String) : this(fromHex(hexString))

    class Transform : org.simpleframework.xml.transform.Transform<HexString> {
        override fun read(value: String): HexString {
            return HexString(ImmutableByteArray.fromHex(value))
        }

        override fun write(value: HexString): String {
            return value.toHexString()
        }
    }
}
