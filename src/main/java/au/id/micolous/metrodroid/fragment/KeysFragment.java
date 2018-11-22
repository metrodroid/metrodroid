/*
 * CardKeysFragment.java
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

package au.id.micolous.metrodroid.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.activity.AddKeyActivity;
import au.id.micolous.metrodroid.key.CardKeys;
import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicStaticKeys;
import au.id.micolous.metrodroid.key.InsertKeyTask;
import au.id.micolous.metrodroid.provider.CardKeyProvider;
import au.id.micolous.metrodroid.provider.KeysTableColumns;
import au.id.micolous.metrodroid.util.BetterAsyncTask;
import au.id.micolous.metrodroid.util.KeyFormat;
import au.id.micolous.metrodroid.util.Utils;

public class KeysFragment extends ListFragment implements AdapterView.OnItemLongClickListener {
    private ActionMode mActionMode;
    private int mActionKeyId;
    private static final int REQUEST_SELECT_FILE = 1;
    private static final int REQUEST_SAVE_FILE = 2;

    private static final String STD_EXPORT_FILENAME = "Metrodroid-Keys.json";

    private static final String TAG = "KeysFragment";

    private android.view.ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.keys_contextual, menu);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                menu.findItem(R.id.export_key).setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete_key) {
                if (MetrodroidApplication.hideCardNumbers()) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.cant_delete_with_obfuscation)
                            .setMessage(R.string.cant_delete_with_obfuscation_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                            .show();
                    return true;
                }

                ClassicCardKeys keys = null;
                try {
                    keys = CardKeys.forID(mActionKeyId);
                } catch (JSONException e) {
                    Log.d(TAG, "JSON error in deleting key?");
                }

                String deleteMessage;
                if (keys != null) {
                    deleteMessage = Utils.localizeString(R.string.delete_key_confirm_message,
                            keys.getDescription(), keys.getFileType());
                } else {
                    deleteMessage = Utils.localizeString(R.string.delete_key_confirm_message,
                            "??", "??");
                }

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.delete_key_confirm_title)
                        .setMessage(deleteMessage)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            new BetterAsyncTask<Void>(getActivity(), false, false) {
                                @Override
                                protected Void doInBackground() throws Exception {
                                    Uri uri = ContentUris.withAppendedId(CardKeyProvider.CONTENT_URI, mActionKeyId);
                                    getActivity().getContentResolver().delete(uri, null, null);
                                    return null;
                                }

                                @Override
                                protected void onResult(Void unused) {
                                    mActionMode.finish();
                                    ((KeysAdapter) getListAdapter()).notifyDataSetChanged();
                                }
                            }.execute();
                            dialog.dismiss();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                        .show();
                return true;
            } else if (item.getItemId() == R.id.export_key) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("application/json");
                    i.putExtra(Intent.EXTRA_TITLE, STD_EXPORT_FILENAME);

                    startActivityForResult(Intent.createChooser(i, Utils.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE);
                }
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionKeyId = 0;
            mActionMode = null;
        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<android.database.Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getActivity(), CardKeyProvider.CONTENT_URI,
                    null,
                    null,
                    null,
                    KeysTableColumns.CREATED_AT + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            ((CursorAdapter) getListView().getAdapter()).swapCursor(cursor);
            setListShown(true);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getString(R.string.no_keys));
        getListView().setOnItemLongClickListener(this);
        setListAdapter(new KeysAdapter());
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) getListAdapter().getItem(position);

        mActionKeyId = cursor.getInt(cursor.getColumnIndex(KeysTableColumns._ID));
        mActionMode = getActivity().startActionMode(mActionModeCallback);

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_keys_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_key) {
            Uri uri = Uri.fromFile(Environment.getExternalStorageDirectory());
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.putExtra(Intent.EXTRA_STREAM, uri);

            // In Android 4.4 and later, we can say the right thing!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                i.setType("*/*");
                String[] mimetypes = new String[]{"application/json", "application/octet-stream", "application/x-extension-bin"};
                i.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            } else {
                // Failsafe, used in the emulator for local files
                i.setType("application/octet-stream");
            }

            if (item.getItemId() == R.id.add_key)
                startActivityForResult(Intent.createChooser(i, Utils.localizeString(R.string.select_file)),
                        REQUEST_SELECT_FILE);
            return true;
        } else if (item.getItemId() == R.id.key_more_info) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://micolous.github.io/metrodroid/key_formats")));
        }
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        try {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SELECT_FILE: {
                        uri = data.getData();
                        String type = getActivity().getContentResolver().getType(uri);
                        //noinspection StringConcatenation
                        Log.d(TAG, "REQUEST_SELECT_FILE content_type = " + type);

                        KeyFormat f;
                        f = Utils.detectKeyFormat(getActivity(), uri);
                        //noinspection StringConcatenation
                        Log.d(TAG, "Detected file format: " + f.name());

                        switch (f) {
                            case JSON_MFC_STATIC:
                                // Static keys can't be prompted
                                @StringRes int err = importKeysFromStaticJSON(getActivity(), uri);
                                if (err != 0) {
                                    Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case JSON_MFC:
                            case JSON_MFC_NO_UID:
                            case RAW_MFC:
                                startActivity(new Intent(Intent.ACTION_VIEW, uri, getActivity(), AddKeyActivity.class));
                                break;

                            default:
                                Toast.makeText(getActivity(), R.string.invalid_key_file, Toast.LENGTH_SHORT).show();
                                break;
                        }

                        break;
                    }

                    case REQUEST_SAVE_FILE: {
                        Log.d(TAG, "REQUEST_SAVE_FILE");
                        uri = data.getData();
                        Objects.requireNonNull(uri);

                        new BetterAsyncTask<Void>(getActivity(), false, false) {
                            @Override
                            protected Void doInBackground() throws Exception {
                                OutputStream os = MetrodroidApplication.getInstance().getContentResolver().openOutputStream(uri);
                                Objects.requireNonNull(os);

                                ClassicCardKeys keys = CardKeys.forID(mActionKeyId);
                                Objects.requireNonNull(keys);
                                String json = keys.toJSON().toString(2);

                                IOUtils.write(json, os, Charset.defaultCharset());
                                os.close();
                                return null;

                            }

                            @Override
                            protected void onResult(Void unused) {
                                Toast.makeText(MetrodroidApplication.getInstance(), R.string.file_exported, Toast.LENGTH_SHORT).show();
                                mActionMode.finish();
                            }
                        }.execute();
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
    }

    @StringRes
    private static int importKeysFromStaticJSON(Activity activity, Uri uri) throws IOException {
        InputStream stream = activity.getContentResolver().openInputStream(uri);
        byte[] keyData = IOUtils.toByteArray(stream);

        try {
            JSONObject json = new JSONObject(new String(keyData, Utils.getUTF8()));
            Log.d(TAG, "inserting key");

            // Test that we can deserialise this
            ClassicStaticKeys k = ClassicStaticKeys.fromJSON(json);
            if (k.keys().size() == 0) {
                return R.string.key_file_empty;
            }

            new InsertKeyTask(activity, k, true).execute();
            return 0;
        } catch (JSONException ex) {
            Log.d(TAG, "jsonException", ex);
            return R.string.invalid_json;
        }
    }

    private class KeysAdapter extends ResourceCursorAdapter {
        KeysAdapter() {
            super(getActivity(), android.R.layout.simple_list_item_2, null, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String id = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_ID));
            String type = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE));

            TextView textView1 = view.findViewById(android.R.id.text1);
            TextView textView2 = view.findViewById(android.R.id.text2);

            switch (type) {
                case CardKeys.TYPE_MFC_STATIC: {
                    String keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA));
                    String desc = null;
                    String fileType = null;
                    try {
                        ClassicStaticKeys k = ClassicStaticKeys.fromJSON(new JSONObject(keyData));
                        desc = k.getDescription();
                        fileType = k.getFileType();
                    } catch (JSONException ignored) { }

                    if (desc != null) {
                        textView1.setText(desc);
                    } else {
                        textView1.setText(R.string.untitled_key_group);
                    }

                    if (fileType != null) {
                        textView2.setText(fileType);
                    } else {
                        textView2.setText(R.string.unknown);
                    }
                    break;
                }
                case CardKeys.TYPE_MFC: {
                    String keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA));
                    String fileType = null;

                    try {
                        ClassicCardKeys k = ClassicCardKeys.fromJSON(new JSONObject(keyData), KeyFormat.JSON_MFC);
                        fileType = k.getFileType();
                    } catch (JSONException ignored) { }

                    if (MetrodroidApplication.hideCardNumbers()) {
                        textView1.setText(R.string.hidden_card_number);
                    } else {
                        textView1.setText(id);
                    }

                    if (fileType != null) {
                        textView2.setText(fileType);
                    } else {
                        textView2.setText(R.string.unknown);
                    }
                    break;
                }
                default:
                    textView1.setText(R.string.unknown);
                    textView2.setText(R.string.unknown);
                    break;
            }

        }
    }
}
