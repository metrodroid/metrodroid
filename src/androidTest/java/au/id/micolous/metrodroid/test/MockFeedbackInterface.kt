package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.transit.CardInfo

class MockFeedbackInterface : TagReaderFeedbackInterface {
    val statusTexts = ArrayList<String>()
    val progressBars = ArrayList<Pair<Int, Int>>()
    val showCardTypes = ArrayList<CardInfo>()

    override fun updateStatusText(msg: String) {
        statusTexts.add(msg)
    }

    override fun updateProgressBar(progress: Int, max: Int) {
        progressBars.add(Pair(progress, max))
    }

    override fun showCardType(cardInfo: CardInfo?) {
        if (cardInfo == null) {
            return
        }
        showCardTypes.add(cardInfo)
    }
}