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

package au.id.micolous.metrodroid.transit.opal;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.ImmutableMapBuilder;
import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Constants used on an Opal card.
 */
@SuppressWarnings("WeakerAccess") // for tests
public final class OpalData {
    // Opal travel modes
    public static final int MODE_RAIL = 0x00;
    public static final int MODE_FERRY_LR = 0x01; // Ferry and Light Rail
    public static final int MODE_BUS = 0x02;

    // Opal actions
    public static final int ACTION_NONE = 0x00;
    public static final int ACTION_NEW_JOURNEY = 0x01;
    public static final int ACTION_TRANSFER_SAME_MODE = 0x02;
    public static final int ACTION_TRANSFER_DIFF_MODE = 0x03;
    public static final int ACTION_MANLY_NEW_JOURNEY = 0x04;
    public static final int ACTION_MANLY_TRANSFER_SAME_MODE = 0x05;
    public static final int ACTION_MANLY_TRANSFER_DIFF_MODE = 0x06;
    public static final int ACTION_JOURNEY_COMPLETED_DISTANCE = 0x07;
    public static final int ACTION_JOURNEY_COMPLETED_FLAT_RATE = 0x08;
    public static final int ACTION_JOURNEY_COMPLETED_AUTO_ON = 0x09;
    public static final int ACTION_JOURNEY_COMPLETED_AUTO_OFF = 0x0a;
    public static final int ACTION_TAP_ON_REVERSAL = 0x0b;
    public static final int ACTION_TAP_ON_REJECTED = 0x0c;

    static final Map<Integer, Integer> MODES = new ImmutableMapBuilder<Integer, Integer>()
            .put(MODE_RAIL, R.string.opal_vehicle_rail)
            .put(MODE_FERRY_LR, R.string.opal_vehicle_ferry_lr)
            .put(MODE_BUS, R.string.opal_vehicle_bus).build();

    static final Map<Integer, Integer> ACTIONS = new ImmutableMapBuilder<Integer, Integer>()
            .put(ACTION_NONE, R.string.opal_action_none)
            .put(ACTION_NEW_JOURNEY, R.string.opal_action_new_journey)
            .put(ACTION_TRANSFER_SAME_MODE, R.string.opal_action_transfer_same_mode)
            .put(ACTION_TRANSFER_DIFF_MODE, R.string.opal_action_transfer_diff_mode)
            .put(ACTION_MANLY_NEW_JOURNEY, R.string.opal_action_manly_new_journey)
            .put(ACTION_MANLY_TRANSFER_SAME_MODE, R.string.opal_action_manly_transfer_same_mode)
            .put(ACTION_MANLY_TRANSFER_DIFF_MODE, R.string.opal_action_manly_transfer_diff_mode)
            .put(ACTION_JOURNEY_COMPLETED_DISTANCE, R.string.opal_action_journey_completed_distance)
            .put(ACTION_JOURNEY_COMPLETED_FLAT_RATE, R.string.opal_action_journey_completed_flat_rate)
            .put(ACTION_JOURNEY_COMPLETED_AUTO_OFF, R.string.opal_action_journey_completed_auto_off)
            .put(ACTION_JOURNEY_COMPLETED_AUTO_ON, R.string.opal_action_journey_completed_auto_on)
            .put(ACTION_TAP_ON_REVERSAL, R.string.opal_action_tap_on_reversal)
            .put(ACTION_TAP_ON_REJECTED, R.string.opal_action_tap_on_rejected)
            .build();

    private OpalData() {
    }

    static String getLocalisedMode(int mode) {
        if (MODES.containsKey(mode)) {
            return Localizer.INSTANCE.localizeString(MODES.get(mode));
        }
        return Localizer.INSTANCE.localizeString(R.string.unknown_format, NumberUtils.INSTANCE.longToHex(mode));
    }

    static String getLocalisedAction(int action) {
        if (ACTIONS.containsKey(action)) {
            return Localizer.INSTANCE.localizeString(ACTIONS.get(action));
        }

        return Localizer.INSTANCE.localizeString(R.string.unknown_format, NumberUtils.INSTANCE.longToHex(action));
    }
}
