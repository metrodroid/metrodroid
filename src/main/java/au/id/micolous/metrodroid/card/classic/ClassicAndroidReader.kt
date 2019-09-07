/*
 * ClassicCard.kt
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

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log

import au.id.micolous.metrodroid.key.*
import au.id.micolous.metrodroid.multi.Localizer

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.util.ImmutableByteArray

import au.id.micolous.metrodroid.key.CardKeysEmbed

object ClassicAndroidReader {
    private const val TAG = "ClassicAndroidReader"
    private val devicesMifareWorks = setOf(
            // Devices which do **not** declare "com.nxp.mifare" feature, but have an NXP NFC chipset.
            // Google (both regular and XL)
            // https://issuetracker.google.com/issues/135168804
            "Pixel 2",
            "Pixel 3",
            "Pixel 3a",

            // Oppo
            "Find7"
    )
    private val devicesMifareNotWorks = setOf<String>()

    val mifareClassicSupport: Boolean? by lazy {
            try {
                detectMfcSupport()
            } catch (e: Exception) {
                Log.w(TAG, "Detecting nfc support failed", e)
                null
            }
        }

    fun getKeyRetrieverEmbed(context: Context): CardKeysFromFiles = CardKeysEmbed(context, "keys")

    private fun detectMfcSupport(): Boolean {
        if (android.os.Build.MODEL in devicesMifareNotWorks) {
            return false
        }

        if (android.os.Build.MODEL in devicesMifareWorks) {
            return true
        }

        // TODO: Some devices report MIFARE Classic support, when they actually don't have it.
        //
        // Detecting based on libraries and device nodes doesn't work great either. There's edge
        // cases, and it's still vulnerable to vendors doing silly things.

        // Fallback: Look for com.nxp.mifare feature.
        val ret = MetrodroidApplication.instance.packageManager.hasSystemFeature("com.nxp.mifare")

        Log.d(TAG, "Falling back to com.nxp.mifare feature detection " + if (ret) "(found)" else "(missing)")

        return ret
    }

    fun getKeyRetriever(context: Context): CardKeysMerged = CardKeysMerged(listOf(
                getKeyRetrieverEmbed(context),
                CardKeysDB(context)
        ))

    suspend fun dumpTag(tagId: ImmutableByteArray, tag: Tag, feedbackInterface: TagReaderFeedbackInterface): ClassicCard {
        feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfc_reading))
        feedbackInterface.showCardType(null)

        var tech: MifareClassic? = null

        try {
            tech = ClassicPatcher.getTech(tag)
            tech!!.connect()

            val techWrapper = ClassicCardTechAndroid(tech, tagId)

            val keyRetriever = getKeyRetriever(MetrodroidApplication.instance)

            return ClassicReader.readCard(
                    keyRetriever, techWrapper, feedbackInterface)
        } finally {
            if (tech != null && tech.isConnected) {
                tech.close()
            }
        }
    }

    suspend fun dumpPlus(tag: CardTransceiver, feedbackInterface: TagReaderFeedbackInterface,
                         atqa: Int, sak: Short): ClassicCard? {
        feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfp_reading))
        feedbackInterface.showCardType(null)

        val keyRetriever = getKeyRetriever(MetrodroidApplication.instance)
        return ClassicReader.readPlusCard(keyRetriever, tag, feedbackInterface, atqa, sak)
    }
}
