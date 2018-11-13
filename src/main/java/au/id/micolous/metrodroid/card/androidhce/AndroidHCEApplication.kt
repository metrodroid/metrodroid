package au.id.micolous.metrodroid.card.androidhce

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application
import au.id.micolous.metrodroid.util.Utils

/**
 * Used to detect support for Android Host Card Emulation.
 *
 * We need to detect this explicitly, as a typical workflow allows an app doing HCE to pause all
 * further HCE processing once a card is reached that is supported, but there is no virtual card
 * configured on the device.
 *
 * As a result, we need to ask the user which cards they want to read via HCE.
 */
class AndroidHCEApplication : ISO7816Application {

    private constructor(appData: ISO7816Application.ISO7816Info) : super(appData)

    @Suppress("unused")
    private constructor() : super() /* For XML Serializer */

    companion object {
        val TYPE = "androidhce"

        /**
         * From com.android.nfc.cardemulation.HostEmulationManager:
         * https://android.googlesource.com/platform/packages/apps/Nfc/+/master/src/com/android/nfc/cardemulation/HostEmulationManager.java
         */
        val ANDROID_HCE_AID = Utils.hexStringToByteArray("A000000476416E64726F6964484345")

        /**
         * This records the fact that Android HCE was detected. Because this AID does nothing, we
         * can't really dump from this.
         *
         * Unfortunately, for a reader, Android does not respond to selection commands to discover the
         * list of available apps.
         *
         * See: https://android.googlesource.com/platform/packages/apps/Nfc/+/master/src/com/android/nfc/cardemulation/HostEmulationManager.java
         */
        fun dumpTag(appData: ISO7816Application.ISO7816Info,
                    feedbackInterface: TagReaderFeedbackInterface): AndroidHCEApplication {
            feedbackInterface.updateStatusText(Utils.localizeString(R.string.android_hce_reading))
            return AndroidHCEApplication(appData)
        }
    }
}
