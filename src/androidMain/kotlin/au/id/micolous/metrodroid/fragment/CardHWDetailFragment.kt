/*
 * CardHWDetailActivity.kt
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity
import au.id.micolous.metrodroid.ui.ListItemInterface
import com.unnamed.b.atv.model.TreeNode

class CardHWDetailFragment : TreeListFragment() {
    override val items: List<ListItemInterface>
        get() = CardSerializer.fromPersist(requireArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD)!!).manufacturingInfo.orEmpty()

    override fun onClick(node: TreeNode, value: Any) {}
}
