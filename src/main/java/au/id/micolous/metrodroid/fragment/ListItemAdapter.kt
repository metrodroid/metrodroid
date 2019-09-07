/*
 * ListItemAdapter.kt
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.UriListItem

import au.id.micolous.farebot.R

class ListItemAdapter(context: Context, items: List<ListItem>) : ArrayAdapter<ListItem>(context, 0, items) {

    override fun getItemViewType(position: Int): Int = if (getItem(position) is HeaderListItem) 0 else 1

    override fun getViewTypeCount(): Int = 2

    override fun getView(position: Int, convertViewReuse: View?, parent: ViewGroup): View {
        val convertView = convertViewReuse ?: (context as Activity).layoutInflater.inflate(
                if (getItemViewType(position) == 0) R.layout.list_header else android.R.layout.simple_list_item_2,
                parent, false)

        val item = getItem(position)
        val text1: TextView? = convertView.findViewById(android.R.id.text1)
        val text2: TextView? = convertView.findViewById(android.R.id.text2)

        text1?.text = item?.text1?.spanned
        if (item !is HeaderListItem) {
            text2?.text = item?.text2?.spanned
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            text1?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            text2?.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }

        return convertView
    }

    override fun isEnabled(position: Int): Boolean = getItem(position) is UriListItem
}
