package au.id.micolous.metrodroid.transit.tampere

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

@Parcelize
class TampereSubscription(private val mA: ImmutableByteArray? = null,
                          private val mB: ImmutableByteArray? = null,
                          private val mC: ImmutableByteArray? = null,
                          private val mStart: Int? = null,
                          private val mEnd: Int? = null,
                          private val mType: Int
) : Subscription() {

    override val validFrom: Timestamp?
        get() = mStart?.let { TampereTransitData.parseDaystamp(it) }

    override val validTo: Timestamp?
        get() = mEnd?.let { TampereTransitData.parseDaystamp(it) }

    override val subscriptionName: String?
        get() = "Subscription ($mType)"

    override fun getRawFields(level: TransitData.RawLevel): List<ListItemInterface> =
            super.getRawFields(level).orEmpty() +
                listOf(ListItem("A", mA?.toHexString()),
                        ListItem("B", mB?.toHexString()),
                        ListItem("C", mC?.toHexString())) +
                if (level == TransitData.RawLevel.ALL) listOf(
                        ListItem("Start", mStart?.hexString),
                        ListItem("End", mEnd?.hexString),
                        ListItem("Type", mType.hexString),
                ) else emptyList()
}
