/*
 * TabPagerAdapter.kt
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
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import au.id.micolous.metrodroid.multi.Localizer

class TabPagerAdapter(private val mActivity: AppCompatActivity, private val mViewPager: ViewPager) : FragmentPagerAdapter(mActivity.supportFragmentManager) {
    private val mTabs = ArrayList<TabInfo>()
    private var mCurTransaction: FragmentTransaction? = null

    init {
        mViewPager.adapter = this
    }

    fun addTab(nameResource: Int, clss: Class<*>, args: Bundle?) {
        val info = TabInfo(clss, args, Localizer.localizeString(nameResource))
        mTabs.add(info)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = mTabs.size

    override fun startUpdate(view: View) {}

    override fun getItem(p0: Int): Fragment {
        val info = mTabs[p0]
        return Fragment.instantiate(mActivity, info.mClass.name, info.mArgs)
    }

    override fun getPageTitle(p0: Int): CharSequence = mTabs[p0].mName

    @SuppressLint("CommitTransaction")
    override fun destroyItem(view: View, i: Int, obj: Any) {
        if (mCurTransaction == null) {
            mCurTransaction = mActivity.supportFragmentManager.beginTransaction()
        }
        mCurTransaction!!.hide(obj as Fragment)
    }

    override fun finishUpdate(view: View) {
        if (mCurTransaction != null) {
            mCurTransaction?.commitAllowingStateLoss()
            mCurTransaction = null
            mActivity.supportFragmentManager.executePendingTransactions()
        }
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return (obj as Fragment).view === view
    }

    override fun saveState(): Parcelable? = null

    override fun restoreState(parcelable: Parcelable?, classLoader: ClassLoader?) {}

    private class TabInfo(val mClass: Class<*>, val mArgs: Bundle?, val mName: String)
}
