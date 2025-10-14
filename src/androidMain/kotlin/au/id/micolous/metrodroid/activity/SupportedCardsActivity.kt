/*
 * SupportedCardsActivity.kt
 *
 * Copyright 2011, 2017 Eric Butler
 * Copyright 2015-2019 Michael Farrell
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
package au.id.micolous.metrodroid.activity

import androidx.fragment.app.Fragment
import au.id.micolous.metrodroid.fragment.SupportedCardsFragment

/**
 * @author Eric Butler, Michael Farrell
 */
class SupportedCardsActivity : FragmentWrapperActivity() {
    override fun createFragment(): Fragment = SupportedCardsFragment()
}
