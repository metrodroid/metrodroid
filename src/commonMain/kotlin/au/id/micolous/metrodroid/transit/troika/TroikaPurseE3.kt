package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

// This is e-purse layout
@Parcelize
internal class TroikaPurseE3(val rawData: ImmutableByteArray) : TroikaBlock(
        rawData,
        mExpiryDate = convertDateTime1992(rawData.getBitsFromBuffer(61, 16), 0),
        // 41 bits zero
        mLastValidator = rawData.getBitsFromBuffer(128, 16),
        mLastValidationTime = convertDateTime2016(0, rawData.getBitsFromBuffer(144, 23)),
        // 4 bits zero
        mTransfers = listOf(rawData.getBitsFromBuffer(171, 7)),
        mLastTransportLeadingCode = rawData.getBitsFromBuffer(178, 2),
        mLastTransportLongCode = rawData.getBitsFromBuffer(180, 8),
        mFareDesc = when (rawData.getBitsFromBuffer(210, 2)) {
            1 -> Localizer.localizeString(R.string.troika_fare_single)
            2 -> Localizer.localizeString(R.string.troika_fare_90mins)
            else -> null
        },
        //12 bits zero
        mCheckSum = rawData.getHexString(28, 4)
) {

    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private val mBalance get() = rawData.getBitsFromBuffer(188, 22)

    override val balance: TransitBalance?
        get() = TransitBalanceStored(
                TransitCurrency.RUB(mBalance),
                Localizer.localizeString(R.string.card_name_troika),
                mExpiryDate)

    override val subscription: Subscription?
        get() = null

    override val debug get() = super.debug + listOf(
            ListItem("Ticket Type 2", rawData.getBitsFromBuffer(77, 10).hexString),

            // Always 0 so far
            ListItem("A1", rawData.getBitsFromBuffer(87, 9).hexString),
            ListItem("A2", "0x" + rawData.getHexString(12, 4)),
            ListItem("B", rawData.getBitsFromBuffer(167, 4).hexString),
            ListItem("C", rawData.getBitsFromBuffer(212, 10).hexString)
    )
}
