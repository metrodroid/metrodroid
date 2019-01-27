/*
 * TmoneyTransitData.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.tmoney

import android.os.Parcel
import android.os.Parcelable

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.NumberUtils
import org.jetbrains.annotations.NonNls

import java.util.ArrayList

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.iso7816.ISO7816TLV
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

actual class TMoneyTransitData : TransitData {

    override val serialNumber: String?
    private val mBalance: Int
    private val mDate: String?
    private val mTrips: List<TMoneyTrip>?

    public override val balance: TransitCurrency?
        get() = TransitCurrency.KRW(mBalance)

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_tmoney)

    override val info: List<ListItem>?
        get() {
            val items = ArrayList<ListItem>()

            items.add(ListItem(R.string.tmoney_date, mDate))

            return items
        }

    override val trips: List<TMoneyTrip>?
        get() = mTrips

    actual constructor(tMoneyCard: TMoneyCard) : super() {
        serialNumber = parseSerial(tMoneyCard)
        mBalance = tMoneyCard.balance.byteArrayToInt()
        mDate = parseDate(tMoneyCard)
        mTrips = ArrayList()
        for (record in tMoneyCard.transactionRecords!!) {
            val t = TMoneyTrip.parseTrip(record) ?: continue
            mTrips.add(t)
        }
    }

    private constructor(p: Parcel) {
        serialNumber = p.readString()
        mBalance = p.readInt()
        mDate = p.readString()

        mTrips = p.readArrayList(TMoneyTrip::class.java.classLoader) as List<TMoneyTrip>
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(serialNumber)
        dest.writeInt(mBalance)
        dest.writeString(mDate)
        dest.writeList(mTrips)
    }

    override fun describeContents(): Int = 0

    actual companion object {
        @JvmStatic
        val CREATOR: Parcelable.Creator<TMoneyTransitData> = object : Parcelable.Creator<TMoneyTransitData> {
            override fun createFromParcel(parcel: Parcel): TMoneyTransitData {
                return TMoneyTransitData(parcel)
            }

            override fun newArray(size: Int): Array<TMoneyTransitData?> {
                return arrayOfNulls(size)
            }
        }

        actual val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.tmoney_card)
                .setName(Localizer.localizeString(R.string.card_name_tmoney))
                .setLocation(R.string.location_seoul)
                .setCardType(CardType.ISO7816)
                .setPreview()
                .build()

        actual fun parseTransitIdentity(card: TMoneyCard): TransitIdentity {
            return TransitIdentity(Localizer.localizeString(R.string.card_name_tmoney), parseSerial(card))
        }

        private fun getSerialTag(card: TMoneyCard): ImmutableByteArray? {
            return ISO7816TLV.findBERTLV(card.appFci!!, "b0", false)
        }

        private fun parseSerial(card: TMoneyCard): String {
            return NumberUtils.groupString(getSerialTag(card)!!.getHexString(4, 8), " ", 4, 4, 4)
        }

        @NonNls
        private fun parseDate(card: TMoneyCard): String {
            val tmoneytag = getSerialTag(card)
            return (tmoneytag!!.getHexString(17, 2) + "/"
                    + tmoneytag.getHexString(19, 1))
        }
    }
}
