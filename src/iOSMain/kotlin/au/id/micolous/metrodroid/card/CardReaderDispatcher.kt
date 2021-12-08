/*
 * CardReaderDispatcher.kt
 *
 * Copyright 2021 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.card

import au.id.micolous.metrodroid.card.classic.PlusCardReaderIOS
import au.id.micolous.metrodroid.card.desfire.DesfireCardReaderIOS
import au.id.micolous.metrodroid.card.felica.FelicaCardReaderIOS
import au.id.micolous.metrodroid.card.iso7816.ISO7816CardReaderIOS
import au.id.micolous.metrodroid.card.nfcv.VicinityReaderIOS
import au.id.micolous.metrodroid.card.ultralight.UltralightCardReaderIOS
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.logAndSwiftWrap
import au.id.micolous.metrodroid.transit.CardInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.CoreNFC.*
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

object CardReaderDispatcher {
    @OptIn(DelicateCoroutinesApi::class)
    fun <T : NFCTagProtocol> readTagType(session: NFCTagReaderSession,
                                         tag: T, cardType: CardType,
                                         feedback: TagReaderFeedbackIOS,
                                         reader: CardReaderIOS<T>,
                                         postDump: (card: Card) -> Unit
    ) {
        feedback.statusConnecting(cardType = cardType)
        val lambda = lam@{ err: NSError? ->
            println("Connect callback")
            println("err = $err")
            if (err != null) {
                feedback.connectionError(err = err)
                return@lam
            }

            feedback.statusReading(cardType = cardType)

            GlobalScope.launch {
                println("reader async")
                try {
                    val card = reader.dump(tag = tag, feedback = feedback)
                    if (!card.isPartialRead) {
                        feedback.updateProgressBar(progress = 1, max = 1)
                    }
                    session.invalidateSession()
                    postDump(card)
                } catch (e: Exception) {
                    session.invalidateSessionWithErrorMessage(errorMessage = e.toString())
                }
            }
        }
        lambda.freeze()
        println("Connecting")
        session.connectToTag(tag, completionHandler = lambda)
        println("Connecting called")
    }

    @Throws(Throwable::class)
    fun readTag(session: NFCTagReaderSession,
                tags: List<NFCTagProtocol>,
                feedback: TagReaderFeedbackIOS,
                postDump: (card: Card) -> Unit) = logAndSwiftWrap("CardReaderDispatcher", "Failed to dump") {
        val tag = tags.firstOrNull()
        if (tag == null) {
            println ("No tags found")
            return@logAndSwiftWrap
        }
        println ("Found tag $tag")
        when (tag) {
            is NFCMiFareTagProtocol ->
                when (tag.mifareFamily) {
                    NFCMiFareDESFire -> readTagType(session = session, tag = tag, cardType = CardType.MifareDesfire, feedback = feedback, reader = DesfireCardReaderIOS, postDump = postDump)
                    NFCMiFareUltralight -> readTagType(session = session, tag = tag, cardType = CardType.MifareUltralight, feedback = feedback, reader = UltralightCardReaderIOS, postDump = postDump)
                    NFCMiFarePlus -> readTagType(session = session, tag = tag, cardType = CardType.MifarePlus, feedback = feedback, reader = PlusCardReaderIOS, postDump = postDump)
                    else -> session.invalidateSessionWithErrorMessage(errorMessage = Localizer.localizeString(R.string.ios_unknown_mifare, "$tag"))
                }
            is NFCISO7816TagProtocol ->
                readTagType(session = session, tag = tag, cardType = CardType.ISO7816, feedback = feedback, reader = ISO7816CardReaderIOS, postDump = postDump)

            is NFCFeliCaTagProtocol ->
                readTagType(session = session, tag = tag, cardType = CardType.FeliCa, feedback = feedback, reader = FelicaCardReaderIOS, postDump = postDump)

            is NFCISO15693TagProtocol ->
                readTagType(session = session, tag = tag, cardType = CardType.Vicinity, feedback = feedback, reader = VicinityReaderIOS, postDump = postDump)

            else ->
                session.invalidateSessionWithErrorMessage(errorMessage =
                Localizer.localizeString(R.string.ios_unknown_tag, "$tag"))
        }
    }
}

class CardReaderFeedbackIOS: TagReaderFeedbackIOS {
    private val session: AtomicReference<NFCReaderSession?> = AtomicReference(null)

    fun bindSession(sessionIn: NFCReaderSession) {
        session.value = sessionIn
    }

    override fun updateStatusText(msg: String) {
        this.msg.value = msg
        refresh()
    }
    
    override fun updateProgressBar(progress: Int, max: Int) {
        this.cur.value = progress
        this.max.value = max
        refresh()
    }
    
    override fun showCardType(cardInfo: CardInfo?) {
    }
    
    private val msg: AtomicReference<String> = AtomicReference("")
    private var cur = AtomicInt(0)
    private var max = AtomicInt(1)

    init {
        msg.value = Localizer.localizeString(R.string.ios_nfcreader_tap)
        freeze()
    }
    
    private fun refresh() {
        session.value?.alertMessage = "${msg.value} ${cur.value * 100 / max.value} %"
    }
    
    override fun statusConnecting(cardType: CardType) {
        updateStatusText(msg=Localizer.localizeString(R.string.ios_nfcreader_connecting,
                cardType.toString()))
    }

    override fun statusReading(cardType: CardType) {
        updateStatusText(msg=Localizer.localizeString(R.string.ios_nfcreader_reading, cardType.toString()))
    }
    
    override fun connectionError(err: NSError) {
        session.value?.invalidateSessionWithErrorMessage(errorMessage=
			       Localizer.localizeString(R.string.ios_nfcreader_connection_error, "$err"))
    }
}

class CardReaderDelegateIOS(private val postDump: (card: Card) -> Unit): NFCTagReaderSessionDelegateProtocol, NSObject() {
    private val feedback = CardReaderFeedbackIOS()
    init {
        freeze()
    }
    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) {
        println("NFC Session became active")
    }
    
    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        println("NFC Session end $didInvalidateWithError")
    }

    override fun tagReaderSession(session: NFCTagReaderSession, didDetectTags: List<*>) {
        CardReaderDispatcher.readTag(session=session, tags=didDetectTags.filterIsInstance<NFCTagProtocol>(),
                feedback=feedback, postDump=postDump)
    }

    fun bindSession(session: NFCReaderSession) {
        feedback.bindSession(session)
    }
}

@Suppress("unused") // Used from Swift
object CardReaderSessionIOS {
    fun readTag(postDump: (card: Card) -> Unit) {
        print ("Reading available: ${NFCTagReaderSession.readingAvailable}")
        val delegate = CardReaderDelegateIOS(postDump=postDump)
        val session = NFCTagReaderSession(
                pollingOption=NFCPollingISO14443 or NFCPollingISO15693 or NFCPollingISO18092,
                delegate=delegate, queue = null)
        delegate.bindSession(session=session)
        session.alertMessage = Localizer.localizeString(R.string.ios_nfcreader_tap)
        session.beginSession()
    }
}
