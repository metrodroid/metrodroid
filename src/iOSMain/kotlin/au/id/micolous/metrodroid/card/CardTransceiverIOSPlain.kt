package au.id.micolous.metrodroid.card

import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking


abstract class CardTransceiverIOSPlain: CardTransceiverIOSCommon<CardTransceiverIOSPlain.Companion.Capsule>() {
    abstract fun send(dt: ImmutableByteArray, cb: (NSData?, NSError?) -> Unit)

    override fun sendData(chan: SendChannel<Capsule>, data: ImmutableByteArray) {
        val lambda = { reply: NSData?, error: NSError? ->
            callback(chan, reply, error)
        }
        send(data, lambda)
    }

    companion object {
        fun callback(channel: SendChannel<Capsule>, reply: NSData?,
                     error: NSError?) {
            val capsule = Capsule(reply, error)
            runBlocking {
                channel.send(capsule)
                channel.close()
            }
        }

        data class Capsule(
            val reply: NSData?,
            val err: NSError?): CapsuleInterface {
            override fun makeData() = Pair(reply?.toImmutable(), err)
        }
    }
}