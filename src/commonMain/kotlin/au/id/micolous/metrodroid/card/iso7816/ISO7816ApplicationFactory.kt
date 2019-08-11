/*
 * ISO7816ApplicationFactory.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer

interface ISO7816ApplicationFactory {
    val applicationNames: Collection<ImmutableByteArray>

    /**
     * If True, after dumping the first successful application (that doesn't result in an error,
     * such as file not found), don't try to process any more application names from this factory.
     *
     * @return True to stop after the first app, False to dump all apps from this factory.
     */
    val stopAfterFirstApp: Boolean
        get() = false

    suspend fun dumpTag(protocol: ISO7816Protocol,
                        capsule: ISO7816ApplicationMutableCapsule,
                        feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application>?

    val typeMap: Map<String, KSerializer<out ISO7816Application>>

    val fixedAppIds: Boolean
       get() = true
}
