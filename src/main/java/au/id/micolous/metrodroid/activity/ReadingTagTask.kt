package au.id.micolous.metrodroid.activity

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.nfc.Tag
import android.nfc.TagLostException
import androidx.preference.PreferenceManager
import android.util.Log
import au.id.micolous.farebot.BuildConfig
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardReader
import au.id.micolous.metrodroid.card.UnsupportedTagException
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.provider.CardProvider
import au.id.micolous.metrodroid.provider.CardsTableColumns
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.safeShow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import java.util.GregorianCalendar

internal class ReadingTagTask private constructor(
        private val readingTagActivity: ReadingTagActivity,
        private val tag: Tag): CoroutineScope  {
    override val coroutineContext = Job()

    fun doInBackground(): Pair<Uri?,Boolean> {
        val card = CardReader.dumpTag(tag, readingTagActivity)

        readingTagActivity.updateStatusText(Localizer.localizeString(R.string.saving_card))

        val cardXml = CardSerializer.toPersist(card)

        if (BuildConfig.DEBUG) {
            if (card.isPartialRead) {
                Log.w(TAG, "Partial card read.")
            } else {
                Log.i(TAG, "Finished dumping card.")
            }
            for (line in cardXml.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {

                Log.d(TAG, "Persist: $line")
            }
        }

        val tagIdString = card.tagId.toHexString()

        val values = ContentValues()
        values.put(CardsTableColumns.TYPE, card.cardType.toInteger())
        values.put(CardsTableColumns.TAG_SERIAL, tagIdString)
        values.put(CardsTableColumns.DATA, cardXml)
        values.put(CardsTableColumns.SCANNED_AT, card.scannedAt.timeInMillis)
        values.put(CardsTableColumns.LABEL, card.label)

        val uri = readingTagActivity.contentResolver.insert(CardProvider.CONTENT_URI_CARD, values)

        val prefs = PreferenceManager.getDefaultSharedPreferences(readingTagActivity).edit()
        prefs.putString(Preferences.PREF_LAST_READ_ID, tagIdString)
        prefs.putLong(Preferences.PREF_LAST_READ_AT, GregorianCalendar.getInstance().timeInMillis)
        prefs.apply()

        return Pair(uri, card.isPartialRead)
    }

    private fun showCard(cardUri: Uri?) {
        if (cardUri == null)
            return
        val intent = Intent(Intent.ACTION_VIEW, cardUri)
        intent.putExtra(CardInfoActivity.SPEAK_BALANCE_EXTRA, true)
        readingTagActivity.startActivity(intent)
        readingTagActivity.finish()
    }

    fun onPostExecute(cardUri: Uri?, exception: Exception?, isPartialRead: Boolean) {
        if (isPartialRead) {
            AlertDialog.Builder(readingTagActivity)
                    .setTitle(R.string.card_partial_read_title)
                    .setMessage(R.string.card_partial_read_desc)
                    .setCancelable(false)
                    .setPositiveButton(R.string.show_partial_data) { _, _ -> showCard(cardUri) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> readingTagActivity.finish() }
                    .safeShow()
            return
        }

        if (exception == null && cardUri != null) {
            showCard(cardUri)
            return
        }

        when (exception) {
            is TagLostException -> {
                // Tag was lost. Just drop out silently.
            }
            is UnsupportedTagException -> AlertDialog.Builder(readingTagActivity)
                    .setTitle(R.string.unsupported_tag)
                    .setMessage(exception.dialogMessage)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ -> readingTagActivity.finish() }
                    .safeShow()
            else -> Utils.showErrorAndFinish(readingTagActivity, exception)
        }
    }

    companion object {
        private val TAG = ReadingTagTask::class.java.simpleName

        fun doRead(readingTagActivity: ReadingTagActivity, tag: Tag) {
            ReadingTagTask(readingTagActivity, tag).start()
        }
    }

    private fun start() {
        launch {
            var res: Pair<Uri?, Boolean>? = null
            var ex: Exception? = null
            try {
                res = doInBackground()
            } catch (e: Exception) {
                ex = e
            }

            readingTagActivity.runOnUiThread { onPostExecute(res?.first, ex, res?.second ?: false) }
        }
    }
}
