/*
 * HSLTransitData.kt
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*

/**
 * Implements a reader for HSL transit cards.
 *
 * Documentation and sample libraries for this are available at:
 * http://dev.hsl.fi/#travel-card
 *
 * The documentation (in Finnish) is available at:
 * http://dev.hsl.fi/hsl-card-java/HSL-matkakortin-kuvaus.pdf
 *
 * Machine translation to English:
 * https://translate.google.com/translate?sl=auto&tl=en&js=y&prev=_t&hl=en&ie=UTF-8&u=http%3A%2F%2Fdev.hsl.fi%2Fhsl-card-java%2FHSL-matkakortin-kuvaus.pdf&edit-text=&act=url
 */
@Parcelize
class HSLTransitData(override val serialNumber: String?,
                     private val mBalance: Int,
                     override val trips: List<Trip>) : TransitData() {
    // TODO: push these into Subscriptions
    /*
        if (mHasKausi)
            ret += "\n" + app.getString(R.string.hsl_pass_is_valid);
        if (mArvoExpire * 1000.0 > System.currentTimeMillis())
            ret += "\n" + app.getString(R.string.hsl_value_ticket_is_valid) + "!";
    */

    override val cardName: String
        get() = "HSL"

    /*
    public String getCustomString() {
        DateFormat shortDateTimeFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        DateFormat shortDateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder ret = new StringBuilder();
        if (!mKausiNoData) {
            ret.append(GR(R.string.hsl_season_ticket)).append(":\n");
            ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ")
            .append(mKausiVehicleNumber).append("\n");
            ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ")
            .append(Long.toString(mKausiLineJORE).substring(1)).append("\n");
            ret.append("JORE extension").append(": ").append(mKausiJOREExt).append("\n");
            ret.append("Direction").append(": ").append(mKausiDirection).append("\n");

            ret.append(GR(R.string.hsl_season_ticket_starts)).append(": ")
            .append(shortDateFormat.format(mKausiStart * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_ends)).append(": ")
            .append(shortDateFormat.format(mKausiEnd * 1000.0));
            ret.append("\n\n");
            ret.append(GR(R.string.hsl_season_ticket_bought_on)).append(": ")
            .append(shortDateTimeFormat.format(mKausiPurchase * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_price_was)).append(": ")
            .append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mKausiPurchasePrice / 100.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_you_last_used_this_ticket)).append(": ")
            .append(shortDateTimeFormat.format(mKausiLastUse * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_previous_season_ticket)).append(": ")
            .append(shortDateFormat.format(mKausiPrevStart * 1000.0));
            ret.append(" - ").append(shortDateFormat.format(mKausiPrevEnd * 1000.0));
            ret.append("\n\n");
        }

        ret.append(GR(R.string.hsl_value_ticket)).append(":\n");
        ret.append(GR(R.string.hsl_value_ticket_bought_on)).append(": ")
        .append(shortDateTimeFormat.format(mArvoPurchase * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_expires_on)).append(": ")
        .append(shortDateTimeFormat.format(mArvoExpire * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_transfer)).append(": ")
        .append(shortDateTimeFormat.format(mArvoXfer * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_sign)).append(": ")
        .append(shortDateTimeFormat.format(mArvoExit * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_price)).append(": ")
        .append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mArvoPurchasePrice / 100.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_disco_group)).append(": ").append(mArvoDiscoGroup).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_pax)).append(": ").append(mArvoPax).append("\n");
        ret.append("Mystery1").append(": ").append(mArvoMystery1).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_duration)).append(": ").append(mArvoDuration).append(" min\n");
        ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ").append(mArvoVehicleNumber).append("\n");
        ret.append("Region").append(": ").append(regionNames[(int) mArvoRegional]).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ")
        .append(Long.toString(mArvoLineJORE).substring(1)).append("\n");
        ret.append("JORE extension").append(": ").append(mArvoJOREExt).append("\n");
        ret.append("Direction").append(": ").append(mArvoDirection).append("\n");

        return ret.toString();
    }
    */

    public override val balance: TransitCurrency?
        get() = TransitCurrency.EUR(mBalance)

    /*
    private static final String[] regionNames = {
        "N/A", "Helsinki", "Espoo", "Vantaa", "Koko alue", "Seutu", "", "", "", "",  // 0-9
        "", "", "", "", "", "", "", "", "", "", // 10-19
        "", "", "", "", "", "", "", "", "", "", // 20-29
        "", "", "", "", "", "", "", "", "", ""}; // 30-39
        */
    /*    private static final Map<Long,String> vehicleNames =  Collections.unmodifiableMap(new HashMap<Long, String>() {{
        put(1L, "Metro");
        put(18L, "Bus");
        put(16L, "Tram");
    }});*/

    companion object {
        private fun parseTrips(card: DesfireCard): MutableList<HSLTrip> {
            val file = card.getApplication(APP_ID)!!.getFile(0x04)

            if (file is RecordDesfireFile) {
                val recordFile = file as RecordDesfireFile?

                val useLog = mutableListOf<HSLTrip>()
                useLog += recordFile!!.records.map { HSLTrip(it) }
                useLog.sortWith(Trip.Comparator())
                return useLog
            }
            return mutableListOf()
        }

        private fun parse(desfireCard: DesfireCard): HSLTransitData {
            val serialNumber = desfireCard.getApplication(APP_ID)!!.getFile(0x08)!!.data.toHexString().substring(2, 20)  //data.byteArrayToInt(1, 9);

            var data = desfireCard.getApplication(APP_ID)!!.getFile(0x02)!!.data
            val mBalance = data.getBitsFromBuffer(0, 20)
            val mLastRefill = HSLRefill(data)

            val mTrips = parseTrips(desfireCard)

            var balanceIndex = -1

            for (i in mTrips.indices) {
                if (mTrips[i].mArvo == 1) {
                    balanceIndex = i
                    break
                }
            }

            data = desfireCard.getApplication(APP_ID)!!.getFile(0x03)!!.data
            /*val mArvoMystery1 = data.getBitsFromBuffer(0, 9).toLong()
            val mArvoDiscoGroup = data.getBitsFromBuffer(9, 5).toLong()
            val mArvoDuration = data.getBitsFromBuffer(14, 13).toLong()
            val mArvoRegional = data.getBitsFromBuffer(27, 5).toLong()

            val mArvoExit = cardDateToCalendar(
                    data.getBitsFromBuffer(32, 14),
                    data.getBitsFromBuffer(46, 11))*/

            //68 price, 82 zone?
            val mArvoPurchasePrice = data.getBitsFromBuffer(68, 14)
            //mArvoDiscoGroup = data.getBitsFromBuffer(82, 6);
            val mArvoPurchase = cardDateToCalendar(
                    data.getBitsFromBuffer(88, 14),
                    data.getBitsFromBuffer(102, 11))

            val mArvoExpire = cardDateToCalendar(
                    data.getBitsFromBuffer(113, 14),
                    data.getBitsFromBuffer(127, 11))

            val mArvoPax = data.getBitsFromBuffer(138, 6)

            /*val mArvoXfer = cardDateToCalendar(
                    data.getBitsFromBuffer(144, 14),
                    data.getBitsFromBuffer(158, 11))*/

            val mArvoVehicleNumber = data.getBitsFromBuffer(169, 14)

            //val mArvoUnknown = data.getBitsFromBuffer(183, 2).toLong()

            val mArvoLineJORE = data.getBitsFromBuffer(185, 14).toLong()
            //val mArvoJOREExt = data.getBitsFromBuffer(199, 4).toLong()
            //val mArvoDirection = data.getBitsFromBuffer(203, 1).toLong()

            if (balanceIndex > -1) {
                mTrips[balanceIndex].mLine = mArvoLineJORE.toString()
                mTrips[balanceIndex].mVehicleNumber = mArvoVehicleNumber
            } else if (mArvoPurchase.timeInMillis > 2) {
                val t = HSLTrip(mArvo = 1,
                        expireTimestamp = mArvoExpire,
                        mFare = mArvoPurchasePrice,
                        passengerCount = mArvoPax,
                        startTimestamp = mArvoPurchase,
                        mVehicleNumber = mArvoVehicleNumber,
                        mLine = mArvoLineJORE.toString())
                mTrips.add(t)
                mTrips.sortWith(Trip.Comparator())
            }

            var seasonIndex = -1
            for (i in mTrips.indices) {
                if (mTrips[i].mArvo == 0) {
                    seasonIndex = i
                    break
                }
            }

            data = desfireCard.getApplication(APP_ID)!!.getFile(0x01)!!.data

            /*if (data.getBitsFromBuffer(19, 14) == 0 && data.getBitsFromBuffer(67, 14) == 0) {
                val mKausiNoData = true
            }*/

            /*var mKausiStart = cardDateToCalendar(data.getBitsFromBuffer(19, 14))
            var mKausiEnd: Timestamp = cardDateToCalendar(data.getBitsFromBuffer(33, 14))
            var mKausiPrevStart = cardDateToCalendar(data.getBitsFromBuffer(67, 14))
            var mKausiPrevEnd: Timestamp = cardDateToCalendar(data.getBitsFromBuffer(81, 14))
            if (mKausiPrevStart.compareTo(mKausiStart) > 0) {
                val temp = mKausiStart
                val temp2 = mKausiEnd
                mKausiStart = mKausiPrevStart
                mKausiEnd = mKausiPrevEnd
                mKausiPrevStart = temp
                mKausiPrevEnd = temp2
            }*/
            val mKausiPurchase = cardDateToCalendar(
                    data.getBitsFromBuffer(110, 14),
                    data.getBitsFromBuffer(124, 11))
            val mKausiPurchasePrice = data.getBitsFromBuffer(149, 15)
            /*val (timeInMillis, tz) = cardDateToCalendar(
                    data.getBitsFromBuffer(192, 14),
                    data.getBitsFromBuffer(206, 11))*/
            val mKausiVehicleNumber = data.getBitsFromBuffer(217, 14)
            //mTrips[0].mVehicleNumber = mArvoVehicleNumber;

            //val mKausiUnknown = data.getBitsFromBuffer(231, 2).toLong()

            val mKausiLineJORE = data.getBitsFromBuffer(233, 14).toLong()
            //mTrips[0].mLine = Long.toString(mArvoLineJORE).substring(1);

            //val mKausiJOREExt = data.getBitsFromBuffer(247, 4).toLong()
            //val mKausiDirection = data.getBitsFromBuffer(241, 1).toLong()
            if (seasonIndex > -1) {
                mTrips[seasonIndex].mVehicleNumber = mKausiVehicleNumber
                mTrips[seasonIndex].mLine = mKausiLineJORE.toString()
            } else if (mKausiVehicleNumber > 0) {
                mTrips += HSLTrip(mArvo = 0,
                        expireTimestamp = mKausiPurchase,
                        mFare = mKausiPurchasePrice,
                        passengerCount = 1,
                        startTimestamp = mKausiPurchase,
                        mVehicleNumber = mKausiVehicleNumber,
                        mLine = mKausiLineJORE.toString()
                )
            }

            return HSLTransitData(serialNumber = serialNumber,
                    mBalance = mBalance,
                    trips = mTrips + listOfNotNull(mLastRefill))
        }

        private val EPOCH = Epoch.local(1997, MetroTimeZone.HELSINKI)

        private val CARD_INFO = CardInfo(
                imageId = R.drawable.hsl_card,
                name = "HSL",
                locationId = R.string.location_helsinki_finland,
                cardType = CardType.MifareDesfire)

        private const val APP_ID = 0x1120ef

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(CARD_INFO)

            override fun earlyCheck(appIds: IntArray) = APP_ID in appIds

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity {
                try {
                    val data = card.getApplication(APP_ID)!!.getFile(0x08)!!.data
                    return TransitIdentity("HSL", data.toHexString().substring(2, 20))
                } catch (ex: Exception) {
                    throw RuntimeException("Error parsing HSL serial", ex)
                }

            }
        }

        internal fun cardDateToCalendar(day: Int, minute: Int): TimestampFull = EPOCH.dayMinute(day, minute)

        internal fun cardDateToCalendar(day: Int): Daystamp = EPOCH.days(day)
    }
}
