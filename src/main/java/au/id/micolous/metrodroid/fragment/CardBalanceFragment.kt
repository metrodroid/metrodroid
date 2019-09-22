/*
 * CardBalanceFragment.kt
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.ListFragment
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.activity.CardInfoActivity
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem

class CardBalanceFragment : ListFragment() {
    private var mTransitData: TransitData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTransitData = arguments!!.getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val combined = ArrayList<Any>()
        val balances = mTransitData!!.balances
        if (balances != null)
            combined.addAll(balances)
        val subscriptions = mTransitData!!.subscriptions
        if (subscriptions != null)
            combined.addAll(subscriptions)
        listAdapter = BalancesAdapter(activity!!, combined)
    }

    private inner class BalancesAdapter internal constructor(context: Context, balances: List<Any>) : ArrayAdapter<Any>(context, 0, balances) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)

            if (item == null) {
                // https://github.com/micolous/metrodroid/issues/28
                Log.w(TAG, "null balance received -- this is an error")
                return getErrorView(convertView, parent, "null")
            }

            return (item as? TransitBalance)?.let { getBalanceView(convertView, parent, it) }
                    ?: ((item as? Subscription)?.let { getSubscriptionView(convertView, parent, it) }
                            ?: getErrorView(convertView, parent, item.javaClass.simpleName))

        }

        private fun getErrorView(convertView: View?, parent: ViewGroup, err: String): View {
            var view = convertView
            if (view == null || view.tag !== TAG_ERROR_VIEW) {
                view = activity?.layoutInflater?.inflate(R.layout.balance_item, parent, false)
                view!!.tag = TAG_ERROR_VIEW
            }

            (view.findViewById<View>(R.id.balance) as TextView).text = err
            return view
        }

        fun getSubscriptionView(convertView: View?, parent: ViewGroup, subscription: Subscription): View {
            var view = convertView
            if (view == null || view.tag !== TAG_SUBSCRIPTION_VIEW) {
                view = activity?.layoutInflater?.inflate(R.layout.subscription_item, parent, false)
                view!!.tag = TAG_SUBSCRIPTION_VIEW
            }

            val validView = view.findViewById<TextView>(R.id.valid)
            val validity = subscription.formatValidity()
            if (validity != null) {
                validView.text = validity.spanned
                validView.visibility = View.VISIBLE
            } else
                validView.visibility = View.GONE

            val tripsView = view.findViewById<TextView>(R.id.trips)
            val daysView = view.findViewById<TextView>(R.id.days)
            val remainingTrips = subscription.formatRemainingTrips()

            if (remainingTrips != null) {
                tripsView.text = remainingTrips
                tripsView.visibility = View.VISIBLE
            } else {
                tripsView.visibility = View.GONE
            }

            val remainingDays = subscription.remainingDayCount
            if (remainingDays != null) {
                daysView.text = Localizer.localizePlural(R.plurals.remaining_day_count,
                        remainingDays, remainingDays)
                daysView.visibility = View.VISIBLE
            } else {
                daysView.visibility = View.GONE
            }

            val companyView = view.findViewById<TextView>(R.id.company)
            val agencyName = subscription.getAgencyName(true)
            if (agencyName != null) {
                companyView.text = agencyName.spanned
                companyView.visibility = View.VISIBLE
            } else {
                companyView.text = ""
                companyView.visibility = View.GONE
            }
            val nameView = view.findViewById<TextView>(R.id.name)
            val name = subscription.subscriptionName
            if (name != null) {
                nameView.text = name
                nameView.visibility = View.VISIBLE
            } else {
                nameView.visibility = View.GONE
            }

            // TODO: Replace this with structured data.
            val usedView = view.findViewById<TextView>(R.id.used)
            if (subscription.subscriptionState === Subscription.SubscriptionState.UNKNOWN) {
                usedView.visibility = View.GONE
            } else {
                usedView.setText(subscription.subscriptionState.descriptionRes)
                usedView.visibility = View.VISIBLE
            }

            val paxLayout = view.findViewById<LinearLayout>(R.id.pax_layout)
            val paxIcon = view.findViewById<ImageView>(R.id.pax_icon)
            val paxTextView = view.findViewById<TextView>(R.id.pax_text_view)
            val pax = subscription.passengerCount

            if (pax >= 1) {
                paxTextView.text = "$pax"
                paxIcon.contentDescription = Localizer.localizePlural(R.plurals.passengers, pax)

                paxIcon.setImageDrawable(AppCompatResources.getDrawable(context,
                        if (pax == 1) R.drawable.material_ic_person_24dp else R.drawable.material_ic_group_24dp))

                paxLayout.visibility = View.VISIBLE
                // company and pax have the same height dictated by company
                // hence we need to show company if we want to show pax
                // even if company is empty
                companyView.visibility = View.VISIBLE
            } else {
                // No information.
                paxLayout.visibility = View.GONE
            }

            val properties = view.findViewById<ListView>(R.id.properties)
            val moreInfoPrompt = view.findViewById<TextView>(R.id.more_info_prompt)

            if (subHasExtraInfo(subscription)) {
                moreInfoPrompt.visibility = View.VISIBLE
                properties.visibility = View.GONE
            } else {
                properties.visibility = View.GONE
                moreInfoPrompt.visibility = View.GONE
            }

            return view
        }

        private fun getBalanceView(convertView: View?,
                                   parent: ViewGroup, balance: TransitBalance): View {
            var view = convertView
            if (view == null || view.tag !== TAG_BALANCE_VIEW) {
                view = activity?.layoutInflater?.inflate(R.layout.balance_item, parent, false)
                view!!.tag = TAG_BALANCE_VIEW
            }

            val validView = view.findViewById<TextView>(R.id.valid)
            val validity = TransitBalance.formatValidity(balance)
            if (validity != null) {
                validView.text = validity.spanned
                validView.visibility = View.VISIBLE
            } else
                validView.visibility = View.GONE

            val name = balance.name
            val nameView = view.findViewById<TextView>(R.id.name)
            val balanceView = view.findViewById<TextView>(R.id.balance)
            val balanceCur = balance.balance
            if (name != null) {
                nameView.text = name
                nameView.visibility = View.VISIBLE
            } else
                nameView.visibility = View.GONE

            balanceView.text = balanceCur.maybeObfuscateBalance().formatCurrencyString(true).spanned
            balanceView.visibility = View.VISIBLE

            return view
        }

        override fun isEnabled(position: Int): Boolean {
            val item = getItem(position) ?: return false

            // We don't do anything for balances, yet.
            return (item as? Subscription)?.let { subHasExtraInfo(it) } ?: false
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {

        Log.d(TAG, "Clicked $id $position")
        val item = listAdapter.getItem(position) ?: return

        if (item is TransitBalance) {
            return
        }

        if (item is Subscription) {
            val infos = subMergeInfos(item) ?: return

            val lv = v.findViewById<ListView>(R.id.properties)
            val tv = v.findViewById<TextView>(R.id.more_info_prompt)

            if (lv.visibility == View.VISIBLE) {
                lv.visibility = View.GONE
                tv.visibility = View.VISIBLE
                lv.adapter = null
                return
            }

            tv.visibility = View.GONE
            lv.visibility = View.INVISIBLE

            val a = ListItemAdapter(activity!!, infos)
            lv.adapter = a

            // Calculate correct height
            var totalHeight = 0
            for (i in 0 until a.count) {
                val li = a.getView(i, null, lv)
                li.measure(0, 0)
                totalHeight += li.measuredHeight
            }

            // Set correct height
            val par = lv.layoutParams
            par.height = totalHeight + lv.dividerHeight * (a.count - 1)
            lv.layoutParams = par
            lv.visibility = View.VISIBLE
            lv.requestLayout()

            lv.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val TAG = "CardBalanceFragment"

        private const val TAG_BALANCE_VIEW = "balanceView"
        private const val TAG_SUBSCRIPTION_VIEW = "subscriptionView"
        private const val TAG_ERROR_VIEW = "errorView"

        internal fun subHasExtraInfo(sub: Subscription): Boolean = Subscription.hasInfo(sub)

        internal fun subMergeInfos(sub: Subscription): List<ListItem>? = Subscription.mergeInfo(sub)
    }
}
