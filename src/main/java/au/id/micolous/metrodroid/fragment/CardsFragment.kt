/*
 * CardsFragment.kt
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.core.content.FileProvider
import androidx.loader.content.Loader
import android.util.Log
import android.util.Pair
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import android.widget.Toast

import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.serializers.CardImporter
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.serializers.classic.MctCardImporter
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.Preferences
import org.jetbrains.annotations.NonNls

import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

import javax.xml.parsers.ParserConfigurationException

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.activity.CardInfoActivity
import au.id.micolous.metrodroid.provider.CardDBHelper
import au.id.micolous.metrodroid.provider.CardProvider
import au.id.micolous.metrodroid.provider.CardsTableColumns
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.ExportHelper
import au.id.micolous.metrodroid.util.TripObfuscator
import au.id.micolous.metrodroid.util.Utils

class CardsFragment : ExpandableListFragment() {

    private val mLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor> {
            return CursorLoader(activity!!, CardProvider.CONTENT_URI_CARD,
                    CardDBHelper.PROJECTION,
                    null, null,
                    "${CardsTableColumns.SCANNED_AT} DESC, ${CardsTableColumns._ID} DESC")
        }

        override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
            val scans = HashMap<Pair<Int, String>, MutableList<Scan>>()
            val cards = ArrayList<Pair<Int, String>>()
            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val type = cursor.getInt(cursor
                        .getColumnIndex(CardsTableColumns.TYPE))
                val serial = cursor.getString(cursor
                        .getColumnIndex(CardsTableColumns.TAG_SERIAL))
                val id = Pair(type, serial)
                if (!scans.containsKey(id)) {
                    scans[id] = ArrayList()
                    cards.add(id)
                }
                scans[id]!!.add(Scan(cursor))
            }

            Log.d(TAG, "creating adapter " + cards.size)
            listAdapter = CardsAdapter(activity!!, scans, cards)
            setListShown(true)
            setEmptyText(getString(R.string.no_scanned_cards))
        }

        override fun onLoaderReset(cursorLoader: Loader<Cursor>) {}
    }

    private class Scan internal constructor(cursor: Cursor) {
        internal val mScannedAt: Long = cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT))
        internal val mLabel: String? = cursor.getString(cursor.getColumnIndex(CardsTableColumns.LABEL))
        internal val mType: Int = cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE))
        internal val mSerial: String = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL))
        internal val mData: String = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA))
        internal var mTransitIdentity: TransitIdentity? = null
        internal val mId: Int = cursor.getInt(cursor.getColumnIndex(CardsTableColumns._ID))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerForContextMenu(listView)
        if (listAdapter == null) {
            loaderManager.initLoader(0, null, mLoaderCallbacks).startLoading()
        }
    }

    override fun onListChildClick(parent: ExpandableListView, v: View, groupPosition: Int, childPosition: Int, id: Long): Boolean {

        Log.d(TAG, "Clicked $id $groupPosition $childPosition")
        val uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id)
        val intent = Intent(activity, CardInfoActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = uri
        startActivity(intent)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cards_menu, menu)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            var item: MenuItem? = menu.findItem(R.id.import_mct_file)
            if (item != null)
                item.isVisible = false
            item = menu.findItem(R.id.import_mfc_file)
            if (item != null)
                item.isVisible = false
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
        activity!!.menuInflater.inflate(R.menu.card_context_menu, menu)
    }

    override fun onContextItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.delete_card) {
            val id = (item.menuInfo as ExpandableListView.ExpandableListContextMenuInfo).id
            val uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id)
            activity!!.contentResolver.delete(uri, null, null)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            val clipboard: ClipboardManager?
            val xml: String
            val i: Intent
            val uri: Uri

            when (item.itemId) {
                R.id.import_clipboard -> {
                    run {
                        clipboard = activity?.getSystemService(Activity.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (clipboard == null) {
                            Toast.makeText(activity, R.string.clipboard_error, Toast.LENGTH_SHORT).show()
                            return true
                        }

                        val d = clipboard.primaryClip
                        if (d == null) {
                            Toast.makeText(activity, R.string.no_data_in_clipboard, Toast.LENGTH_SHORT).show()
                        } else {
                            val ci = d.getItemAt(0)
                            xml = ci.coerceToText(activity).toString()

                            val uris = ExportHelper.importCards(xml, XmlOrJsonCardFormat(), activity!!)

                            updateListView()
                            val it = uris.iterator()
                            onCardsImported(activity!!, uris.size, if (it.hasNext()) it.next() else null)
                        }
                    }
                    return true
                }

                R.id.import_mct_file -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        return true
                    }
                    uri = Uri.fromFile(Environment.getExternalStorageDirectory())
                    i = Intent(Intent.ACTION_GET_CONTENT)
                    i.putExtra(Intent.EXTRA_STREAM, uri)
                    i.type = "*/*"
                    val mctMimetypes = arrayOf("text/plain",
                            // Fallback for cases where we didn't get a good mime type from the
                            // OS, this allows most "other" files to be selected.
                            "application/octet-stream")
                    i.putExtra(Intent.EXTRA_MIME_TYPES, mctMimetypes)
                    startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.select_file)), REQUEST_SELECT_FILE_MCT)
                    return true
                }

                R.id.import_mfc_file -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        return true
                    }
                    uri = Uri.fromFile(Environment.getExternalStorageDirectory())
                    i = Intent(Intent.ACTION_GET_CONTENT)
                    i.putExtra(Intent.EXTRA_STREAM, uri)
                    i.type = "*/*"
                    val mfcMimetypes = arrayOf(
                            // Fallback for cases where we didn't get a good mime type from the
                            // OS, this allows most "other" files to be selected.
                            "application/octet-stream")
                    i.putExtra(Intent.EXTRA_MIME_TYPES, mfcMimetypes)
                    startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.select_file)), REQUEST_SELECT_FILE_MFC)
                    return true
                }

                R.id.import_file -> {
                    // Some files are text/xml, some are application/xml.
                    // In Android 4.4 and later, we can say the right thing!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        uri = Uri.fromFile(Environment.getExternalStorageDirectory())
                        i = Intent(Intent.ACTION_GET_CONTENT)
                        i.putExtra(Intent.EXTRA_STREAM, uri)
                        i.type = "*/*"
                        val mimetypes = arrayOf("application/xml", "application/json", "text/xml", "text/json", "text/plain", "application/zip",
                                // Fallback for cases where we didn't get a good mime type from the
                                // OS, this allows most "other" files to be selected.
                                "application/octet-stream")
                        i.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                        startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.select_file)), REQUEST_SELECT_FILE)
                    } else {
                        // Failsafe, used in the emulator for local files
                        ReadTask(this).execute(Uri.fromFile(File(SD_IMPORT_PATH)))
                    }
                    return true
                }

                R.id.deduplicate_cards -> {
                    DedupTask(activity!!).execute()
                    return true
                }

                R.id.export_all -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        i.addCategory(Intent.CATEGORY_OPENABLE)
                        i.type = "application/zip"
                        i.putExtra(Intent.EXTRA_TITLE, STD_EXPORT_FILENAME)
                        startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE)
                    } else {
                        val file = File(SD_EXPORT_PATH)
                        ExportHelper.exportCardsZip(file.outputStream(), activity!!)
                        Toast.makeText(activity, R.string.saved_metrodroid_zip, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            }
        } catch (ex: Exception) {
            Utils.showError(activity!!, ex)
        }

        return false
    }

    private class DedupTask internal constructor(context: Context) : AsyncTask<Void, Int, Pair<String, Int>>() {
        private val mContext: WeakReference<Context> = WeakReference(context)

        override fun doInBackground(vararg voids: Void): Pair<String, Int> {
            try {
                val tf = ExportHelper.findDuplicates(MetrodroidApplication.instance)
                return if (tf == null || tf.isEmpty()) Pair<String, Int>(null, 0) else Pair<String, Int>(null, ExportHelper.deleteSet(MetrodroidApplication.instance,
                        tf))
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return Pair<String, Int>(Utils.getErrorMessage(ex), null)
            }

        }

        override fun onPostExecute(res: Pair<String, Int>) {
            val err = res.first
            val tf = res.second
            val context = mContext.get() ?: return

            if (err != null) {
                AlertDialog.Builder(context)
                        .setMessage(err)
                        .show()
                return
            }

            Toast.makeText(context,
                    Localizer.localizePlural(R.plurals.cards_deduped, tf!!, tf),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private class SaveTask : AsyncTask<Uri, Int, String>() {

        override fun doInBackground(vararg uris: Uri): String? {
            try {
                val os = MetrodroidApplication.instance.contentResolver.openOutputStream(uris[0])
                        ?: return "openOutputStream failed"
                ExportHelper.exportCardsZip(os, MetrodroidApplication.instance)
                os.close()
                return null
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return Utils.getErrorMessage(ex)
            }

        }

        override fun onPostExecute(err: String?) {
            if (err == null) {
                Toast.makeText(MetrodroidApplication.instance, R.string.saved_xml_custom, Toast.LENGTH_SHORT).show()
                return
            }
            AlertDialog.Builder(MetrodroidApplication.instance)
                    .setMessage(err)
                    .show()
        }
    }

    private abstract class CommonReadTask internal constructor(cardsFragment: CardsFragment,
                                                               private val mCardImporter: CardImporter) : AsyncTask<Uri, Int, Pair<String, Collection<Uri>>>() {
        private val mCardsFragment: WeakReference<CardsFragment> = WeakReference(cardsFragment)

        override fun doInBackground(vararg uris: Uri): Pair<String, Collection<Uri>> {
            try {
                val cr = MetrodroidApplication.instance.contentResolver

                Log.d(TAG, "REQUEST_SELECT_FILE content_type = " + cr.getType(uris[0])!!)
                val stream = cr.openInputStream(uris[0])!!
// Will be handled by exception handler below...

                val iuri = ExportHelper.importCards(
                        stream, mCardImporter, MetrodroidApplication.instance)

                return Pair<String, Collection<Uri>>(null, iuri)
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return Pair<String, Collection<Uri>>(Utils.getErrorMessage(ex), null)
            }

        }

        override fun onPostExecute(res: Pair<String, Collection<Uri>>) {
            val err = res.first
            val uris = res.second
            val cf = mCardsFragment.get() ?: return

            if (err == null) {
                cf.updateListView()
                val it = uris.iterator()
                onCardsImported(cf.activity!!, uris.size, if (it.hasNext()) it.next() else null)
                return
            }
            AlertDialog.Builder(cf.activity)
                    .setMessage(err)
                    .show()
        }
    }


    private class ReadTask @Throws(ParserConfigurationException::class) internal constructor(cardsFragment: CardsFragment) : CommonReadTask(cardsFragment, XmlOrJsonCardFormat())

    private class MCTReadTask internal constructor(cardsFragment: CardsFragment) : CommonReadTask(cardsFragment, MctCardImporter())

    private class MFCReadTask internal constructor(cardsFragment: CardsFragment) : CommonReadTask(cardsFragment, MfcCardImporter())

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri: Uri?
        try {
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    REQUEST_SELECT_FILE -> {
                        uri = data?.data
                        ReadTask(this).execute(uri)
                    }

                    REQUEST_SELECT_FILE_MCT -> {
                        uri = data?.data
                        MCTReadTask(this).execute(uri)
                    }

                    REQUEST_SELECT_FILE_MFC -> {
                        uri = data?.data
                        MFCReadTask(this).execute(uri)
                    }

                    REQUEST_SAVE_FILE -> {
                        uri = data?.data
                        Log.d(TAG, "REQUEST_SAVE_FILE")
                        SaveTask().execute(uri)
                    }
                }
            }
        } catch (ex: Exception) {
            Utils.showError(activity!!, ex)
        }

    }

    private fun updateListView() {
        (view!!.findViewById<ExpandableListView>(android.R.id.list).expandableListAdapter as CardsAdapter).notifyDataSetChanged()
    }

    private class CardsAdapter internal constructor(ctxt: Context,
                                                    private val mScans: Map<Pair<Int, String>, List<Scan>>,
                                                    private val mCards: List<Pair<Int, String>>) : BaseExpandableListAdapter() {
        private val mLayoutInflater: LayoutInflater

        init {
            Log.d(TAG, "Cards adapter " + mCards.size)
            mLayoutInflater = LayoutInflater.from(ctxt)
        }

        override fun getGroupCount(): Int {
            Log.d(TAG, "getgroupcount " + mCards.size)
            return mCards.size
        }

        override fun getChildrenCount(i: Int): Int {
            return mScans[mCards[i]]!!.size
        }

        override fun getGroup(i: Int): Any? {
            Log.d(TAG, "getgroup $i")
            return mScans[mCards[i]]
        }

        override fun getChild(parent: Int, child: Int): Any {
            return mScans[mCards[parent]]!![child]
        }

        override fun getGroupId(i: Int): Long = (i.toLong() + 0x1000000)

        override fun getChildId(parent: Int, child: Int): Long {
            return mScans[mCards[parent]]!![child].mId.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(group: Int, isExpanded: Boolean, convertViewReuse: View?, parent: ViewGroup): View {
            val convertView = convertViewReuse ?: mLayoutInflater.inflate(R.layout.card_name_header,
                        parent, false)
            val scan = mScans[mCards[group]]!![0]
            val type = scan.mType
            val serial = scan.mSerial
            val label = scan.mLabel

            if (scan.mTransitIdentity == null) {
                try {
                    scan.mTransitIdentity = CardSerializer.fromDb(scan.mData)!!.parseTransitIdentity()
                } catch (ex: Exception) {
                    val error = String.format("Error: %s", Utils.getErrorMessage(ex))
                    scan.mTransitIdentity = TransitIdentity(error, null)
                }

            }

            val identity = scan.mTransitIdentity

            val textView1 = convertView!!.findViewById<TextView>(android.R.id.text1)
            val textView2 = convertView.findViewById<TextView>(android.R.id.text2)

            if (identity != null) {
                textView1.text = identity.name
                if (label?.isEmpty() == false) {
                    // This is used for imported cards from mfcdump_to_farebotxml.py
                    // Used for development and testing. We should always show this.
                    textView2.text = label
                } else if (Preferences.hideCardNumbers) {
                    textView2.text = ""
                    textView2.visibility = View.GONE
                    // User doesn't want to show any card numbers.
                } else {
                    // User wants to show card numbers (default).
                    if (identity.serialNumber != null) {
                        textView2.text = identity.serialNumber
                    } else {
                        // Fall back to showing the serial number of the NFC chip.
                        textView2.text = serial
                    }
                }
            } else {
                textView1.setText(R.string.unknown_card)
                if (Preferences.hideCardNumbers) {
                    textView2.text = CardType.parseValue(type).toString()
                } else {
                    textView2.text = "${CardType.parseValue(type)} - $serial"
                }
            }
            return convertView
        }

        override fun getChildView(parent: Int, child: Int, isLast: Boolean, convertViewReuse: View?, viewGroup: ViewGroup): View {
            val convertView = convertViewReuse ?: mLayoutInflater.inflate(R.layout.card_scan_item,
                        viewGroup, false)
            val scan = mScans[mCards[parent]]!![child]
            val scannedAt = TripObfuscator.maybeObfuscateTS(TimestampFull(scan.mScannedAt, MetroTimeZone.LOCAL))

            val textView1 = convertView.findViewById<TextView>(android.R.id.text1)
            textView1.text = Localizer.localizeString(R.string.scanned_at_format,
                    TimestampFormatter.timeFormat(scannedAt),
                    TimestampFormatter.dateFormat(scannedAt))

            return convertView
        }

        override fun isChildSelectable(i: Int, i1: Int): Boolean = true
    }

    companion object {
        private const val TAG = "CardsFragment"
        private const val REQUEST_SELECT_FILE = 1
        private const val REQUEST_SAVE_FILE = 2
        private const val REQUEST_SELECT_FILE_MCT = 3
        private const val REQUEST_SELECT_FILE_MFC = 4
        @NonNls
        private const val STD_EXPORT_FILENAME = "Metrodroid-Export.zip"
        private val SD_EXPORT_PATH = Environment.getExternalStorageDirectory().toString() + "/" + STD_EXPORT_FILENAME
        @NonNls
        private const val STD_IMPORT_FILENAME = "Metrodroid-Import.zip"
        private val SD_IMPORT_PATH = Environment.getExternalStorageDirectory().toString() + "/" + STD_IMPORT_FILENAME

        private fun onCardsImported(ctx: Context, uriCount: Int, firstUri: Uri?) {
            Toast.makeText(ctx, Localizer.localizePlural(
                    R.plurals.cards_imported, uriCount, uriCount), Toast.LENGTH_SHORT).show()
            if (uriCount == 1 && firstUri != null) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, firstUri))
            }
        }
    }
}
