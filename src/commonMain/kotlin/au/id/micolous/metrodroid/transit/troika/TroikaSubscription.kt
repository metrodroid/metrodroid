package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription

@Parcelize
internal class TroikaSubscription(private val mExpiryDate: Timestamp?, override val validFrom: Timestamp?,
                                  private val mValidityEnd: Timestamp?, override val remainingTripCount: Int?,
                                  private val mValidityLengthMinutes: Int?,
                                  private val mTicketType: Int) : Subscription() {

    override val validTo: Timestamp?
        get() {
            val candidate = mValidityEnd ?: mExpiryDate
            if (mValidityLengthMinutes?:0 >= 60 * 24)
                return candidate?.toDaystamp()
            return candidate
        }

    override val subscriptionName: String?
        get() = TroikaBlock.getHeader(mTicketType)

    override fun getAgencyName(isShort: Boolean) =
            Localizer.localizeFormatted(R.string.card_name_troika)
}
