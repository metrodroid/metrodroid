/*
 * TabPagerAdapter.java
 *
 * Copyright (C) 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.ui

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.app.Fragment
import android.app.FragmentTransaction
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.View

import au.id.micolous.farebot.R

class TabPagerAdapter(private val mActivity: Activity, private val mViewPager: ViewPager) : PagerAdapter(), ActionBar.TabListener, ViewPager.OnPageChangeListener {
    private val mActionBar: ActionBar? = mActivity.actionBar
    private val mTabs = ArrayList<TabInfo>()
    private var mCurTransaction: FragmentTransaction? = null

    init {
        mViewPager.adapter = this
        mViewPager.setOnPageChangeListener(this)
    }

    fun addTab(tab: ActionBar.Tab, clss: Class<*>, args: Bundle) {
        val info = TabInfo(clss, args)
        tab.tag = info
        tab.setTabListener(this)
        mTabs.add(info)
        mActionBar!!.addTab(tab)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = mTabs.size

    override fun startUpdate(view: View) {}

    @SuppressLint("CommitTransaction")
    override fun instantiateItem(view: View, position: Int): Any {
        val info = mTabs[position]

        if (mCurTransaction == null) {
            mCurTransaction = mActivity.fragmentManager.beginTransaction()
        }

        val fragment = Fragment.instantiate(mActivity, info.mClass.name, info.mArgs)
        mCurTransaction!!.add(R.id.pager, fragment)
        return fragment
    }

    @SuppressLint("CommitTransaction")
    override fun destroyItem(view: View, i: Int, `object`: Any) {
        if (mCurTransaction == null) {
            mCurTransaction = mActivity.fragmentManager.beginTransaction()
        }
        mCurTransaction!!.hide(`object` as Fragment)
    }

    override fun finishUpdate(view: View) {
        if (mCurTransaction != null) {
            mCurTransaction?.commitAllowingStateLoss()
            mCurTransaction = null
            mActivity.fragmentManager.executePendingTransactions()
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return (`object` as Fragment).view === view
    }

    override fun saveState(): Parcelable? = null

    override fun restoreState(parcelable: Parcelable?, classLoader: ClassLoader?) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        mActionBar!!.setSelectedNavigationItem(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onTabSelected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {
        val tag = tab.tag
        for (i in mTabs.indices) {
            if (mTabs[i] == tag) {
                mViewPager.currentItem = i
            }
        }
    }

    override fun onTabUnselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {}

    override fun onTabReselected(tab: ActionBar.Tab, fragmentTransaction: FragmentTransaction) {}

    private class TabInfo internal constructor(internal val mClass: Class<*>, internal val mArgs: Bundle)
}
