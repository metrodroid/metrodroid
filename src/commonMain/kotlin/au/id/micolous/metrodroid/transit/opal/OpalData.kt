/*
 * OpalData.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.opal

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.VisibleForTesting
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Constants used on an Opal card.
 */
// for tests
object OpalData {
    // Opal travel modes
    private const val MODE_RAIL = 0x00
    private const val MODE_FERRY_LR = 0x01 // Ferry and Light Rail
    @VisibleForTesting
    const val MODE_BUS = 0x02

    // Opal actions
    private const val ACTION_NONE = 0x00
    private const val ACTION_NEW_JOURNEY = 0x01
    private const val ACTION_TRANSFER_SAME_MODE = 0x02
    private const val ACTION_TRANSFER_DIFF_MODE = 0x03
    private const val ACTION_MANLY_NEW_JOURNEY = 0x04
    private const val ACTION_MANLY_TRANSFER_SAME_MODE = 0x05
    private const val ACTION_MANLY_TRANSFER_DIFF_MODE = 0x06
    @VisibleForTesting
    const val ACTION_JOURNEY_COMPLETED_DISTANCE = 0x07
    private const val ACTION_JOURNEY_COMPLETED_FLAT_RATE = 0x08
    private const val ACTION_JOURNEY_COMPLETED_AUTO_ON = 0x09
    private const val ACTION_JOURNEY_COMPLETED_AUTO_OFF = 0x0a
    private const val ACTION_TAP_ON_REVERSAL = 0x0b
    private const val ACTION_TAP_ON_REJECTED = 0x0c

    private val MODES = mapOf(
            MODE_RAIL to R.string.opal_vehicle_rail,
            MODE_FERRY_LR to R.string.opal_vehicle_ferry_lr,
            MODE_BUS to R.string.opal_vehicle_bus)

    private val ACTIONS = mapOf(
        ACTION_NONE to R.string.opal_action_none,
        ACTION_NEW_JOURNEY to R.string.opal_action_new_journey,
        ACTION_TRANSFER_SAME_MODE to R.string.opal_action_transfer_same_mode,
        ACTION_TRANSFER_DIFF_MODE to R.string.opal_action_transfer_diff_mode,
        ACTION_MANLY_NEW_JOURNEY to R.string.opal_action_manly_new_journey,
        ACTION_MANLY_TRANSFER_SAME_MODE to R.string.opal_action_manly_transfer_same_mode,
        ACTION_MANLY_TRANSFER_DIFF_MODE to R.string.opal_action_manly_transfer_diff_mode,
        ACTION_JOURNEY_COMPLETED_DISTANCE to R.string.opal_action_journey_completed_distance,
        ACTION_JOURNEY_COMPLETED_FLAT_RATE to R.string.opal_action_journey_completed_flat_rate,
        ACTION_JOURNEY_COMPLETED_AUTO_OFF to R.string.opal_action_journey_completed_auto_off,
        ACTION_JOURNEY_COMPLETED_AUTO_ON to R.string.opal_action_journey_completed_auto_on,
        ACTION_TAP_ON_REVERSAL to R.string.opal_action_tap_on_reversal,
        ACTION_TAP_ON_REJECTED to R.string.opal_action_tap_on_rejected)

    internal fun getLocalisedMode(mode: Int): String {
        MODES[mode]?.let { return Localizer.localizeString(it) }
        return Localizer.localizeString(R.string.unknown_format, NumberUtils.longToHex(mode.toLong()))
    }

    internal fun getLocalisedAction(action: Int): String {
        ACTIONS[action]?.let { return Localizer.localizeString(it) }
        return Localizer.localizeString(R.string.unknown_format, NumberUtils.longToHex(action.toLong()))
    }
}
