/*
 * HSLArvo.kt
 *
 * Copyright 2019 Google
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

import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.ui.ListItem
import kotlin.random.Random

@Parcelize
data class HSLArvo(override val parsed: En1545Parsed,
                   val lastTransaction: HSLTransaction?): En1545Subscription() {
    override val lookup: En1545Lookup
        get() = HSLLookup

    @VisibleForTesting
    fun formatPeriod(): String {
        val period = parsed.getIntOrZero(CONTRACT_PERIOD)
        val units = parsed.getIntOrZero(CONTRACT_PERIOD_UNITS)
        when (units) {
            0 -> return Localizer.localizePlural(R.plurals.hsl_valid_mins,
                    period, period)
            1 -> return Localizer.localizePlural(R.plurals.hsl_valid_hours,
                    period, period)
            2 -> return Localizer.localizePlural(R.plurals.hsl_valid_days_24h,
                    period, period)
            else -> return Localizer.localizePlural(R.plurals.hsl_valid_days_calendar,
                    period, period)
        }
    }

    @VisibleForTesting
    val profile: String?
        get() {
        val prof = parsed.getInt(CUSTOMER_PROFILE)
            when (prof) {
                null -> {}
                1 -> return Localizer.localizeString(R.string.hsl_adult)
                else -> return Localizer.localizeString(R.string.unknown_format, prof)
            }
            val child = parsed.getInt(CHILD)
            when (child) {
                0 -> return Localizer.localizeString(R.string.hsl_adult)
                1 -> return Localizer.localizeString(R.string.hsl_child)
                else -> return null
            }
    }

    @VisibleForTesting
    val language get() = HSLLookup.languageCode(parsed.getInt(LANGUAGE_CODE))

    override val info: List<ListItem>
        get() = super.info.orEmpty().filter { it.text1?.unformatted != Localizer.localizeString(R.string.purchase_date) } + listOfNotNull(
                ListItem(R.string.hsl_period, formatPeriod()), // FIXME: put above separator
                ListItem(R.string.hsl_language, language),
                profile?.let { ListItem(R.string.hsl_customer_profile, it) },
                purchaseTimestamp?.let { ListItem(R.string.purchase_date, it.format() + (formatHour(parsed.getInt(CONTRACT_SALE_HOUR)) ?: FormattedString(""))) }
        )

    override val subscriptionName: String?
        get() = Localizer.localizeString(R.string.hsl_arvo_format,
                HSLLookup.getArea(parsed, prefix = CONTRACT_PREFIX,
                        isValidity = true))

    companion object {
        private const val CONTRACT_PERIOD_UNITS = "ContractPeriodUnits"
        private const val CONTRACT_PERIOD = "ContractPeriod"
        private const val CONTRACT_PREFIX = "Contract"
        private const val LANGUAGE_CODE = "LanguageCode"
        private const val CHILD = "Child"
        private const val CUSTOMER_PROFILE = "CustomerProfile"
        private const val CONTRACT_SALE_HOUR = "ContractSaleHour"
        private val FIELDS_WALTTI = En1545Container(
                En1545FixedInteger(HSLLookup.contractWalttiRegionName(CONTRACT_PREFIX), 8),
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger(CUSTOMER_PROFILE, 5),
                En1545FixedInteger("CustomerProfileGroup", 5),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractWalttiZoneName(CONTRACT_PREFIX), 6),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3), // Unconfirmed
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 14),  // Unconfirmed
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedInteger(CONTRACT_UNKNOWN_A, 5),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("reservedA", 5),
                En1545FixedInteger("ValidityStatus", 1)
        )

        private val FIELDS_V1 = En1545Container(
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger(CHILD, 1),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 1),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 4),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 5),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("reservedA", 5),
                En1545FixedInteger("ValidityStatus", 1)
        )

        private val FIELDS_V1_UL = En1545Container(
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger(CHILD, 1),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 1),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 4),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger(CONTRACT_SALE_DEVICE, 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 5),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedHex("Seal1", 48),
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("reservedA", 5),
                En1545FixedInteger("ValidityStatus", 1)
        )

        private val FIELDS_V2 = En1545Container(
                En1545FixedInteger("ProductCodeType", 1),
                En1545FixedInteger("ProductCode", 14),
                En1545FixedInteger("ProductCodeGroup", 14),
                En1545FixedInteger(CUSTOMER_PROFILE, 5),
                En1545FixedInteger("CustomerProfileGroup", 5),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger("ValidityLengthTypeGroup", 2),
                En1545FixedInteger("ValidityLengthGroup", 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 2),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 6),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger("SaleDeviceNumber", 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14),
                En1545FixedInteger("TicketFareGroup", 14),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6),
                En1545FixedInteger("ExtraZone", 1),
                En1545FixedInteger("PeriodPassValidityArea", 6),
                En1545FixedInteger("ExtensionProductCode", 14),
                En1545FixedInteger("Extension1ValidityArea", 6),
                En1545FixedInteger("Extension1Fare", 14),
                En1545FixedInteger("Extension2ValidityArea", 6),
                En1545FixedInteger("Extension2Fare", 14),
                En1545FixedInteger("SaleStatus", 1),
                En1545FixedInteger("reservedA", 4),
                En1545FixedInteger.date(CONTRACT_START) ,
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END),
                En1545FixedInteger("ValidityEndDateGroup", 14),
                En1545FixedInteger("ValidityEndTimeGroup", 11),
                En1545FixedInteger("reservedB", 5),
                En1545FixedInteger("ValidityStatus", 1)
        )

        private val FIELDS_V2_UL = En1545Container(
                En1545FixedInteger("ProductCode", 10),
                En1545FixedInteger(CHILD, 1),
                En1545FixedInteger(LANGUAGE_CODE, 2),
                En1545FixedInteger(CONTRACT_PERIOD_UNITS, 2),
                En1545FixedInteger(CONTRACT_PERIOD, 8),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(CONTRACT_PREFIX), 2),
                En1545FixedInteger(HSLLookup.contractAreaName(CONTRACT_PREFIX), 6),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedInteger(CONTRACT_SALE_HOUR, 5),
                En1545FixedInteger("SaleDeviceType", 3),
                En1545FixedInteger("SaleDeviceNumber", 14),
                En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 15),
                En1545FixedInteger(CONTRACT_PASSENGER_TOTAL, 6),
                En1545FixedHex("Seal1", 48),
                En1545FixedInteger.date(CONTRACT_START) ,
                En1545FixedInteger.timeLocal(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger.timeLocal(CONTRACT_END)
                // RFU 14 bits and seal2 64 bits
        )

        fun parse(raw: ImmutableByteArray, version: HSLTransitData.Variant): HSLArvo?  {
            if (raw.isAllZero())
                return null
            val (fields, offset) = when (version) {
                HSLTransitData.Variant.HSL_V1 -> Pair(FIELDS_V1, 144)
                HSLTransitData.Variant.HSL_V2 -> Pair(FIELDS_V2, 286)
                HSLTransitData.Variant.WALTTI -> Pair(FIELDS_WALTTI, 168)
            }
            val parsed = En1545Parser.parse(raw, fields)
            return HSLArvo(parsed,
                HSLTransaction.parseEmbed(raw = raw, version = version, offset = offset,
                                          walttiArvoRegion = parsed.getInt(HSLLookup.contractWalttiRegionName(CONTRACT_PREFIX))))
        }
        fun parseUL(raw: ImmutableByteArray, version: Int): HSLArvo?  {
            if (raw.isAllZero())
                return null
            if (version == 2)
                return HSLArvo(En1545Parser.parse(raw, FIELDS_V2_UL),
                        HSLTransaction.parseEmbed(raw = raw, version = HSLTransitData.Variant.HSL_V2,
                                offset = 264))
            return HSLArvo(En1545Parser.parse(raw, FIELDS_V1_UL),
                    HSLTransaction.parseEmbed(raw = raw, version = HSLTransitData.Variant.HSL_V1,
                            offset = 264))
        }

        fun formatHour(hour: Int?): FormattedString? {
            hour ?: return null
            val obfHour = if (!Preferences.obfuscateTripTimes) hour else ((hour + 21 + Random.nextInt(6)) % 24)
            return FormattedString(" ${NumberUtils.zeroPad(obfHour,2)}:XX")
        }
    }
}
