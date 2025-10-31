package au.id.micolous.metrodroid.card

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSError

abstract class CardTransceiverIOSCommon<CAPSULE: CardTransceiverIOSCommon.CapsuleInterface>: CardTransceiver {
    interface CapsuleInterface {
        fun makeData(): Pair<ImmutableByteArray?, NSError?>
    }

    abstract fun sendData(chan: SendChannel<CAPSULE>, data: ImmutableByteArray)

    final override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        Log.d(TAG, ">>> $data")
        val (repl, err) = runBlocking {
            val chan = Channel<CAPSULE>()
            sendData(chan, data)
            chan.receive().makeData()
        }
        if (err != null || repl == null) {
            Log.d(TAG, "<!< $err")
            if (err?.code == 100L)
                throw CardLostException(err.toString())
            throw CardTransceiveException(err.toString())
        } else {
            Log.d(TAG, "<<< $repl")
            return repl
        }
    }

    companion object {
        private const val TAG = "CardTransceiverIOSCommon"
    }
}
