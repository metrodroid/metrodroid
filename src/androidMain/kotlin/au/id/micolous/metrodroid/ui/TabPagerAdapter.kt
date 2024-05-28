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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TabPagerAdapter(
    mActivity: FragmentActivity,
    mViewPager: ViewPager2) :
    FragmentStateAdapter(mActivity) {
    private val mTabs = ArrayList<TabInfo>()

    init {
        mViewPager.adapter = this
        addMediator(mActivity, mViewPager)
    }

    fun addTab(nameResource: Int, creator: () -> Fragment, args: Bundle?) {
        val info = TabInfo(creator, args, Localizer.localizeString(nameResource))
        mTabs.add(info)
        notifyItemInserted(mTabs.size - 1)
    }

    override fun getItemCount(): Int = mTabs.size

    override fun createFragment(position: Int): Fragment {
        val info = mTabs[position]
        val frag = info.mCreator()
        frag.arguments = info.mArgs
        return frag
    }

    private class TabInfo(val mCreator: () -> Fragment,
                          val mArgs: Bundle?, val mName: String)

    fun addMediator(activity: FragmentActivity, viewPager: ViewPager2) {
        val tabLayout = activity.findViewById<TabLayout>(R.id.tabs) ?: return

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = mTabs[position].mName
        }.attach()
    }
}
