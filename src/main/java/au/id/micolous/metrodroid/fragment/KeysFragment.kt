@file:UseExperimental(ExperimentalStdlibApi::class)
/*
 * CardKeysFragment.kt
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.StringRes
import androidx.fragment.app.ListFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.activity.AddKeyActivity
import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader
import au.id.micolous.metrodroid.key.*
import au.id.micolous.metrodroid.key.KeyFormat
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.provider.CardKeyProvider
import au.id.micolous.metrodroid.provider.KeysTableColumns
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.util.BetterAsyncTask
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.annotations.NonNls

class KeysFragment : ListFragment(), AdapterView.OnItemLongClickListener {
    private var mActionMode: ActionMode? = null
    private var mActionKeyId: Int = 0

    private val mActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.keys_contextual, menu)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                menu.findItem(R.id.export_key).isVisible = false
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == R.id.delete_key) {
                if (Preferences.hideCardNumbers) {
                    AlertDialog.Builder(activity)
                            .setTitle(R.string.cant_delete_with_obfuscation)
                            .setMessage(R.string.cant_delete_with_obfuscation_message)
                            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                            .show()
                    return true
                }

                var keys: CardKeys? = null
                try {
                    keys = CardKeysDB(MetrodroidApplication.instance).forID(mActionKeyId)
                } catch (e: Exception) {
                    Log.d(TAG, "error in deleting key?")
                }

                val deleteMessage = Localizer.localizeString(R.string.delete_key_confirm_message,
                            keys?.description ?: "??", keys?.fileType ?: "??")

                AlertDialog.Builder(activity)
                        .setTitle(R.string.delete_key_confirm_title)
                        .setMessage(deleteMessage)
                        .setPositiveButton(R.string.delete) { dialog, _ ->
                            object : BetterAsyncTask<Void?>(requireActivity(), false, false) {
                                override fun doInBackground(): Void? {
                                    val uri = ContentUris.withAppendedId(CardKeyProvider.CONTENT_URI, mActionKeyId.toLong())
                                    requireActivity().contentResolver.delete(uri, null, null)
                                    return null
                                }

                                override fun onResult(result: Void?) {
                                    mActionMode!!.finish()
                                    (listAdapter as KeysAdapter).notifyDataSetChanged()
                                }
                            }.execute()
                            dialog.dismiss()
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                        .show()
                return true
            } else if (item.itemId == R.id.export_key) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = "application/json"
                    i.putExtra(Intent.EXTRA_TITLE, STD_EXPORT_FILENAME)

                    startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE)
                }
            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            mActionKeyId = 0
            mActionMode = null
        }
    }

    private val mLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
            return KeyLoader(MetrodroidApplication.instance)
        }

        override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor?) {
            (listView.adapter as CursorAdapter).swapCursor(cursor)
            setListShown(true)
        }

        override fun onLoaderReset(cursorLoader: Loader<Cursor>) {}
    }

    private class KeyLoader internal constructor(context: Context) : CursorLoader(context, CardKeyProvider.CONTENT_URI, null, null, null, KeysTableColumns.CREATED_AT + " DESC") {

        private fun list2Cursor(list: List<CardKeysFromFiles.CardKeyRead>): Cursor {
            val cur = MatrixCursor(arrayOf(KeysTableColumns._ID, KeysTableColumns.CARD_ID, KeysTableColumns.CARD_TYPE, KeysTableColumns.KEY_DATA))
            for ((id, tagId, cardType, keyData) in list) {
                cur.addRow(arrayOf(id, tagId, cardType, keyData))
            }

            return cur
        }

        override fun loadInBackground(): Cursor? {
            val cursor: Cursor? = super.loadInBackground()
            val embedList = ClassicAndroidReader.getKeyRetrieverEmbed(context).getKeyList()
            if (embedList.isEmpty())
                return cursor
            val embedCursor = list2Cursor(embedList)
            return MergeCursor(arrayOf(cursor, embedCursor))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEmptyText(getString(R.string.no_keys))
        listView.onItemLongClickListener = this
        listAdapter = KeysAdapter(requireActivity())
        loaderManager.initLoader(0, null, mLoaderCallbacks)
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val cursor = listAdapter?.getItem(position) as Cursor

        mActionKeyId = cursor.getInt(cursor.getColumnIndex(KeysTableColumns._ID))
        mActionMode = requireActivity().startActionMode(mActionModeCallback)

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_keys_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_key) {
            val i = Utils.getContentIntent(listOf("application/json", "application/x-extension-bin"))
            startActivityForResult(i, REQUEST_SELECT_FILE)
            return true
        } else if (item.itemId == R.id.key_more_info) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://micolous.github.io/metrodroid/key_formats")))
        }
        return false
    }

    @SuppressLint("StaticFieldLeak")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri: Uri? = data?.data
        try {
            if (resultCode == Activity.RESULT_OK && uri != null) {
                when (requestCode) {
                    REQUEST_SELECT_FILE -> {
                        val type = requireActivity().contentResolver.getType(uri)

                        Log.d(TAG, "REQUEST_SELECT_FILE content_type = $type")

                        val f: KeyFormat = Utils.detectKeyFormat(requireActivity(), uri)

                        Log.d(TAG, "Detected file format: " + f.name)

                        when (f) {
                            KeyFormat.JSON_MFC_STATIC -> {
                                // Static keys can't be prompted
                                @StringRes val err = importKeysFromStaticJSON(requireActivity(), uri)
                                if (err != 0) {
                                    Toast.makeText(activity, err, Toast.LENGTH_SHORT).show()
                                }
                            }

                            KeyFormat.JSON_MFC, KeyFormat.JSON_MFC_NO_UID, KeyFormat.RAW_MFC -> startActivity(Intent(Intent.ACTION_VIEW, uri, activity, AddKeyActivity::class.java))

                            else -> Toast.makeText(activity, R.string.invalid_key_file, Toast.LENGTH_SHORT).show()
                        }
                    }

                    REQUEST_SAVE_FILE -> {
                        Log.d(TAG, "REQUEST_SAVE_FILE")

                        object : BetterAsyncTask<Void?>(requireActivity(), false, false) {
                            override fun doInBackground(): Void? {
                                val ctxt = MetrodroidApplication.instance
                                val os = ctxt.contentResolver.openOutputStream(uri)!!

                                val keys = ClassicAndroidReader.getKeyRetriever(ctxt).forID(mActionKeyId)!!
                                val json = keys.toJSON().toString()

                                os.write(json.encodeToByteArray())
                                os.close()
                                return null

                            }

                            override fun onResult(result: Void?) {
                                Toast.makeText(MetrodroidApplication.instance, R.string.file_exported, Toast.LENGTH_SHORT).show()
                                mActionMode!!.finish()
                            }
                        }.execute()
                    }
                }
            }
        } catch (ex: Exception) {
            Utils.showError(requireActivity(), ex)
        }

    }

    private class KeysAdapter internal constructor(activity: Activity) : ResourceCursorAdapter(activity, android.R.layout.simple_list_item_2, null, false) {

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            @NonNls val id = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_ID))
            val type = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE))

            val textView1 = view.findViewById<TextView>(android.R.id.text1)
            val textView2 = view.findViewById<TextView>(android.R.id.text2)

            when (type) {
                CardKeys.TYPE_MFC_STATIC -> {
                    val keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA))
                    var desc: String? = null
                    var fileType: String? = null
                    try {
                        val k = ClassicStaticKeys.fromJSON(
                                CardSerializer.jsonPlainStable.parseToJsonElement(keyData).jsonObject,
                                "cursor/$id")
                        desc = k!!.description
                        fileType = k.fileType
                    } catch (ignored: Exception) {
                    }

                    if (desc != null) {
                        textView1.text = desc
                    } else {
                        textView1.setText(R.string.untitled_key_group)
                    }

                    if (fileType != null) {
                        textView2.text = fileType
                    } else {
                        textView2.setText(R.string.unknown)
                    }
                }
                CardKeys.TYPE_MFC -> {
                    val keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA))
                    var fileType: String? = null

                    try {
                        val k = ClassicCardKeys.fromJSON(
                                CardSerializer.jsonPlainStable.parseToJsonElement(keyData).jsonObject,
                                "cursor/$id")
                        fileType = k.fileType
                    } catch (ignored: Exception) {
                    }

                    if (Preferences.hideCardNumbers) {
                        textView1.setText(R.string.hidden_card_number)
                    } else {
                        textView1.text = id
                    }

                    if (fileType != null) {
                        textView2.text = fileType
                    } else {
                        textView2.setText(R.string.unknown)
                    }
                }
                else -> {
                    textView1.setText(R.string.unknown)
                    textView2.setText(R.string.unknown)
                }
            }

        }
    }

    companion object {
        private const val REQUEST_SELECT_FILE = 1
        private const val REQUEST_SAVE_FILE = 2

        private const val STD_EXPORT_FILENAME = "Metrodroid-Keys.json"

        private const val TAG = "KeysFragment"

        @StringRes
        private fun importKeysFromStaticJSON(activity: Activity, uri: Uri): Int {
            val stream = activity.contentResolver.openInputStream(uri) ?: return R.string.key_file_empty
            val keyData = stream.readBytes()

            try {
                val json = CardSerializer.jsonPlainStable.parseToJsonElement(String(keyData, Charsets.UTF_8))
                Log.d(TAG, "inserting key")

                // Test that we can deserialise this
                val k = ClassicStaticKeys.fromJSON(json.jsonObject, uri.path ?: "unspecified")
                if (k?.isEmpty() != false) {
                    return R.string.key_file_empty
                }

                InsertKeyTask(activity, k).execute()
                return 0
            } catch (ex: Exception) {
                Log.d(TAG, "jsonException", ex)
                return R.string.invalid_json
            }

        }
    }
}
