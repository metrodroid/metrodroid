/*
 * ThessUltralightTransitFactory.kt
 *
 * Copyright 2024 apo-mak
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
package au.id.micolous.metrodroid.transit.thess

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.Trip

@Parcelize
data class ThessUltralightTransaction(
        private val mTimestamp: TimestampFull?,
        private val mIsEntry: Boolean,
        private val mIsUsed: Boolean,
        private val mIsSingleUse: Boolean,
        private val mProductCode: Int) : Transaction {

    override val timestamp get() = mTimestamp
    
    override val mode get() = Trip.Mode.METRO
    
    override val isTapOff get() = !mIsEntry
    
    override val isTapOn get() = mIsEntry
    
    override val fare get() = null // No fare stored in card
    
    override val machineID get() = null // Not implemented for now
    
    override fun getAgencyName(isShort: Boolean) = 
        FormattedString("Thessaloniki Metro")
    
    override fun getRawFields(level: Int) = super.getRawFields(level).orEmpty() + listOfNotNull(
        FormattedString("Product code") to FormattedString("0x${mProductCode.toString(16)}"),
        if (mIsSingleUse)
            FormattedString("Ticket type") to FormattedString("Single-use")
        else
            FormattedString("Ticket type") to FormattedString("Multi-trip"),
        FormattedString("Used") to FormattedString(mIsUsed.toString())
    )

    companion object {
        // Status byte is at page 6, byte 0
        // bit7-4 = ticket type (0x8 = single-use, 0xF = multi-trip)
        // bit2 = direction (0 = entry, 1 = exit)
        // bit1 = USED flag (1 = already validated, for single-use)
        fun parseStatusByte(statusByte: Int): Triple<Boolean, Boolean, Boolean> {
            val ticketType = (statusByte and 0xf0)
            val isSingleUse = ticketType == 0x80
            val isEntry = (statusByte and 0x04) == 0
            val isUsed = (statusByte and 0x02) != 0
            
            return Triple(isEntry, isUsed, isSingleUse)
        }
        
        val THESSALONIKI_TIMEZONE = MetroTimeZone.GREECE
    }
}
