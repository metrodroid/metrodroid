package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.VisibleForTesting
import au.id.micolous.metrodroid.transit.TransitData

actual object Preferences {
    actual val obfuscateBalance: Boolean = false
    actual val obfuscateTripFares: Boolean = false
    actual val hideCardNumbers: Boolean = false
    actual val obfuscateTripDates: Boolean = false
    actual val convertTimezone: Boolean = false
    actual val mfcFallbackReader: String = ""
    actual val mfcAuthRetry: Int = 5
    actual val retrieveLeapKeys: Boolean = false
    actual val obfuscateTripTimes: Boolean = false
    actual val debugSpans: Boolean = false
    actual val localisePlaces: Boolean = false

    @VisibleForTesting
    var languageActual = "en"

    actual var showRawStationIds = true
    actual val language: String
        get() = languageActual
    actual val region: String?
        get() = null
    actual var showBothLocalAndEnglish = true
    actual val rawLevel: TransitData.RawLevel
        get() = TransitData.RawLevel.UNKNOWN_ONLY
    actual val metrodroidVersion: String
        get() = "testing"
}
