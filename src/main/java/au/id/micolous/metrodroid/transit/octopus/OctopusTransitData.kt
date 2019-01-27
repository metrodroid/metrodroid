/*
 * OctopusTransitData.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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
package au.id.micolous.metrodroid.transit.octopus

import android.os.Parcel
import android.os.Parcelable

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.card.felica.FelicaService
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.china.NewShenzhenTransitData
import au.id.micolous.metrodroid.util.Utils

import org.apache.commons.lang3.ArrayUtils

import java.util.ArrayList
import java.util.Collections

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
actual class OctopusTransitData : TransitData {
    override fun describeContents(): Int = 0

    private var mOctopusBalance = 0
    private var mShenzhenBalance = 0
    private var mHasOctopus = false
    private var mHasShenzhen = false

    override// Octopus balance takes priority 1
    // Shenzhen Tong balance takes priority 2
    val balances: ArrayList<TransitBalance>?
        get() {
            val bals = ArrayList<TransitBalance>()
            if (mHasOctopus) {
                bals.add(TransitBalanceStored(TransitCurrency.HKD(mOctopusBalance)))
            }
            if (mHasShenzhen) {
                bals.add(TransitBalanceStored(TransitCurrency.CNY(mShenzhenBalance)))
            }
            return bals
        }

    override// TODO: Find out where this is on the card.
    val serialNumber: String?
        get() = null

    override val cardName: String
        get() = if (mHasShenzhen) {
            if (mHasOctopus) {
                Localizer.localizeString(R.string.card_name_octopus_szt_dual)
            } else {
                Localizer.localizeString(R.string.card_name_szt)
            }
        } else {
            Localizer.localizeString(R.string.card_name_octopus)
        }

    private constructor(card: FelicaCard) {
        var service: FelicaService? = null
        try {
            service = card.getSystem(SYSTEMCODE_OCTOPUS)!!.getService(SERVICE_OCTOPUS)
        } catch (ignored: NullPointerException) {
        }

        if (service != null) {
            val metadata = service.blocks[0].data
            mOctopusBalance = (metadata.byteArrayToInt(0, 4) - OctopusData.getOctopusOffset(card.scannedAt)) * 10
            mHasOctopus = true
        }

        service = null
        try {
            service = card.getSystem(SYSTEMCODE_SZT)!!.getService(SERVICE_SZT)
        } catch (ignored: NullPointerException) {
        }

        if (service != null) {
            val metadata = service.blocks[0].data
            mShenzhenBalance = (metadata.byteArrayToInt(0, 4) - OctopusData.getShenzhenOffset(card.scannedAt)) * 10
            mHasShenzhen = true
        }
    }

    private constructor(parcel: Parcel) {
        mOctopusBalance = parcel.readInt()
        mShenzhenBalance = parcel.readInt()
        mHasOctopus = parcel.readInt() == 1
        mHasShenzhen = parcel.readInt() == 1
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(mOctopusBalance)
        parcel.writeInt(mShenzhenBalance)
        parcel.writeInt(if (mHasOctopus) 1 else 0)
        parcel.writeInt(if (mHasShenzhen) 1 else 0)
    }

    actual companion object {
        @JvmStatic
        val CREATOR: Parcelable.Creator<OctopusTransitData> = object : Parcelable.Creator<OctopusTransitData> {
            override fun createFromParcel(`in`: Parcel): OctopusTransitData {
                return OctopusTransitData(`in`)
            }

            override fun newArray(size: Int): Array<OctopusTransitData?> {
                return arrayOfNulls(size)
            }
        }
        actual val SYSTEMCODE_SZT = 0x8005
        actual val SYSTEMCODE_OCTOPUS = 0x8008

        private val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.octopus_card, R.drawable.octopus_card_alpha)
                .setName(Localizer.localizeString(R.string.card_name_octopus))
                .setLocation(R.string.location_hong_kong)
                .setCardType(CardType.FeliCa)
                .build()

        actual val SERVICE_OCTOPUS = 0x0117
        actual val SERVICE_SZT = 0x0118

        private val TAG = "OctopusTransitData"

        val FACTORY: FelicaCardTransitFactory = object : FelicaCardTransitFactory {

            // Shenzhen Tong is added to supported list by new Shenzhen Tong code.
            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(systemCodes: List<Int>): Boolean {
                return systemCodes.contains(SYSTEMCODE_OCTOPUS) || systemCodes.contains(SYSTEMCODE_SZT)
            }

            override fun getCardInfo(systemCodes: List<Int>): CardInfo? {
                // OctopusTransitData is special, because it handles two types of cards.  So we can just
                // directly say which cardInfo matches.
                if (systemCodes.contains(SYSTEMCODE_OCTOPUS))
                    return CARD_INFO // also dual-mode cards.

                return if (systemCodes.contains(SYSTEMCODE_SZT)) NewShenzhenTransitData.CARD_INFO else null

            }

            override fun parseTransitData(felicaCard: FelicaCard): TransitData {
                return OctopusTransitData(felicaCard)
            }

            override fun parseTransitIdentity(card: FelicaCard): TransitIdentity {
                return if (card.getSystem(SYSTEMCODE_SZT) != null) {
                    if (card.getSystem(SYSTEMCODE_OCTOPUS) != null) {
                        // Dual-mode card.
                        TransitIdentity(Localizer.localizeString(R.string.card_name_octopus_szt_dual), null)
                    } else {
                        // SZT-only card.
                        TransitIdentity(Localizer.localizeString(R.string.card_name_szt), null)
                    }
                } else {
                    // Octopus-only card.
                    TransitIdentity(Localizer.localizeString(R.string.card_name_octopus), null)
                }
            }
        }
    }
}
