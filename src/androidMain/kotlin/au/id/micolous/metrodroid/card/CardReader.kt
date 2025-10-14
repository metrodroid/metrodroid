package au.id.micolous.metrodroid.card

import android.nfc.Tag
import android.nfc.tech.*
import android.util.Log
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader
import au.id.micolous.metrodroid.card.desfire.DesfireCardReader
import au.id.micolous.metrodroid.card.felica.FelicaReader
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card
import au.id.micolous.metrodroid.card.nfcv.NFCVCard
import au.id.micolous.metrodroid.card.nfcv.NFCVCardReader
import au.id.micolous.metrodroid.card.ultralight.AndroidUltralightTransceiver
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardReader
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.toImmutable


object CardReader {
    private const val TAG = "CardReader"

    /* Doesn't block and doesn't cause any RF activity. */
    private fun getAtqaSak(tag: NfcA): Pair<Int, Short> = Pair(tag.atqa.toImmutable().byteArrayToIntReversed(), tag.sak)

    private fun getAtqaSak(tag: Tag): Pair<Int, Short>? = NfcA.get(tag)?.let { getAtqaSak(it) }

    fun dumpTag(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): Card {
        val techs = tag.techList
        val tagId = tag.id.toImmutable()
        Log.d(TAG, "Reading tag ${tagId.toHexString()}. ${techs.size} tech(s) supported:")
        for (tech in techs) {
            Log.d(TAG, tech)
        }

        if (IsoDep::class.java.name in techs) {
            feedbackInterface.updateStatusText(Localizer.localizeString(R.string.iso14a_detect))

            // ISO 14443-4 card types
            // This also encompasses NfcA (ISO 14443-3A) and NfcB (ISO 14443-3B)
            AndroidIsoTransceiver(tag).use {
                it.connect()

                val d = DesfireCardReader.dumpTag(it, feedbackInterface)
                if (d != null) {
                    return Card(tagId = tagId, scannedAt = TimestampFull.now(), mifareDesfire = d)
                }
                
                val isoCard = ISO7816Card.dumpTag(it, feedbackInterface)
                if (isoCard.applications.isEmpty() && NfcA::class.java.name in techs) {
                    val p = getAtqaSak(tag)?.let { (atqa,sak) ->
                        ClassicAndroidReader.dumpPlus(it, feedbackInterface, atqa, sak)
                    }
                    if (p != null)
                        return Card(tagId = tagId, scannedAt = TimestampFull.now(), mifareClassic = p)
                }
                return Card(tagId = tagId, scannedAt = TimestampFull.now(), iso7816 = isoCard)
            }
        }

        if (NfcF::class.java.name in techs) {
            val c = AndroidFelicaTransceiver(tag).use {
                it.connect()
                FelicaReader.dumpTag(
                    it, feedbackInterface, onlyFirst = Preferences.felicaOnlyFirst
                )
            }
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

        if (NfcA::class.java.name in techs && getAtqaSak(tag) == Pair(0x0044, 0.toShort())) {
            val u = dumpTagA(tag, feedbackInterface)
            if (u != null)
                return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                        mifareUltralight = u)
        }

        if (NfcV::class.java.name in techs) {
            val u = dumpTagV(tag, feedbackInterface)
            return Card(tagId = tagId, scannedAt = TimestampFull.now(),
                        vicinity = u)
        }

        throw UnsupportedTagProtocolException(techs.toList(), tagId.toHexString())
    }

    private fun dumpTagUL(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): UltralightCard =
        AndroidUltralightTransceiver(tag).use {
            it.connect()
            UltralightCardReader.dumpTag(it, feedbackInterface)
                    ?: throw UnknownUltralightException()
        }

    private fun dumpTagA(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): UltralightCard? =
        AndroidNfcATransceiver(tag).use {
            it.connect()
            UltralightCardReader.dumpTagA(it, feedbackInterface)
        }

    private fun dumpTagV(tag: Tag, feedbackInterface: TagReaderFeedbackInterface): NFCVCard =
        AndroidNfcVTransceiver(tag).use {
            it.connect()
            NFCVCardReader.dumpTag(it, feedbackInterface)
        }
}
