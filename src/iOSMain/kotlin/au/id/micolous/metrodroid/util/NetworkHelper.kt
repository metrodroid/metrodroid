package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import platform.Foundation.*
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.SharedImmutable
import kotlin.native.concurrent.freeze

private const val TAG = "NetworkHelper"

object NetworkHelperReal : NetworkHelper {
    object Callback {
        private fun callback(channel: SendChannel<ImmutableByteArray?>, dat: NSData?, response: NSURLResponse?,
                             error: NSError?) {
            val respTyped = response as NSHTTPURLResponse
            runBlocking {
                if (error == null && respTyped.statusCode == 200.toLong())
                    channel.send(dat?.toByteArray()?.toImmutable())
                else
                    channel.send(null)
            }
        }

        fun frozenLambda(channel: SendChannel<ImmutableByteArray?>): ((dat: NSData?, response: NSURLResponse?,
                     error: NSError?)->Unit) {
            channel.freeze()
            val l = { dat: NSData?, response: NSURLResponse?, error: NSError? ->
                callback(channel, dat, response, error)
            }

            l.freeze()
            return l
        }
    }
    override fun sendPostRequest(urlString: String,
                                 request: ByteArray): ByteArray? {
        try {
            Log.d(TAG, "Sending packet " + request.toImmutable())
            val url = NSURL(string = urlString)
            val conn = NSMutableURLRequest.requestWithURL(url)
            val session = NSURLSession.sharedSession
            conn.setAllowsConstrainedNetworkAccess(true)
            conn.setAllowsExpensiveNetworkAccess(true)
            conn.setAllowsCellularAccess(true)
            conn.HTTPMethod = "POST"
            conn.setValue(null, "Content-Type")
            conn.setValue("Metrodroid/" + Preferences.metrodroidVersion, "User-Agent")
            conn.HTTPBody = request.toNSData()
            val channel = Channel<ImmutableByteArray?>()
            channel.freeze()
            val callback = Callback.frozenLambda(channel)
            val task =
                    session.dataTaskWithRequest(conn, completionHandler = callback)
            task.resume()
            return runBlocking { channel.receive() }?.dataCopy
        } catch (e: Exception) {
            return null
        }
    }

    override fun randomUUID() = NSUUID().UUIDString
}

@SharedImmutable
private val networkHelperRef = AtomicReference<NetworkHelper>(NetworkHelperReal)
actual var networkHelper: NetworkHelper
    get() = networkHelperRef.value
    set(value) { networkHelperRef.value = value}
