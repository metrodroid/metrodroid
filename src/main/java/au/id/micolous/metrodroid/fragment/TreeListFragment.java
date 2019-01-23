/*
 * CardHWDetailActivity.java
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

package au.id.micolous.metrodroid.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;

public abstract class TreeListFragment extends Fragment implements TreeNode.TreeNodeClickListener {
    private AndroidTreeView tView;

    public static class ListItemHolder extends TreeNode.BaseNodeViewHolder<Pair<ListItem, Integer>> {
        private ImageView mArrowView;

        public ListItemHolder(Context context) {
            super(context);
        }

        @Override
        public View createNodeView(TreeNode node, Pair<ListItem, Integer> itemPair) {
            ListItem item = itemPair.first;
            int level = itemPair.second;
            View view = item.getView(LayoutInflater.from(context), null, false);
            float pxPerLevel = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10.0f,
                    context.getResources().getDisplayMetrics()
            );
            float addPadding = level * pxPerLevel;
            mArrowView = view.findViewById(R.id.arrow_img);
            if (!(item instanceof ListItemRecursive)) {
                float pxLeaf = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        5.0f,
                        context.getResources().getDisplayMetrics()
                );
                addPadding += pxLeaf;
            } else if (((ListItemRecursive) item).getSubTree() == null) {
                if (mArrowView != null)
                    mArrowView.setVisibility(View.INVISIBLE);
            }
            if (Build.VERSION.SDK_INT >= 17) {
                view.setPaddingRelative(view.getPaddingStart() + (int) addPadding,
                        view.getPaddingTop(), view.getPaddingEnd(), view.getPaddingBottom());
            } else {
                view.setPadding(view.getPaddingLeft() + (int) addPadding,
                        view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
            }
            return view;
        }

        @Override
        public void toggle(boolean active) {
            if (mArrowView == null)
                return;
            TypedArray a = context.obtainStyledAttributes(new int[]{
                    active ? R.attr.DrawableOpenIndicator : R.attr.DrawableClosedIndicator});

            mArrowView.setImageResource(a.getResourceId(0,
                    active ? R.drawable.expander_open_holo_dark
                            : R.drawable.expander_close_holo_dark));
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity activity = getActivity();

        TreeNode root = TreeNode.root();

        List<? extends ListItem> items = getItems();

        for (ListItem item : items)
            root.addChild(getTreeNode(item, 0));

        tView = new AndroidTreeView(activity, root);
        tView.setDefaultAnimation(true);
        tView.setDefaultViewHolder(ListItemHolder.class);
        tView.setDefaultNodeClickListener(this);

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                tView.restoreState(state);
            }
        }

        return tView.getView();
    }

    private static TreeNode getTreeNode(ListItem item, int level) {
        if (!(item instanceof ListItemRecursive))
            return new TreeNode(Pair.create(item, level));
        List <ListItem> subTree = ((ListItemRecursive) item).getSubTree();
        TreeNode root = new TreeNode(Pair.create(item, level));
        if (subTree == null)
            return root;
        for (ListItem subItem : subTree) {
            root.addChild(getTreeNode(subItem, level + 1));
        }
        return root;
    }

    protected abstract List<? extends ListItem> getItems();

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", tView.getSaveState());
    }
}
