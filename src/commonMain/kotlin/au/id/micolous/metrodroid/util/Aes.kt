package au.id.micolous.metrodroid.util

expect object Aes {
    fun decryptCbc(encrypted: ImmutableByteArray, key: ImmutableByteArray,
                   iv: ImmutableByteArray=ImmutableByteArray.empty(16)): ImmutableByteArray

    fun encryptCbc(decrypted: ImmutableByteArray, key: ImmutableByteArray,
                   iv: ImmutableByteArray=ImmutableByteArray.empty(16)): ImmutableByteArray
}