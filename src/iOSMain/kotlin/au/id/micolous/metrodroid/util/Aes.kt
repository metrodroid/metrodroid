package au.id.micolous.metrodroid.util

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.*

actual object Aes {
    private fun aesOp(mode: CCOperation, input: ImmutableByteArray, key: ImmutableByteArray, iv: ImmutableByteArray) : ImmutableByteArray {
        if (key.size != 16 || iv.size != 16 || input.size % 16 != 0)
            throw IllegalArgumentException("Invalid crypto argument size")
        val output = ByteArray(input.size) { 0.toByte() }
        val outputSize = ULongArray(1) { 0u }
        key.dataCopy.usePinned { keyPinned ->
            iv.dataCopy.usePinned { ivPinned ->
                input.dataCopy.usePinned { inputPinned ->
                    output.usePinned { outputPinned ->
                        outputSize.usePinned { outputSizePinned ->
                            val ccStatus = CCCrypt(mode,
                                    kCCAlgorithmAES128,
                                    0,
                                    keyPinned.addressOf(0),
                                    key.size.toULong(),
                                    ivPinned.addressOf(0),
                                    inputPinned.addressOf(0),
                                    input.size.toULong(),
                                    outputPinned.addressOf(0),
                                    output.size.toULong(),
                                    outputSizePinned.addressOf(0))
                        }
                    }
                }
            }
        }
        return output.toImmutable()
    }

    actual fun decryptCbc(encrypted: ImmutableByteArray, key: ImmutableByteArray,
                          iv: ImmutableByteArray): ImmutableByteArray = aesOp(
            kCCDecrypt, encrypted, key, iv
    )

    actual fun encryptCbc(decrypted: ImmutableByteArray, key: ImmutableByteArray,
                          iv: ImmutableByteArray): ImmutableByteArray = aesOp(
            kCCEncrypt, decrypted, key, iv
    )
}
