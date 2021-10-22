package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object NetworkHelperReal : NetworkHelper {
    override fun sendPostRequest(urlString: String, request: ByteArray): ByteArray? {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            Log.d("POST", "Sending ${request.toImmutable().toHexString()}")
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", null)

            conn.setRequestProperty("User-Agent", "Metrodroid/" + Preferences.metrodroidVersion)
            val send = conn.outputStream

            send.write(request)
            val recv = conn.inputStream
            val recvBytes = recv.readBytes()
            conn.disconnect()
            Log.d("POST", "Received ${recvBytes.toImmutable().toHexString()}")
            return recvBytes
        } catch (e: IOException) {
            return null
        }
    }

    override fun randomUUID() = UUID.randomUUID().toString()
}

actual var networkHelper: NetworkHelper = NetworkHelperReal
