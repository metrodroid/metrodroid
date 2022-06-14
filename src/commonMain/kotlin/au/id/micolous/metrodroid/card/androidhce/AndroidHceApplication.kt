/*
 * AndroidHceApplication.kt
 *
 * Copyright 2018-2022 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.androidhce

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * Android HCE Application, from com.android.nfc.cardemulation.HostCardEmulationManager
 *
 * This application does nothing, however we can use this to detect an emulated card.
 */
@Serializable
data class AndroidHceApplication(
    override val generic: ISO7816ApplicationCapsule) : ISO7816Application() {

    override val type: String
        get() = TYPE

    companion object {
        // https://android.googlesource.com/platform/packages/apps/Nfc/+/0477b13932a3a1417473e8e4ea37ab7202865999/src/com/android/nfc/cardemulation/HostEmulationManager.java#66
        private val FILENAMES = listOf(
            ImmutableByteArray.fromHex("A000000476416E64726F6964484345")
        )

        private const val TYPE = "androidhce"

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
                // This does nothing, because this application doesn't do anything:
                // https://android.googlesource.com/platform/packages/apps/Nfc/+/0477b13932a3a1417473e8e4ea37ab7202865999/src/com/android/nfc/cardemulation/HostEmulationManager.java#176
                return listOf<ISO7816Application>(AndroidHceApplication(capsule.freeze()))
            }
        }
    }
}

