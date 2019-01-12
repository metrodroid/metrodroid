package au.id.micolous.metrodroid.card

import android.nfc.tech.IsoDep

import java.io.IOException

import au.id.micolous.metrodroid.xml.ImmutableByteArray

class AndroidCardTransceiver(private val mTech: IsoDep) : CardTransceiver {

    @Throws(IOException::class)
    override fun transceive(data: ImmutableByteArray): ImmutableByteArray {
        return ImmutableByteArray.fromByteArray(mTech.transceive(data.dataCopy))
    }
}
