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

import androidx.fragment.app.Fragment
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Pair
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.TextListItem
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.view.AndroidTreeView

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive

@JvmSuppressWildcards(false)
abstract class TreeListFragment : Fragment(), TreeNode.TreeNodeClickListener {
    private var tView: AndroidTreeView? = null

    protected abstract val items: List<ListItem>

    class ListItemHolder(context: Context) : TreeNode.BaseNodeViewHolder<Pair<ListItem, Int>>(context) {
        private var mArrowView: ImageView? = null

        private fun adjustListView(view: View, li: ListItem) {
            val mText1 = li.text1?.spanned
            val mText2 = li.text2?.spanned
            val text1Empty = mText1?.toString().isNullOrEmpty()
            val text2Empty = mText2?.toString().isNullOrEmpty()
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)
            if (text1Empty)
                text1.visibility = View.GONE
            else {
                text1.text = mText1
                text1.visibility = View.VISIBLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    text1.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
            }
            if (text2Empty)
                text2.visibility = View.GONE
            else {
                text2.text = mText2
                text2.visibility = View.VISIBLE
                text2.setTextIsSelectable(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    text2.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
            }
            if (text1Empty != text2Empty) {
                val remaining = if (text1Empty) text2 else text1
                val params = remaining.layoutParams as RelativeLayout.LayoutParams
                params.addRule(RelativeLayout.CENTER_VERTICAL or RelativeLayout.CENTER_IN_PARENT,
                        remaining.id)
            }
        }

        private fun getListView(li: ListItem, inflater: LayoutInflater, root: ViewGroup?, attachToRoot: Boolean): View {
            val view = inflater.inflate(android.R.layout.simple_list_item_2, root, attachToRoot)
            adjustListView(view, li)
            return view
        }

        private fun decorateTextView(text: TextView, ctxt: Context,
                                     attr: Int, def: Int) {
            val textAppearenceRes: Int
            val ta = ctxt.obtainStyledAttributes(intArrayOf(attr))
            textAppearenceRes = ta.getResourceId(0, def)
            ta.recycle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                text.setTextAppearance(textAppearenceRes)
            else
                text.setTextAppearance(ctxt, textAppearenceRes)
        }

        private fun getTextListView(li: ListItem, inflater: LayoutInflater, root: ViewGroup?, attachToRoot: Boolean): View {
            val view = inflater.inflate(android.R.layout.simple_list_item_2, root, attachToRoot)
            adjustListView(view, li)
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            decorateTextView(text1, inflater.context, android.R.attr.textAppearanceMedium,
                    android.R.style.TextAppearance_Medium)
            return view
        }

        private fun getHeaderListL1View(li: HeaderListItem, inflater: LayoutInflater): View {
            val ctxt = inflater.context
            val rl = RelativeLayout(ctxt)
            val text = TextView(ctxt)
            decorateTextView(text, ctxt, android.R.attr.textAppearanceLarge,
                    android.R.style.TextAppearance_Large)
            text.text = li.text1!!.spanned
            text.gravity = Gravity.CENTER_HORIZONTAL
            val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.CENTER_HORIZONTAL
            text.layoutParams = lp
            rl.addView(text)
            return rl
        }

        private fun getHeaderListL2View(li: HeaderListItem, inflater: LayoutInflater, root: ViewGroup?, attachToRoot: Boolean): View {
            val view = inflater.inflate(R.layout.list_header, root, attachToRoot)

            view.findViewById<TextView>(android.R.id.text1).text = li.text1!!.spanned
            return view
        }

        private fun getHeaderListView(li: HeaderListItem, inflater: LayoutInflater, root: ViewGroup?, attachToRoot: Boolean): View {
            val level = li.headingLevel
            return if (level == 1) getHeaderListL1View(li, inflater) else getHeaderListL2View(li, inflater, root, attachToRoot)
        }

        private fun getRecursiveListView(li: ListItem, inflater: LayoutInflater, root: ViewGroup?, attachToRoot: Boolean): View {
            val view = inflater.inflate(R.layout.list_recursive, root, attachToRoot)
            adjustListView(view, li)
            return view
        }

        override fun createNodeView(node: TreeNode, itemPair: Pair<ListItem, Int>): View {
            val item = itemPair.first
            val level = itemPair.second
            val view: View = when (item) {
                is ListItemRecursive -> getRecursiveListView(item, LayoutInflater.from(context), null, false)
                is HeaderListItem -> getHeaderListView(item, LayoutInflater.from(context), null, false)
                is TextListItem -> getTextListView(item, LayoutInflater.from(context), null, false)
                else -> getListView(item, LayoutInflater.from(context), null, false)
            }
            val pxPerLevel = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10.0f,
                    context.resources.displayMetrics
            ).toInt().toFloat()
            var addPadding = level * pxPerLevel
            mArrowView = view.findViewById(R.id.arrow_img)
            if (item !is ListItemRecursive) {
                val pxLeaf = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        5.0f,
                        context.resources.displayMetrics
                ).toInt().toFloat()
                addPadding += pxLeaf
            } else if (item.subTree == null) {
                mArrowView?.visibility = View.INVISIBLE
            }
            if (Build.VERSION.SDK_INT >= 17) {
                view.setPaddingRelative(view.paddingStart + addPadding.toInt(),
                        view.paddingTop, view.paddingEnd, view.paddingBottom)
            } else {
                view.setPadding(view.paddingLeft + addPadding.toInt(),
                        view.paddingTop, view.paddingRight, view.paddingBottom)
            }
            return view
        }

        override fun toggle(active: Boolean) {
            if (mArrowView == null)
                return
            val a = context.obtainStyledAttributes(intArrayOf(if (active) R.attr.DrawableOpenIndicator else R.attr.DrawableClosedIndicator))

            mArrowView?.setImageResource(a.getResourceId(0,
                    if (active)
                        R.drawable.expander_open_holo_dark
                    else
                        R.drawable.expander_close_holo_dark))
            a.recycle()
            mArrowView?.contentDescription = Localizer.localizeString(
                if (active)
                    R.string.closed_arrow
                else
                    R.string.open_arrow
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = TreeNode.root()

        for (item in items)
            root.addChild(getTreeNode(item, 0))

        val tv = AndroidTreeView(activity, root)
        tView = tv
        tv.setDefaultAnimation(true)
        tv.setDefaultViewHolder(ListItemHolder::class.java)
        tv.setDefaultNodeClickListener(this)

        if (savedInstanceState != null) {
            val state = savedInstanceState.getString("tState")
            if (!TextUtils.isEmpty(state)) {
                tv.restoreState(state)
            }
        }

        return tv.view
    }

    private fun getTreeNode(item: ListItem, level: Int): TreeNode {
        if (item !is ListItemRecursive)
            return TreeNode(Pair.create(item, level))
        val root = TreeNode(Pair.create<ListItem, Int>(item, level))
        for (subItem in item.subTree.orEmpty()) {
            root.addChild(getTreeNode(subItem, level + 1))
        }
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tState", tView!!.saveState)
    }
}
