/*
 * VasApplication.kt
 *
 * Copyright 2025 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.vas

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * VAS application.
 *
 * https://github.com/kormax/apple-vas
 *
 * This implementation is incomplete, but we can use this to detect Apple and Google Pay, and read
 * EMV more reliably.
 */
@Serializable
data class VasApplication(
    override val generic: ISO7816ApplicationCapsule) : ISO7816Application() {

    override val type: String
        get() = TYPE

    companion object {
        private val FILENAMES = listOf(
            ImmutableByteArray.fromHex("4F53452E5641532E3031")
        )

        private const val TYPE = "applevas"

        val FACTORY: ISO7816ApplicationFactory = object : ISO7816ApplicationFactory {
            override val typeMap: Map<String, KSerializer<out ISO7816Application>>
                get() = mapOf(TYPE to serializer() )
            override val applicationNames: Collection<ImmutableByteArray>
                get() = FILENAMES

            override fun dumpTag(
                protocol: ISO7816Protocol,
                capsule: ISO7816ApplicationMutableCapsule,
                feedbackInterface: TagReaderFeedbackInterface,
                presentAids: List<ImmutableByteArray?>
            ): List<ISO7816Application> {
                // TODO: read data files
                return listOf<ISO7816Application>(VasApplication(capsule.freeze()))
            }
        }
    }
}

