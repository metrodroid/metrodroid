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
internal class TroikaPurseE5(val rawData: ImmutableByteArray) : TroikaBlock(
        rawData,
        mExpiryDate = convertDate2019(rawData.getBitsFromBuffer(61, 13)),
        // 10 bits Ticket Type 2
        // 84-107: lastRefillTime
        // 107-117: refillCounter
        // 117-128: unknown (B)
        mLastValidationTime = convertDateTime2019(0, rawData.getBitsFromBuffer(128, 23)),
        mTransfers = listOf(rawData.getBitsFromBuffer(151, 7),
                            rawData.getBitsFromBuffer(158, 7)),
        // 2 bits unknown
        // 19 bits balance
        mLastValidator = rawData.getBitsFromBuffer(186, 16),
        // 202-216: unknown (D)
        // 216-223: tripsOnPurse
        // 224: unknown (E)
        mLastTransportLeadingCode = null,
        mLastTransportLongCode = null,
        mFareDesc = null,
        mCheckSum = rawData.getHexString(28, 4)
) {

    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private val mBalance get() = rawData.getBitsFromBuffer(167, 19)

    override val balance: TransitBalance?
        get() = TransitBalanceStored(
                TransitCurrency.RUB(mBalance),
                Localizer.localizeString(R.string.card_name_troika),
                mExpiryDate)

    override val subscription: Subscription?
        get() = null

    override val lastRefillTime get() = convertDateTime2019(0, rawData.getBitsFromBuffer(84, 23))

    override val debug get() = super.debug + listOf(
            ListItem("Ticket Type 2", rawData.getBitsFromBuffer(74, 10).hexString),
            ListItem("B", rawData.getBitsFromBuffer(117, 11).hexString),
            ListItem("D", rawData.getBitsFromBuffer(202, 14).hexString),
            ListItem("E", rawData.getBitsFromBuffer(223, 1).hexString)
    )

    override val info: List<ListItem>
        get() = super.info.orEmpty() + listOf(
                ListItem(R.string.refill_counter, refillCounter.toString()),
                ListItem(R.string.purse_ride_counter, tripsOnPurse.toString())
        )

    private val refillCounter get() = rawData.getBitsFromBuffer(107, 10)
    private val tripsOnPurse get() = rawData.getBitsFromBuffer(216, 7)
    val checksum get() = rawData.getHexString(28, 4)
}
