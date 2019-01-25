/*
 * ClassicBlock.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ClassicBlock(@SerialName("data") private val mData: ImmutableByteArray) {
    val isUnauthorized: Boolean
        get() = mData == ImmutableByteArray.of(0x04)

    val data: ImmutableByteArray
        get() {
            if (isUnauthorized)
                throw UnauthorizedException()
            return mData
        }

    val isEmpty: Boolean
        get() {
            if (isUnauthorized)
                throw UnauthorizedException()
            return mData == ZERO || mData == FF || mData == ZERO_VB
        }

    companion object {
        fun create(data: ImmutableByteArray): ClassicBlock {
            return ClassicBlock(data)
        }

        fun createUnauthorized(): ClassicBlock {
            return ClassicBlock(ImmutableByteArray.fromHex("04"))
        }

        private val ZERO = ImmutableByteArray(16) { 0.toByte() }
        private val FF = ImmutableByteArray(16) { (-1).toByte() }
        private val ZERO_VB = ImmutableByteArray.fromHex("00000000ffffffff0000000000ff00ff")
    }
}
