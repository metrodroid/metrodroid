package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.multi.Log

object HashUtils {
    private fun calculateCRCReversed(data: ImmutableByteArray, init: Int, table: IntArray) =
            data.fold(init) { cur1, b -> (cur1 shr 8) xor table[(cur1 xor b.toInt()) and 0xff] }

    private fun getCRCTableReversed(poly: Int) =
            (0..255).map { v ->
                (0..7).fold(v) { cur, _ ->
                    if ((cur and 1) != 0)
                        (cur shr 1) xor poly
                    else
                        (cur shr 1)
                }
            }.toIntArray()

    private val CRC16_IBM_TABLE = getCRCTableReversed(0xa001)

    fun calculateCRC16IBM(data: ImmutableByteArray, crc: Int = 0) =
            calculateCRCReversed(data, crc, CRC16_IBM_TABLE)

    /**
     * Checks if a salted hash of a value is found in a group of expected hash values.
     *
     * This is only really useful for MIFARE Classic cards, where the only way to identify a
     * particular transit card is to check the key against a known list.  We don't want to ship
     * any agency-specific keys with Metrodroid (no matter how well-known they are), so this
     * obfuscates the keys.
     *
     * It is fairly straight forward to crack any MIFARE Classic card anyway, and this is only
     * intended to be "on par" with the level of security on the cards themselves.
     *
     * This isn't useful for **all** cards, and should only be used as a last resort.  Many transit
     * cards implement key diversification on all sectors (ie: every sector of every card has a key
     * that is unique to a single card), which renders this technique unusable.
     *
     * The hash is defined as:
     *
     *    hash = lowercase(base16(md5(salt + key + salt)))
     *
     * @param key The key to test.
     * @param salt The salt string to add to the key.
     * @param expectedHashes Expected hash values that might be returned.
     * @return The index of the hash that matched, or a number less than 0 if the value was not
     *         found, or there was some other error with the input.
     */
    fun checkKeyHash(key: ImmutableByteArray, salt: String, vararg expectedHashes: String): Int {
        // Validate input arguments.
        if (expectedHashes.isEmpty()) {
            return -1
        }

        val md5 = MD5Ctx()

        md5.update(ImmutableByteArray.fromASCII(salt))
        md5.update(key)
        md5.update(ImmutableByteArray.fromASCII(salt))

        val digest = md5.digest().toHexString()
        Log.d(TAG, "Key digest: $digest")

        return expectedHashes.indexOf(digest)
    }

    /**
     * Checks a keyhash with a {@link ClassicSectorKey}.
     *
     * See {@link #checkKeyHash(ImmutableByteArray, String, String...)} for further information.
     *
     * @param key The key to check. If this is null, then this will always return a value less than
     *            0 (ie: error).
     */
    fun checkKeyHash(key: ClassicSectorKey?, salt: String, vararg expectedHashes: String): Int {
        if (key == null)
            return -1
        return checkKeyHash(key.key, salt, *expectedHashes)
    }

    fun checkKeyHash(key: ClassicSector?, salt: String, vararg expectedHashes: String): Int {
        if (key == null)
            return -1
        val a = checkKeyHash(key.keyA, salt, *expectedHashes)
        if (a != -1)
            return a
        return checkKeyHash(key.keyB, salt, *expectedHashes)
    }

    private const val TAG = "HashUtils"
}
