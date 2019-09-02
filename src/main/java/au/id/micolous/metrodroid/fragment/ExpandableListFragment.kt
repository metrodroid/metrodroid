/*
 * ExpandableListFragment.java
 * From: http://stackoverflow.com/a/6193434
 * Also from: https://gist.github.com/mosabua/1316903
 *
 * This class is a whole barrel of fun from a licensing perspective.
 *
 * StackOverflow code posted prior to 2016-02-01 is licensed under
 * CC-By-SA. Source: http://meta.stackexchange.com/q/271080
 *
 * This code was posted 2011-05-31.
 *
 * However, this is based on ListFragment and ExpandableListActivity, which is are core Android
 * components licensed under the Apache Software License v2:
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ExpandableListActivity.java
 *
 * There are additional versions of this code with modifications floating around which are generally
 * also licensed under the Apache Software License v2.
 *
 * As a result, I believe the work *should* be licensed under the Apache Software License v2, which
 * permits it to be included in GPLv3 projects.
 *
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package au.id.micolous.metrodroid.fragment

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView

import android.view.ViewGroup.LayoutParams.MATCH_PARENT

open class ExpandableListFragment : Fragment(), OnCreateContextMenuListener, ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupCollapseListener, ExpandableListView.OnGroupExpandListener {

    private val mHandler = Handler()
    private val mOnClickListener = AdapterView.OnItemClickListener { parent, v, position, id -> onListItemClick(parent as ListView, v, position, id) }
    private val mOnChildClickListener = ExpandableListView.OnChildClickListener { parent, v, groupPosition, childPosition, id -> this.onListChildClick(parent, v, groupPosition, childPosition, id) }
    /**
     * Provide the cursor for the list view.
     */
    // The list was hidden, and previously didn't have an
    // adapter.  It is now time to show it.
    var listAdapter: ExpandableListAdapter? = null
        set(adapter) {
            val hadAdapter = listAdapter != null
            field = adapter
            if (mList != null) {
                mList!!.setAdapter(adapter)
                if (!mListShown && !hadAdapter) {
                    setListShown(true, view!!.windowToken != null)
                }
            }
        }
    private var mList: ExpandableListView? = null
    private val mRequestFocus = Runnable { mList!!.focusableViewAvailable(mList) }
    private var mEmptyView: View? = null
    private var mStandardEmptyView: TextView? = null
    private var mListContainer: View? = null
    private var mSetEmptyText: Boolean = false
    private var mListShown: Boolean = false
    private var mFinishedStart = false

    internal val listView: View?
        get() {
            ensureList()
            return mList
        }

    val selectedPosition: Long
        get() {
            ensureList()
            return mList!!.selectedPosition
        }

    val selectedId: Long
        get() {
            ensureList()
            return mList!!.selectedId
        }

    val expandableListView: ExpandableListView?
        get() {
            ensureList()
            return mList
        }

    /**
     * Provide default implementation to return a simple list view.  Subclasses
     * can override to replace with their own layout.  If doing so, the
     * returned view hierarchy *must* have a ListView whose id
     * is [android.R.id.list] and can optionally
     * have a sibling view id [android.R.id.empty]
     * that is to be shown when the list is empty.
     *
     *
     *
     * If you are overriding this method with your own custom content,
     * consider including the standard layout [android.R.layout.list_content]
     * in your layout file, so that you continue to retain all of the standard
     * behavior of ListFragment.  In particular, this is currently the only
     * way to have the built-in indeterminant progress state be shown.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = FrameLayout(activity!!)

        val tv = TextView(activity)
        tv.id = INTERNAL_EMPTY_ID
        tv.gravity = Gravity.CENTER
        root.addView(tv, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val lv = ExpandableListView(activity)
        lv.id = android.R.id.list
        lv.isDrawSelectorOnTop = false
        root.addView(lv, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        root.layoutParams = android.widget.AbsListView.LayoutParams(MATCH_PARENT, MATCH_PARENT)

        return root
    }

    /**
     * Attach to list view once Fragment is ready to run.
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ensureList()
    }

    /**
     * Detach from list view.
     */
    override fun onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus)
        mList = null
        super.onDestroyView()
    }

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call
     * getListView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param l        The ListView where the click happened
     * @param v        The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     */
    fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {}

    open fun onListChildClick(parent: ExpandableListView, v: View, groupPosition: Int, childPosition: Int, id: Long): Boolean {
        return false
    }

    /**
     * Set the currently selected list item to the specified
     * position with the adapter's data
     *
     * @param position
     */
    fun setSelection(position: Int) {
        ensureList()
        mList!!.setSelection(position)
    }

    /**
     * The default content for a ListFragment has a TextView that can
     * be shown when the list is empty.  If you would like to have it
     * shown, call this method to supply the text it should use.
     */
    fun setEmptyText(text: CharSequence) {
        ensureList()
        if (mStandardEmptyView == null) {
            throw IllegalStateException("Can't be used with a custom content view")
        }
        mStandardEmptyView!!.text = text
        if (!mSetEmptyText) {
            mList!!.emptyView = mStandardEmptyView
            mSetEmptyText = true
        }
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     *
     *
     * Applications do not normally need to use this themselves.  The default
     * behavior of ListFragment is to start with the list not being shown, only
     * showing it once an adapter is given with [.setListAdapter].
     * If the list at that point had not been shown, when it does get shown
     * it will be do without the user ever seeing the hidden state.
     *
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    fun setListShown(shown: Boolean) {
        setListShown(shown, true)
    }

    /**
     * Like [.setListShown], but no animation is used when
     * transitioning from the previous state.
     */
    fun setListShownNoAnimation(shown: Boolean) {
        setListShown(shown, false)
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown   If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private fun setListShown(shown: Boolean, animate: Boolean) {
        ensureList()
        if (mListShown == shown) {
            return
        }
        mListShown = shown
        if (shown) {
            if (animate) {
                mListContainer!!.startAnimation(AnimationUtils.loadAnimation(
                        activity, android.R.anim.fade_in))
            }
            mListContainer!!.visibility = View.VISIBLE
        } else {
            if (animate) {
                mListContainer!!.startAnimation(AnimationUtils.loadAnimation(
                        activity, android.R.anim.fade_out))
            }
            mListContainer!!.visibility = View.GONE
        }
    }

    /**
     * Get the ListAdapter associated with this activity's ListView.
     */
    fun getExpandableListAdapter(): ExpandableListAdapter? {
        return listAdapter
    }

    private fun ensureList() {
        if (mList != null) {
            return
        }
        val root = view ?: throw IllegalStateException("Content view not yet created")
        if (root is ExpandableListView) {
            mList = root
        } else {
            mStandardEmptyView = root.findViewById(INTERNAL_EMPTY_ID)
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty)
            }
            mListContainer = root.findViewById(android.R.id.list)
            val rawListView = mListContainer
            if (rawListView !is ExpandableListView) {
                if (rawListView == null) {
                    throw RuntimeException("Your content must have a ExpandableListView whose id attribute is 'android.R.id.list'")
                }
                throw RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' that is not a ExpandableListView class")
            }
            mList = rawListView
            if (mEmptyView != null) {
                mList!!.emptyView = mEmptyView
            }
        }
        mListShown = true
        mList!!.setOnItemClickListener(mOnClickListener)
        mList!!.setOnChildClickListener(mOnChildClickListener)
        if (listAdapter != null) {
            listAdapter = listAdapter
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            setListShown(false, false)
        }
        mHandler.post(mRequestFocus)
    }

    override fun onGroupExpand(arg0: Int) {
        // TODO Auto-generated method stub

    }

    override fun onGroupCollapse(arg0: Int) {
        // TODO Auto-generated method stub

    }

    override fun onChildClick(arg0: ExpandableListView, arg1: View, arg2: Int,
                              arg3: Int, arg4: Long): Boolean {
        // TODO Auto-generated method stub
        return false
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {}

    fun onContentChanged() {
        val emptyView = view!!.findViewById<View>(android.R.id.empty)
        mList = view!!.findViewById(android.R.id.list)
        if (mList == null) {
            throw RuntimeException("Your content must have a ExpandableListView whose id attribute is " + "'android.R.id.list'")
        }
        if (emptyView != null) {
            mList!!.emptyView = emptyView
        }
        mList!!.setOnChildClickListener(this)
        mList!!.setOnGroupExpandListener(this)
        mList!!.setOnGroupCollapseListener(this)

        if (mFinishedStart) {
            listAdapter = listAdapter
        }
        mFinishedStart = true
    }

    companion object {

        private const val INTERNAL_EMPTY_ID = 0x00ff0001
    }
}
