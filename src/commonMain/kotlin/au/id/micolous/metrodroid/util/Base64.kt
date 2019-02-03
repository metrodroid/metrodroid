package au.id.micolous.metrodroid.util

/**
 * Decodes a Base-64 encoded string
 *
 * Returns null on error.
 */
expect fun decodeBase64(src: String): ByteArray?
