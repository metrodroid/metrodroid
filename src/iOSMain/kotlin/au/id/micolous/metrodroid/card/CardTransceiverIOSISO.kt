package au.id.micolous.metrodroid.card

import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import au.id.micolous.metrodroid.util.toNSData
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import platform.CoreNFC.NFCISO7816APDU
import platform.Foundation.NSData
import platform.Foundation.NSError
import kotlin.native.concurrent.freeze

abstract class CardTransceiverIOSISO: CardTransceiverIOSCommon<CardTransceiverIOSISO.Companion.Capsule>() {
    override fun sendData(chan: SendChannel<Capsule>, data: ImmutableByteArray) {
        val lambda = { dataBack: NSData?, sw1: UByte, sw2: UByte, err: NSError? ->
            callback(channel = chan, reply = dataBack, sw1 = sw1, sw2 = sw2, error = err)
        }
        lambda.freeze()
        send(NFCISO7816APDU(data = data.toNSData()), lambda)
    }

    abstract fun send(apdu: NFCISO7816APDU,
                      completionHandler: (NSData?, UByte, UByte, NSError?) -> Unit)

    companion object {
        fun callback(channel: SendChannel<Capsule>, reply: NSData?,
                     sw1: UByte, sw2: UByte, error: NSError?) {
            val capsule = Capsule(reply, sw1, sw2, error)
            runBlocking {
                channel.send(capsule)
                channel.close()
            }
        }

        // Kotlin-objc interface behaves weirdly if we pass it directly,
        // packing as data class is nicer and works better
        data class Capsule (
            val rep: NSData?,
            val sw1: UByte,
            val sw2: UByte,
            val err: NSError?
        ): CapsuleInterface {
            init {
                freeze()
            }

            override fun makeData(): Pair<ImmutableByteArray?, NSError?> = Pair(
                rep?.toImmutable()?.plus(ImmutableByteArray.of(sw1.toByte(), sw2.toByte())),
                err)
        }
    }
}