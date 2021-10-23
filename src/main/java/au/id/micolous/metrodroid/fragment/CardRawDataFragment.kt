package au.id.micolous.metrodroid.fragment

import au.id.micolous.metrodroid.serializers.CardSerializer
import com.unnamed.b.atv.model.TreeNode

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity
import au.id.micolous.metrodroid.ui.ListItemInterface

class CardRawDataFragment : TreeListFragment() {
    override val items: List<ListItemInterface>
        get() = CardSerializer.fromPersist(requireArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD)!!).rawData.orEmpty()

    override fun onClick(node: TreeNode, value: Any) {}
}
