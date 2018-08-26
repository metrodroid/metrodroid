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

import android.app.Activity;
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

import au.id.micolous.metrodroid.activity.AddKeyActivity;
import au.id.micolous.metrodroid.provider.CardKeyProvider;
import au.id.micolous.metrodroid.provider.KeysTableColumns;
import au.id.micolous.metrodroid.util.BetterAsyncTask;
import au.id.micolous.metrodroid.util.Utils;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class KeysFragment extends ListFragment implements AdapterView.OnItemLongClickListener {
    private ActionMode mActionMode;
    private int mActionKeyId;
    private static final int REQUEST_SELECT_FILE = 1;
    private static final String TAG = "KeysFragment";

    private android.view.ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.keys_contextual, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete_key) {
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
                return true;
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
            // Some files are text/xml, some are application/xml.
            // In Android 4.4 and later, we can say the right thing!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                i.setType("*/*");
                String[] mimetypes = {"application/octet-stream", "application/x-extension-bin" };
                i.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            } else {
                // Failsafe, used in the emulator for local files
                i.setType("text/xml");
            }
            startActivityForResult(Intent.createChooser(i, Utils.localizeString(R.string.select_file)), REQUEST_SELECT_FILE);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        try {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SELECT_FILE:
                        uri = data.getData();
                        Log.d(TAG, "REQUEST_SELECT_FILE content_type = " + getActivity().getContentResolver().getType(uri));

                        startActivity(new Intent(Intent.ACTION_VIEW, uri, getActivity(), AddKeyActivity.class));
                        break;
                }
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
    }

    private class KeysAdapter extends ResourceCursorAdapter {
        public KeysAdapter() {
            super(getActivity(), android.R.layout.simple_list_item_2, null, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String id = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_ID));
            String type = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE));

            TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

            if (MetrodroidApplication.hideCardNumbers()) {
                textView1.setText(R.string.hidden_card_number);
            } else {
                textView1.setText(id);
            }
            textView2.setText(type);
        }
    }
}
