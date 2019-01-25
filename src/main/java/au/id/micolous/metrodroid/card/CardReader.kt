package au.id.micolous.metrodroid.card

import android.nfc.Tag
import android.nfc.tech.*
import android.util.Log
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader
import au.id.micolous.metrodroid.card.desfire.DesfireCardReader
import au.id.micolous.metrodroid.card.felica.FelicaReader
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.card.ultralight.UltralightCardReader
import au.id.micolous.metrodroid.card.ultralight.UltralightCardReaderA
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.ImmutableByteArray

import java.util.Locale
import au.id.micolous.metrodroid.card.ultralight.AndroidUltralightTransceiver
import android.nfc.tech.MifareUltralight
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.toImmutable


object CardReader {
    private const val TAG = "CardReader"

    suspend fun dumpTag(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): Card {
        val techs = tag.techList
        val tagId = tag.id.toImmutable()
        Log.d(TAG, String.format(Locale.ENGLISH, "Reading tag %s. %d tech(s) supported:",
                tagId.toHexString(), techs.size))
        for (tech in techs) {
            Log.d(TAG, tech)
        }

        if (IsoDep::class.java.name in techs) {
            feedbackInterface.updateStatusText(Localizer.localizeString(R.string.iso14a_detect))

            // ISO 14443-4 card types
            // This also encompasses NfcA (ISO 14443-3A) and NfcB (ISO 14443-3B)
            val tech = IsoDep.get(tag)
            tech.connect()
            val transceiver = AndroidCardTransceiver(tech::transceive)

            val d = DesfireCardReader.dumpTag(transceiver, feedbackInterface)
            if (d != null) {
                if (tech.isConnected)
                    tech.close()
                return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                        mifareDesfire = d)
            }

            val isoCard = ISO7816Card.dumpTag(transceiver, feedbackInterface)
            if (tech.isConnected)
                tech.close()
            return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                    iso7816 = isoCard)
        }

        if (NfcF::class.java.name in techs) {
            val tech = NfcF.get(tag)
            tech.connect()
            Log.d(TAG, "Default system code: " + ImmutableByteArray.getHexString(tech.systemCode))
            val c = FelicaReader.dumpTag(AndroidCardTransceiver(tech::transceive), feedbackInterface)
            if (tech.isConnected)
                tech.close()
            return Card(tagId = tagId, scannedAt = TimestampFull.now(), felica = c)
        }

        if (MifareClassic::class.java.name in techs) {
            return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                    mifareClassic =
                    ClassicAndroidReader.dumpTag(tagId, tag, feedbackInterface))
        }

        if (MifareUltralight::class.java.name in techs) {
            return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                    mifareUltralight = dumpTagUL(tag, feedbackInterface))
        }

        if (NfcA::class.java.name in techs) {
            val u = dumpTagA(tag, feedbackInterface)
            if (u != null)
                return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                        mifareUltralight = u)
        }

        throw UnsupportedTagException(techs, tagId.toHexString())
    }

    @Throws(Exception::class)
    fun dumpTagUL(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): UltralightCard {
        val tech = MifareUltralight.get(tag)?: throw UnsupportedTagException(arrayOf("Ultralight"), "Ultralight interface failed")

        try {
            tech.connect()
            return UltralightCardReader.dumpTag(AndroidUltralightTransceiver(tech), feedbackInterface)
                    ?: throw UnsupportedTagException(arrayOf("Ultralight"), "Unknown Ultralight type")
        } finally {
            if (tech.isConnected) {
                tech.close()
            }
        }
    }

    private suspend fun dumpTagA(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): UltralightCard? {
        var card: UltralightCard? = null

        val tech = NfcA.get(tag) ?: return null

        try {
            tech.connect()
            if (tech.sak == 0.toShort()
                    && tech.atqa?.contentEquals(byteArrayOf(0x44, 0x00)) == true)
                card = UltralightCardReaderA.dumpTagA(
                        AndroidCardTransceiver(tech::transceive),
                        feedbackInterface)
        } finally {
            if (tech.isConnected) {
                tech.close()
            }
        }
        return card
    }
}
