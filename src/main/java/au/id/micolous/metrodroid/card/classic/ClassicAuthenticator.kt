package au.id.micolous.metrodroid.card.classic

import android.content.Context
import android.util.Log
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.key.CardKeysRetriever
import au.id.micolous.metrodroid.key.ClassicKeys
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.key.ClassicStaticKeys
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import java.io.IOException

class ClassicAuthenticator private constructor(private val mKeys: ClassicKeys,
                                               private val isFallback: Boolean,
                                               private val isDynamic: Boolean,
                                               private val maxProgress: Int,
                                               private val mRetryLimit: Int = Preferences.mfcAuthRetry,
                                               private val mPreferredBundles: MutableList<String> = mutableListOf()
                                               ) {
    private fun tryKey(tech: ClassicCardTech,
                       sectorIndex: Int,
                       sectorKey: ClassicSectorKey): Boolean {
        if (!tech.authenticate(sectorIndex, sectorKey))
            return false
        Log.d(TAG, "Authenticatied on sector $sectorIndex, bundle ${sectorKey.bundle}")
        if (!mPreferredBundles.contains(sectorKey.bundle))
            mPreferredBundles.add(sectorKey.bundle)
        return true
    }

    private fun tryCandidatesSub(tech: ClassicCardTech,
                                 sectorIndex: Int,
                                 candidates: Collection<ClassicSectorKey>): ClassicSectorKey? {
        candidates.forEach { sectorKey ->
            if (tryKey(tech, sectorIndex, sectorKey))
                return sectorKey
        }
        return null
    }

    private fun tryCandidates(tech: ClassicCardTech,
                              sectorIndex: Int,
                              candidates: Collection<ClassicSectorKey>,
                              keyType: ClassicSectorKey.KeyType): ClassicSectorKey? {
        if (keyType == ClassicSectorKey.KeyType.UNKNOWN) {
            tryCandidatesSub(tech, sectorIndex, candidates.map { it.canonType() })?.let { return it }
            tryCandidatesSub(tech, sectorIndex, candidates.map { it.invertType() })?.let { return it }
        } else {
            tryCandidatesSub(tech, sectorIndex, candidates.map { it.updateType(keyType) })?.let { return it }
        }
        return null
    }


    @Throws(IOException::class)
    fun authenticate(tech: ClassicCardTech,
                     feedbackInterface: TagReaderFeedbackInterface,
                     sectorIndex: Int,
                     expectDynamic: Boolean,
                     keyType: ClassicSectorKey.KeyType): ClassicSectorKey? {
        val failFast = (!isDynamic && expectDynamic)

        feedbackInterface.updateProgressBar(sectorIndex * 5, maxProgress)

        if (!isFallback) {
            feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfc_have_key, sectorIndex))
        } else {
            feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfc_default_key, sectorIndex))
        }

        val tries = if (failFast) 1 else mRetryLimit
        // Try to authenticate with the sector multiple times, in case we have
        // impaired communications with the card.
        repeat(tries) { tryNum ->
            // If we have a known key for the sector on the card, try this first.
            Log.d(TAG, "Attempting authentication on sector $sectorIndex, try number $tryNum...")
            tryCandidates(tech, sectorIndex, mKeys.getCandidates(sectorIndex,
                    mPreferredBundles), keyType)?.let { return it }
        }

        // Try with the other keys, unless we know that keys are likely to be dynamic
        if (failFast)
            return null

        feedbackInterface.updateProgressBar(sectorIndex * 5 + 2, maxProgress)

        repeat (mRetryLimit) {tryNum ->
            Log.d(TAG, "Attempting authentication with other keys on sector $sectorIndex, try number $tryNum...")

            // Attempt authentication with alternate keys
            feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfc_other_key, sectorIndex))

            // Be a little more forgiving on the key list.  Lets try all the keys!
            //
            // This takes longer, of course, but means that users aren't scratching
            // their heads when we don't get the right key straight away.
            tryCandidates(tech, sectorIndex, mKeys.allKeys, keyType)?.let {
                Log.d(TAG, String.format("Authenticated successfully to sector %d with other key. "
                        + "Fix the key file to speed up authentication", sectorIndex))
                return it
            }
        }

        //noinspection StringConcatenation
        Log.d(TAG, "Authentication unsuccessful for sector $sectorIndex, giving up")

        return null
    }

    companion object {
        private const val TAG = "ClassicAuthenticator"

        fun makeAuthenticator(tagId: ImmutableByteArray,
                              retriever: CardKeysRetriever,
                              context: Context,
                              maxProgress: Int): ClassicAuthenticator {
            (retriever.forTagID(context, tagId) as? ClassicKeys)?.let {
                return ClassicAuthenticator(it, isFallback = false, isDynamic = true, maxProgress = maxProgress)
            }
            ClassicStaticKeys.forStaticClassic(retriever = retriever, context = context)?.let {
                return ClassicAuthenticator(it, isFallback = false, isDynamic = false, maxProgress = maxProgress)
            }

            return ClassicAuthenticator(ClassicStaticKeys.fallback(), isFallback = true,
                    isDynamic = false, maxProgress = maxProgress)
        }
    }
}
