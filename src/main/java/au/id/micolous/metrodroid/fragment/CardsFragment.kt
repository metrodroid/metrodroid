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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.format.Formatter
import android.util.Log
import android.util.Pair
import android.view.*
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.activity.CardInfoActivity
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.provider.CardDBHelper
import au.id.micolous.metrodroid.provider.CardProvider
import au.id.micolous.metrodroid.provider.CardsTableColumns
import au.id.micolous.metrodroid.serializers.CardMultiImportAdapter
import au.id.micolous.metrodroid.serializers.CardMultiImporter
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.serializers.classic.MctCardImporter
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PushbackInputStream
import java.lang.ref.WeakReference
import kotlin.coroutines.suspendCoroutine

class CardsFragment : ExpandableListFragment(), SearchView.OnQueryTextListener {
    private var searchText: String? = null
    override fun onQueryTextSubmit(query: String?): Boolean {
        searchText = query
        Log.d(TAG, "search submit $query")
        (requireView().findViewById<ExpandableListView>(android.R.id.list).expandableListAdapter as CardsAdapter).filter(searchText)
        return true

    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchText = newText
        Log.d(TAG, "search change $newText")
        (requireView().findViewById<ExpandableListView>(android.R.id.list).expandableListAdapter as CardsAdapter).filter(searchText)
        return true
    }

    private val mLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor> {
            return CursorLoader(requireActivity(), CardProvider.CONTENT_URI_CARD,
                    CardDBHelper.PROJECTION,
                    null, null,
                    "${CardsTableColumns.SCANNED_AT} DESC, ${CardsTableColumns._ID} DESC")
        }

        override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor?) {
            if (cursor == null)
                return
            val scans = mutableMapOf<CardId, MutableList<Scan>>()
            val cards = ArrayList<CardId>()
            val reverseCards = mutableMapOf<CardId, Int>()
            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val type = cursor.getInt(cursor
                        .getColumnIndexOrThrow(CardsTableColumns.TYPE))
                val serial = cursor.getString(cursor
                        .getColumnIndexOrThrow(CardsTableColumns.TAG_SERIAL))
                val id = CardId(type, serial)
                if (!scans.containsKey(id)) {
                    scans[id] = ArrayList()
                    cards.add(id)
                    reverseCards[id] = cards.size - 1
                }
                scans[id]!!.add(Scan(cursor))
            }

            Log.d(TAG, "creating adapter " + cards.size)
            listAdapter = CardsAdapter(requireActivity(), scans, cards, reverseCards)
            setListShown(true)
            setEmptyText(getString(R.string.no_scanned_cards))
        }

        override fun onLoaderReset(cursorLoader: Loader<Cursor>) {}
    }

    private class Scan(cursor: Cursor) {
        val mScannedAt: Long = cursor.getLong(cursor.getColumnIndexOrThrow(CardsTableColumns.SCANNED_AT))
        val mLabel: String? = cursor.getString(cursor.getColumnIndexOrThrow(CardsTableColumns.LABEL))
        val mType: Int = cursor.getInt(cursor.getColumnIndexOrThrow(CardsTableColumns.TYPE))
        val mSerial: String = cursor.getString(cursor.getColumnIndexOrThrow(CardsTableColumns.TAG_SERIAL))
        val mData: String = cursor.getString(cursor.getColumnIndexOrThrow(CardsTableColumns.DATA))
        val mTransitIdentity: TransitIdentity? by lazy {
            try {
                XmlOrJsonCardFormat.parseString(mData)?.parseTransitIdentity()
            } catch (ex: Exception) {
                val error = String.format("Error: %s", getErrorMessage(ex))
                TransitIdentity(error, null)
            }
        }
        val mId: Int = cursor.getInt(cursor.getColumnIndexOrThrow(CardsTableColumns._ID))

        fun matches(query: String): Boolean {
            val ti = mTransitIdentity
            val fields = listOfNotNull(
                    ti?.name,
                    ti?.serialNumber,
                    mLabel,
                    mSerial,
                    CardType.parseValue(mType).toString(),
                    if (ti == null) Localizer.localizeString(R.string.unknown_card) else null
            )
            return fields.any { it.contains(query, ignoreCase = true) }
        }
    }

    private data class CardId (val type: Int, val serial: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerForContextMenu(listView!!)
        if (listAdapter == null) {
            LoaderManager.getInstance(this).initLoader(0, null, mLoaderCallbacks).startLoading()
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
        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        requireActivity().menuInflater.inflate(R.menu.card_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.delete_card) {
            val id = (item.menuInfo as ExpandableListView.ExpandableListContextMenuInfo).id
            val uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id)
            requireActivity().contentResolver.delete(uri, null, null)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.import_clipboard -> {
                    run {
                        val clipboard = activity?.getSystemService(Activity.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (clipboard == null) {
                            Toast.makeText(activity, R.string.clipboard_error, Toast.LENGTH_SHORT).show()
                            return true
                        }

                        val d = clipboard.primaryClip
                        if (d == null) {
                            Toast.makeText(activity, R.string.no_data_in_clipboard, Toast.LENGTH_SHORT).show()
                        } else {
                            val ci = d.getItemAt(0)
                            val xml = ci.coerceToText(activity).toString()

                            val uris = ExportHelper.importCards(xml, XmlOrJsonCardFormat(), requireActivity())

                            updateListView()
                            val it = uris.iterator()
                            onCardsImported(requireActivity(), uris.size, if (it.hasNext()) it.next() else null)
                        }
                    }
                    return true
                }

                R.id.import_mct_file -> {
                    startActivityForResult(Utils.getContentIntent(listOf("text/plain")), REQUEST_SELECT_FILE_MCT)
                    return true
                }

                R.id.import_mfc_file -> {
                    startActivityForResult(Utils.getContentIntent(listOf()), REQUEST_SELECT_FILE_MFC)
                    return true
                }

                R.id.import_file -> {
                    // Some files are text/xml, some are application/xml.
                    val i = Utils.getContentIntent(
                            listOf("application/xml", "application/json", "text/xml", "text/json", "text/plain", "application/zip")
                    )
                    startActivityForResult(i, REQUEST_SELECT_FILE)
                    return true
                }

                R.id.share_xml -> {
                    ShareTask(requireActivity()).execute()
                    return true
                }

                R.id.deduplicate_cards -> {
                    DedupTask(requireActivity()).execute()
                    return true
                }

                R.id.save_xml -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        i.addCategory(Intent.CATEGORY_OPENABLE)
                        i.type = "application/zip"
                        i.putExtra(Intent.EXTRA_TITLE, STD_EXPORT_FILENAME)
                        startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE)
                    } else {
                        @Suppress("DEPRECATION")
                        val file = File(Environment.getExternalStorageDirectory().toString() + "/" + STD_EXPORT_FILENAME)
                        ExportHelper.exportCardsZip(file.outputStream(), requireActivity())
                        Toast.makeText(activity, R.string.saved_metrodroid_zip, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            }
        } catch (ex: Exception) {
            Utils.showError(requireActivity(), ex)
        }

        return false
    }

    private class DedupTask(activity: Activity) : BetterAsyncTask<Pair<String?, Int?>>(activity) {
        override fun doInBackground(): Pair<String?, Int?> {
            try {
                val tf = ExportHelper.findDuplicates(MetrodroidApplication.instance)
                if (tf.isEmpty()) return Pair(null, 0)
                return Pair(null, ExportHelper.deleteSet(MetrodroidApplication.instance, tf))
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return Pair(getErrorMessage(ex), null)
            }
        }

        override fun onResult(result: Pair<String?, Int?>?) {
            val err = result?.first
            val tf = result?.second
            val context = mWeakActivity.get() ?: return

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

    private class ShareTask (activity: Activity): BetterAsyncTask<Pair<String?, File?>>(activity) {

        override fun doInBackground(): Pair<String?, File?> {
            try {
                val folder = File(MetrodroidApplication.instance.cacheDir, "share")
                folder.mkdirs()
                val tf = File.createTempFile("cards", ".zip",
                        folder)
                val os = FileOutputStream(tf)
                ExportHelper.exportCardsZip(os, MetrodroidApplication.instance)
                os.close()
                return Pair(null, tf)
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return Pair(getErrorMessage(ex), null)
            }

        }

        override fun onResult(result: Pair<String?, File?>?) {
            val err = result?.first
            val tf = result?.second

            if (err != null || tf == null) {
                AlertDialog.Builder(MetrodroidApplication.instance)
                        .setMessage(err ?: "No File")
                        .show()
                return
            }

            val i = Intent(Intent.ACTION_SEND)
            val apkURI = FileProvider.getUriForFile(
                    MetrodroidApplication.instance,
                    MetrodroidApplication.instance.packageName + ".provider", tf)
            i.type = "application/zip"
            i.putExtra(Intent.EXTRA_STREAM, apkURI)
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.startActivity(i)
        }
    }

    private class SaveTask (activity: Activity, val uri: Uri) : BetterAsyncTask<String?>(activity) {

        override fun doInBackground(): String? {
            try {
                val os = MetrodroidApplication.instance.contentResolver.openOutputStream(uri)
                        ?: return "openOutputStream failed"
                ExportHelper.exportCardsZip(os, MetrodroidApplication.instance)
                os.close()
                return null
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return getErrorMessage(ex)
            }

        }

        override fun onResult(result: String?) {
            if (result == null) {
                Toast.makeText(MetrodroidApplication.instance, R.string.saved_xml_custom, Toast.LENGTH_SHORT).show()
                return
            }
            AlertDialog.Builder(MetrodroidApplication.instance)
                    .setMessage(result)
                    .show()
        }
    }

    class UserCancelledException : Exception()

    private abstract class CommonReadTask(cardsFragment: CardsFragment,
                                          private val mCardImporter: CardMultiImporter,
                                          val uri: Uri) : BetterAsyncTask<Pair<String?, Collection<Uri>?>?>(
            cardsFragment.requireActivity()) {
        private val mCardsFragment: WeakReference<CardsFragment> = WeakReference(cardsFragment)

        open fun verifyStream(stream: InputStream): InputStream = stream

        override fun doInBackground(): Pair<String?, Collection<Uri>?>? {
            try {
                val cr = MetrodroidApplication.instance.contentResolver

                Log.d(TAG, "REQUEST_SELECT_FILE content_type = ${cr.getType(uri)}")
                val stream = cr.openInputStream(uri)!!
                // Will be handled by exception handler below...

                val iuri = ExportHelper.importCards(
                       verifyStream(stream), mCardImporter,
                       MetrodroidApplication.instance)

                return Pair(null, iuri)
            } catch (ex: UserCancelledException) {
                return null
            } catch (ex: Exception) {
                Log.e(TAG, ex.message, ex)
                return Pair(getErrorMessage(ex), null)
            }

        }

        override fun onResult(result: Pair<String?, Collection<Uri>?>?) {
            if (result == null) {
                return
            }
            val err = result.first
            val uris = result.second
            val cf = mCardsFragment.get() ?: return

            if (err == null && uris != null) {
                cf.updateListView()
                val it = uris.iterator()
                onCardsImported(cf.requireActivity(), uris.size, if (it.hasNext()) it.next() else null)
                return
            }
            AlertDialog.Builder(cf.activity)
                    .setMessage(err ?: "No URIs")
                    .show()
        }
    }


    private class ReadTask(val cardsFragment: CardsFragment, uri: Uri)
        : CommonReadTask(cardsFragment, XmlOrJsonCardFormat(), uri) {
        override fun verifyStream(stream: InputStream): InputStream {
            val pb = PushbackInputStream(stream)
            if (pb.peek() == 'P'.code.toByte()) { // ZIP
                return pb
            }
            val l = stream.available()
            if (l < 4194304) {
                return pb
            }
            val s = runBlocking {
                suspendCoroutine<Boolean> { cont ->
                    launch(Dispatchers.Main) {
                        AlertDialog.Builder(cardsFragment.activity)
                                .setMessage(Localizer.localizeString(R.string.large_file_warning,
                                        Formatter.formatFileSize(MetrodroidApplication.instance, l.toLong())))
                                .setPositiveButton(R.string.large_file_yes) { _, _ -> cont.resumeWith(Result.success(true)) }
                                .setNegativeButton(R.string.large_file_no) { _, _ -> cont.resumeWith(Result.success(false)) }
                                .show()                
                    }
                }
            }
            if (s) {
                return pb
            }
            pb.close()
            throw UserCancelledException()
        }
    }

    private class MCTReadTask(cardsFragment: CardsFragment, uri: Uri)
        : CommonReadTask(cardsFragment, CardMultiImportAdapter (MctCardImporter()), uri)

    private class MFCReadTask(cardsFragment: CardsFragment, uri: Uri)
        : CommonReadTask(cardsFragment, CardMultiImportAdapter (MfcCardImporter()), uri)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri: Uri?
        try {
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    REQUEST_SELECT_FILE -> {
                        uri = data?.data!!
                        ReadTask(this, uri).execute()
                    }

                    REQUEST_SELECT_FILE_MCT -> {
                        uri = data?.data!!
                        MCTReadTask(this, uri).execute()
                    }

                    REQUEST_SELECT_FILE_MFC -> {
                        uri = data?.data!!
                        MFCReadTask(this, uri).execute()
                    }

                    REQUEST_SAVE_FILE -> {
                        uri = data?.data!!
                        Log.d(TAG, "REQUEST_SAVE_FILE")
                        SaveTask(requireActivity(), uri).execute()
                    }
                }
            }
        } catch (ex: Exception) {
            Utils.showError(requireActivity(), ex)
        }

    }

    private fun updateListView() {
        (requireView().findViewById<ExpandableListView>(android.R.id.list).expandableListAdapter as CardsAdapter).notifyDataSetChanged()
    }

    private class CardsAdapter(ctxt: Context,
                               private val mScans: Map<CardId, List<Scan>>,
                               private val mCards: List<CardId>,
                               private val mReverseCards: Map<CardId, Int>) : BaseExpandableListAdapter() {

        private var filteredCards: List<CardId>? = null

        private val effectiveCards: List<CardId>
            get() = filteredCards ?: mCards

        init {
            Log.d(TAG, "Cards adapter " + effectiveCards.size)
        }

        private val mLayoutInflater: LayoutInflater = LayoutInflater.from(ctxt)

        fun filter(query: String?) {
            if (query == null) {
                filteredCards = mCards
            } else {
                filteredCards = mCards.filter { mScans[it]?.get(0)?.matches(query) ?: false }
            }
            notifyDataSetChanged()
        }

        override fun getGroupCount(): Int {
            Log.d(TAG, "getGroupCount " + effectiveCards.size)
            return effectiveCards.size
        }

        override fun getChildrenCount(i: Int): Int = mScans.getValue(effectiveCards[i]).size

        override fun getGroup(i: Int): Any? {
            Log.d(TAG, "getGroup $i")
            return mScans[effectiveCards[i]]
        }

        override fun getChild(parent: Int, child: Int): Any = mScans.getValue(effectiveCards[parent])[child]

        override fun getGroupId(i: Int): Long = (mReverseCards[effectiveCards[i]] ?: i).toLong() + 0x1000000

        override fun getChildId(parent: Int, child: Int): Long = mScans.getValue(effectiveCards[parent])[child].mId.toLong()

        override fun hasStableIds(): Boolean = true

        override fun getGroupView(group: Int, isExpanded: Boolean, convertViewReuse: View?, parent: ViewGroup): View {
            val convertView = convertViewReuse ?: mLayoutInflater.inflate(R.layout.card_name_header,
                        parent, false)
            val scan = mScans.getValue(effectiveCards[group])[0]
            val type = scan.mType
            val serial = scan.mSerial
            val label = scan.mLabel

            val identity = scan.mTransitIdentity

            val textView1 = convertView!!.findViewById<TextView>(android.R.id.text1)
            val textView2 = convertView.findViewById<TextView>(android.R.id.text2)

            if (identity != null) {
                textView1.text = identity.name
                when {
                    label?.isEmpty() == false -> {
                        // This is used for imported cards from mfcdump_to_farebotxml.py
                        // Used for development and testing. We should always show this.
                        textView2.text = label
                    }
                    Preferences.hideCardNumbers -> {
                        textView2.text = ""
                        textView2.visibility = View.GONE
                        // User doesn't want to show any card numbers.
                    }
                    else -> {
                        // User wants to show card numbers (default).
                        textView2.text = Utils.weakLTR(identity.serialNumber ?: serial)
                    }
                }
            } else {
                textView1.setText(R.string.unknown_card)
                if (Preferences.hideCardNumbers) {
                    textView2.text = CardType.parseValue(type).toString()
                } else {
                    @SuppressLint("SetTextI18n")
                    textView2.text = "${CardType.parseValue(type)} - $serial"
                }
            }
            return convertView
        }

        override fun getChildView(parent: Int, child: Int, isLast: Boolean, convertViewReuse: View?, viewGroup: ViewGroup): View {
            val convertView = convertViewReuse ?: mLayoutInflater.inflate(R.layout.card_scan_item,
                        viewGroup, false)
            val scan = mScans.getValue(effectiveCards[parent])[child]
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

        private fun onCardsImported(ctx: Context, uriCount: Int, firstUri: Uri?) {
            Toast.makeText(ctx, Localizer.localizePlural(
                    R.plurals.cards_imported, uriCount, uriCount), Toast.LENGTH_SHORT).show()
            if (uriCount == 1 && firstUri != null) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, firstUri))
            }
        }
    }
}
