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
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import android.widget.RelativeLayout;
import android.widget.TextView;
import au.id.micolous.metrodroid.ui.HeaderListItem;
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

        private static void adjustListView(View view, ListItem li) {
            Spanned mText1 = li.getText1() != null ? li.getText1().getSpanned() : null;
            Spanned mText2 = li.getText2() != null ? li.getText2().getSpanned() : null;
            boolean text1Empty = mText1 == null || mText1.toString().isEmpty();
            boolean text2Empty = mText2 == null || mText2.toString().isEmpty();
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);
            if (text1Empty && text2Empty) {
                text1.setVisibility(View.GONE);
                text2.setVisibility(View.GONE);
                return;
            }
            if (text1Empty) {
                text1.setVisibility(View.GONE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) text2.getLayoutParams();
                params.addRule(RelativeLayout.CENTER_VERTICAL|RelativeLayout.CENTER_IN_PARENT,
                        text2.getId());
                text2.setText(mText2);
                text2.setTextIsSelectable(true);
                text2.setVisibility(View.VISIBLE);
            } else if (text2Empty) {
                text1.setText(mText1);
                text1.setVisibility(View.VISIBLE);
                text2.setVisibility(View.GONE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) text1.getLayoutParams();
                params.addRule(RelativeLayout.CENTER_VERTICAL|RelativeLayout.CENTER_IN_PARENT,
                        text1.getId());
            } else {
                text1.setText(mText1);
                text1.setVisibility(View.VISIBLE);
                text2.setText(mText2);
                text2.setVisibility(View.VISIBLE);
                text2.setTextIsSelectable(true);
            }
        }

        private static View getListView(ListItem li, LayoutInflater inflater, ViewGroup root, boolean attachToRoot) {
            View view = inflater.inflate(android.R.layout.simple_list_item_2, root, attachToRoot);
            adjustListView(view, li);
            return view;
        }

        private static View getHeaderListView(ListItem li, LayoutInflater inflater, ViewGroup root, boolean attachToRoot) {
            View view = inflater.inflate(R.layout.list_header, root, attachToRoot);

            ((TextView) view.findViewById(android.R.id.text1)).setText(li.getText1().getSpanned());
            return view;
        }


        @Override
        public View createNodeView(TreeNode node, Pair<ListItem, Integer> itemPair) {
            ListItem item = itemPair.first;
            int level = itemPair.second;
            View view;
            if (item instanceof HeaderListItem)
                view = getHeaderListView(item, LayoutInflater.from(context), null, false);
            else
                view = getListView(item, LayoutInflater.from(context), null, false);
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
