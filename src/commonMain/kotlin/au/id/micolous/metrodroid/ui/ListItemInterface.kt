package au.id.micolous.metrodroid.ui

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelable
import kotlinx.serialization.Serializable

@Serializable
sealed class ListItemInterface: Parcelable {
    abstract val text1: FormattedString?
    abstract val text2: FormattedString?
}
