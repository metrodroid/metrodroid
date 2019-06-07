/*
 * CardInfoFragment.kt
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.util.Preferences
import com.unnamed.b.atv.model.TreeNode

import au.id.micolous.metrodroid.activity.CardInfoActivity
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.UriListItem

class CardInfoFragment : TreeListFragment() {

    private var mTransitData: TransitData? = null

    override val items: List<ListItem>
        get() = TransitData.mergeInfo(mTransitData!!).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTransitData = arguments?.getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA)
    }

    override fun onClick(node: TreeNode, value: Any) {
        var value = value
        if (value is Pair<*, *>)
            value = value.first
        if (value is UriListItem) {
            val uri = Uri.parse(value.uri)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
