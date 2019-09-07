/*
 * Aes.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.util

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object Aes {
    private fun aesOp(mode: Int, input: ImmutableByteArray, key: ImmutableByteArray, iv: ImmutableByteArray) : ImmutableByteArray {
        val cipherkey: SecretKey = SecretKeySpec(key.dataCopy, "AES")
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(mode, cipherkey, IvParameterSpec(iv.dataCopy))
        return cipher.doFinal(input.dataCopy).toImmutable()
    }

    actual fun decryptCbc(encrypted: ImmutableByteArray, key: ImmutableByteArray,
                          iv: ImmutableByteArray): ImmutableByteArray = aesOp(
            Cipher.DECRYPT_MODE, encrypted, key, iv
    )

    actual fun encryptCbc(decrypted: ImmutableByteArray, key: ImmutableByteArray,
                          iv: ImmutableByteArray): ImmutableByteArray = aesOp(
            Cipher.ENCRYPT_MODE, decrypted, key, iv
    )
}
