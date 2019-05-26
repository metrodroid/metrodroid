package au.id.micolous.metrodroid.card.ksx6924

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.kr_ocap.KROCAPTransitData
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * Implements the Config DF specified by One Card All Pass.
 *
 * This is **not** implemented by Snapper.
 */
@Serializable
data class KROCAPConfigDFApplication (
        override val generic: ISO7816ApplicationCapsule): ISO7816Application() {
    override val type: String
        get() = TYPE

    override fun parseTransitIdentity(card: ISO7816Card): TransitIdentity? {
        if (card.applications.any(KSX6924Application.APP_NAME)) {
            // Don't try to handle something that has a KSX6924Application.
            //return null
        }

        return KROCAPTransitData.parseTransitIdentity(this)
    }

    override fun parseTransitData(card: ISO7816Card): TransitData? {
        if (card.applications.any(KSX6924Application.APP_NAME)) {
            // Don't try to handle something that has a KSX6924Application.
            //return null
        }

        return KROCAPTransitData.parseTransitData(this)
    }


    companion object {
        private val TAG = "KROCAPConfigDFApplication"
        private val NAME = "One Card All Pass"

        val APP_NAME = listOf(ImmutableByteArray.fromHex("a0000004520001"))

        private const val TYPE = "kr_ocap_configdf"


        val FACTORY: ISO7816ApplicationFactory = object : ISO7816ApplicationFactory {
            override val typeMap: Map<String, KSerializer<out ISO7816Application>>
                get() = mapOf(TYPE to serializer())

            override val applicationNames: List<ImmutableByteArray>
                get() = APP_NAME

            override suspend fun dumpTag(protocol: ISO7816Protocol,
                                         capsule: ISO7816ApplicationMutableCapsule,
                                         feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application>? {
                feedbackInterface.updateStatusText(
                        Localizer.localizeString(R.string.card_reading_type, NAME))

                return listOf<ISO7816Application>(KROCAPConfigDFApplication(capsule.freeze()))
            }
        }


    }
}
