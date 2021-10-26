package au.id.micolous.metrodroid.ui

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelable

interface ListItemInterface: Parcelable {
    val text1: FormattedString?
    val text2: FormattedString?
}
