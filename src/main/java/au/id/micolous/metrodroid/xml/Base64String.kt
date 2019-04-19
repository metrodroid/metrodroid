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

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import au.id.micolous.metrodroid.util.ImmutableByteArray

class Base64String (data: ImmutableByteArray): ImmutableByteArray(data) {
    val data
      get() = dataCopy

    constructor(parcel: Parcel) : this(fromHex(parcel.readString()))

    constructor(data: ByteArray) : this(fromByteArray(data))

    companion object {
        fun empty() = Base64String(ImmutableByteArray.empty())

        @JvmStatic
        val CREATOR = object : Parcelable.Creator<Base64String> {
            override fun createFromParcel(parcel: Parcel): Base64String {
                return Base64String(parcel)
            }

            override fun newArray(size: Int): Array<Base64String?> {
                return arrayOfNulls(size)
            }
        }
    }

    class Transform : org.simpleframework.xml.transform.Transform<Base64String> {
        override fun read(value: String): Base64String {
            return Base64String(data = fromByteArray(Base64.decode(value, Base64.DEFAULT)))
        }

        override fun write(value: Base64String): String {
            return Base64.encodeToString(value.dataCopy, Base64.NO_WRAP)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(toHexString())
    }

    override fun describeContents(): Int {
        return 0
    }
}
