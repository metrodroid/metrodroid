/*
 * ClassicAuthenticator.kt
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.key.CardKeysRetriever
import au.id.micolous.metrodroid.key.ClassicKeys
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.key.ClassicStaticKeys
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Preferences

class ClassicAuthenticator internal constructor(private val mKeys: ClassicKeys,
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
                Log.d(TAG, "Authenticated successfully to sector $sectorIndex with other key. "
                        + "Fix the key file to speed up authentication")
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
                              maxProgress: Int): ClassicAuthenticator {
            (retriever.forTagID(tagId) as? ClassicKeys)?.let {
                return ClassicAuthenticator(it, isFallback = false, isDynamic = true, maxProgress = maxProgress)
            }
            retriever.forClassicStatic()?.let {
                return ClassicAuthenticator(it, isFallback = false, isDynamic = false, maxProgress = maxProgress)
            }

            return ClassicAuthenticator(ClassicStaticKeys.fallback(), isFallback = true,
                    isDynamic = false, maxProgress = maxProgress)
        }

    }
}
