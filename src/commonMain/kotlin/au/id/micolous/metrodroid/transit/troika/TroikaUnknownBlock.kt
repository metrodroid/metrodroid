package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Suppress("CanBeParameter")
@Parcelize
internal class TroikaUnknownBlock(private val rawData: ImmutableByteArray) : TroikaBlock(rawData) {
    override val info: List<ListItem>?
        get() = listOf(HeaderListItem(getHeader(mTicketType)),
                ListItem(R.string.troika_layout, mLayout.toString(16)))
}
