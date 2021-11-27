package au.id.micolous.metrodroid.card

import platform.Foundation.NSError

interface TagReaderFeedbackIOS : TagReaderFeedbackInterface {
    fun statusConnecting(cardType: CardType)
    fun statusReading(cardType: CardType)
    fun connectionError(err: NSError)
}