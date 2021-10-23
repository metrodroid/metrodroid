package au.id.micolous.metrodroid.util

interface NetworkHelper {
    fun sendPostRequest(urlString: String, request: ByteArray): ByteArray?
    fun randomUUID(): String
}

expect var networkHelper: NetworkHelper
