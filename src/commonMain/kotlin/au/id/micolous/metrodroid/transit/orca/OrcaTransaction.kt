/*
 * OrcaTransaction.kt
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package au.id.micolous.metrodroid.transit.orca

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.*

@Parcelize
class OrcaTransaction (private val mTimestamp: Long,
                       private val mCoachNum: Int,
                       private val mFtpType: Int,
                       private val mFare: Int,
                       private val mNewBalance: Int,
                       private val mAgency: Int,
                       private val mTransType: Int,
                       private val mIsTopup: Boolean): Transaction() {

    override val timestamp: TimestampFull?
        get() = if (mTimestamp == 0L) null else Epoch.utc(1970, MetroTimeZone.LOS_ANGELES).seconds(mTimestamp)

    override val isTapOff: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_TAP_OUT

    override val isCancel: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_CANCEL_TRIP

    // FIXME: Need to find bus route #s
    override val routeNames: List<FormattedString>?
        get() = when {
            mIsTopup -> listOf(Localizer.localizeFormatted(R.string.orca_topup))
            isLink -> super.routeNames
            isSounder -> super.routeNames
            isSeattleStreetcar -> super.routeNames
            mAgency == OrcaTransitData.AGENCY_ST -> listOf(FormattedString.english("Express Bus"))
            isMonorail -> listOf(FormattedString.english("Seattle Monorail"))
            isWaterTaxi -> listOf(FormattedString.english("Water Taxi"))
            isSwift -> super.routeNames
            mAgency == OrcaTransitData.AGENCY_KCM -> {
                when (mFtpType) {
                    FTP_TYPE_BUS -> listOf(FormattedString.english("Bus"))
                    FTP_TYPE_BRT -> super.routeNames
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(if (mIsTopup || mTransType == TRANS_TYPE_TAP_OUT) -mFare else mFare)

    override val station: Station?
        get() {
            if (mIsTopup)
                return null
            if (isSeattleStreetcar) {
                return StationTableReader.getStation(ORCA_STR_STREETCAR, mCoachNum)
            } else if (isRapidRide || isSwift) {
                return StationTableReader.getStation(ORCA_STR_BRT, mCoachNum)
            }
            val id = (mAgency shl 16) or (mCoachNum and 0xffff)
            val s = StationTableReader.getStationNoFallback(ORCA_STR, id,
                    NumberUtils.intToHex(id))
            if (s != null)
                return s
            if (isLink || isSounder || mAgency == OrcaTransitData.AGENCY_WSF) {
                return Station.unknown(mCoachNum)
            }

            return null
        }

    override val vehicleID: String?
        get() = when {
                mIsTopup -> mCoachNum.toString()
                isLink || isSounder || mAgency == OrcaTransitData.AGENCY_WSF ||
                    isSeattleStreetcar || isSwift || isRapidRide ||
                    isMonorail -> null
                else -> mCoachNum.toString()
            }

    override val mode: Trip.Mode
        get() = when {
            mIsTopup -> Trip.Mode.TICKET_MACHINE
            isMonorail -> Trip.Mode.MONORAIL
            isWaterTaxi -> Trip.Mode.FERRY
            else -> when (mFtpType) {
                FTP_TYPE_LINK -> Trip.Mode.METRO
                FTP_TYPE_SOUNDER -> Trip.Mode.TRAIN
                FTP_TYPE_FERRY -> Trip.Mode.FERRY
                FTP_TYPE_STREETCAR -> Trip.Mode.TRAM
                else -> Trip.Mode.BUS
            }
        }

    override val isTapOn: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_TAP_IN

    private val isLink: Boolean
        get() = mAgency == OrcaTransitData.AGENCY_ST && mFtpType == FTP_TYPE_LINK

    private val isSounder: Boolean
        get() = mAgency == OrcaTransitData.AGENCY_ST && mFtpType == FTP_TYPE_SOUNDER

    private val isSeattleStreetcar: Boolean
        get() = mFtpType == FTP_TYPE_STREETCAR //TODO: Find agency ID

    private val isMonorail: Boolean
        get() = (mAgency == OrcaTransitData.AGENCY_KCM &&
            mFtpType == FTP_TYPE_PURSE_DEBIT && mCoachNum == COACH_NUM_MONORAIL)

    // TODO: Determine if CoachID is used for Water Taxis
    private val isWaterTaxi: Boolean
        get() = (mAgency == OrcaTransitData.AGENCY_KCM &&
            mFtpType == FTP_TYPE_PURSE_DEBIT && mCoachNum != COACH_NUM_MONORAIL)

    private val isRapidRide: Boolean
        get() = mAgency == OrcaTransitData.AGENCY_KCM && mFtpType == FTP_TYPE_BRT

    private val isSwift: Boolean
        get() = mAgency == OrcaTransitData.AGENCY_CT && mFtpType == FTP_TYPE_BRT

    constructor(useData: ImmutableByteArray, isTopup: Boolean): this(
        mIsTopup = isTopup,
        mAgency = useData.getBitsFromBuffer(24, 4),
        mTimestamp = useData.getBitsFromBuffer(28, 32).toLong(),
        mCoachNum = useData.getBitsFromBuffer(68, 24),
        mFtpType = useData.getBitsFromBuffer(60, 8),
        mFare = useData.getBitsFromBuffer(120, 15),
        mTransType = useData.getBitsFromBuffer(136, 8),
        mNewBalance = useData.getBitsFromBuffer(272, 16))

    override fun getAgencyName(isShort: Boolean) = when {
        isMonorail -> // Seattle Monorail Services uses KCM's agency ID.
            FormattedString.language("SMS", "en-US")

        isWaterTaxi ->
            // The King County Water Taxi is now a separate agency but uses
            // KCM's agency ID
            FormattedString.language("KCWT", "en-US")

        else -> StationTableReader.getOperatorName(ORCA_STR, mAgency, isShort)
    }

    override fun isSameTrip(other: Transaction): Boolean {
        return other is OrcaTransaction && mAgency == other.mAgency
    }

    override fun getRawFields(level: TransitData.RawLevel) =
        (level == TransitData.RawLevel.ALL).ifTrue {
            (if (mIsTopup) "topup, " else "") +
                mapOf(
                    "agency" to mAgency,
                    "type" to mTransType,
                    "ftp" to mFtpType,
                    "coach" to mCoachNum,
                    "fare" to mFare,
                    "newBal" to mNewBalance
                ).map { "${it.key} = ${it.value.hexString}" }.joinToString()
        }

    companion object {
        private const val ORCA_STR = "orca"
        private const val ORCA_STR_BRT = "orca_brt"
        private const val ORCA_STR_STREETCAR = "orca_streetcar"

        private const val TRANS_TYPE_PURSE_USE = 0x0c
        private const val TRANS_TYPE_CANCEL_TRIP = 0x01
        private const val TRANS_TYPE_TAP_IN = 0x03
        private const val TRANS_TYPE_TAP_OUT = 0x07
        private const val TRANS_TYPE_PASS_USE = 0x60

        private const val FTP_TYPE_FERRY = 0x08
        private const val FTP_TYPE_SOUNDER = 0x09
        private const val FTP_TYPE_CUSTOMER_SERVICE = 0x0B
        private const val FTP_TYPE_BUS = 0x80
        private const val FTP_TYPE_STREETCAR = 0xF9
        private const val FTP_TYPE_BRT = 0xFA //May also apply to future hardwired bus readers
        private const val FTP_TYPE_LINK = 0xFB
        // Used by Monorail and Water Taxi services
        private const val FTP_TYPE_PURSE_DEBIT = 0xFE

        private const val COACH_NUM_MONORAIL = 0x3
    }
}
