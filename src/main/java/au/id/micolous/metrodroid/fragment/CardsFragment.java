/*
 * CardsFragment.java
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

package au.id.micolous.metrodroid.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.Preferences;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NonNls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardImporter;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.XmlCardFormat;
import au.id.micolous.metrodroid.card.classic.MctCardImporter;
import au.id.micolous.metrodroid.provider.CardDBHelper;
import au.id.micolous.metrodroid.provider.CardProvider;
import au.id.micolous.metrodroid.provider.CardsTableColumns;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.ExportHelper;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class CardsFragment extends ExpandableListFragment {
    private static final String TAG = "CardsFragment";
    private static final int REQUEST_SELECT_FILE = 1;
    private static final int REQUEST_SAVE_FILE = 2;
    private static final int REQUEST_SELECT_FILE_MCT = 3;
    @NonNls
    private static final String STD_EXPORT_FILENAME = "Metrodroid-Export.xml";
    private static final String SD_EXPORT_PATH = Environment.getExternalStorageDirectory() + "/" + STD_EXPORT_FILENAME;
    @NonNls
    private static final String STD_IMPORT_FILENAME = "Metrodroid-Import.xml";
    private static final String SD_IMPORT_PATH = Environment.getExternalStorageDirectory() + "/" + STD_IMPORT_FILENAME;

    private static class Scan {
        private final long mScannedAt;
        private final String mLabel;
        private final int mType;
        private final String mSerial;
        private final String mData;
        private TransitIdentity mTransitIdentity;
        private final int mId;

        Scan(Cursor cursor) {
            mId = cursor.getInt(cursor.getColumnIndex(CardsTableColumns._ID));
            mType = cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE));
            mSerial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL));
            mScannedAt = cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT));
            mLabel = cursor.getString(cursor.getColumnIndex(CardsTableColumns.LABEL));
            mData = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
            return new CursorLoader(getActivity(), CardProvider.CONTENT_URI_CARD,
                    CardDBHelper.PROJECTION,
                    null,
                    null,
                    CardsTableColumns.SCANNED_AT + " DESC, " + CardsTableColumns._ID + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            Map<Pair<Integer, String>, List<Scan>> scans = new HashMap<>();
            List<Pair<Integer, String>> cards = new ArrayList<>();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                Integer type = cursor.getInt(cursor
                        .getColumnIndex(CardsTableColumns.TYPE));
                String serial = cursor.getString(cursor
                        .getColumnIndex(CardsTableColumns.TAG_SERIAL));
                Pair<Integer, String> id = new Pair<>(type, serial);
                if (!scans.containsKey(id)) {
                    scans.put(id, new ArrayList<>());
                    cards.add(id);
                }
                scans.get(id).add(new Scan(cursor));
            }
            //noinspection StringConcatenation
            Log.d(TAG, "creating adapter " + cards.size());
            setListAdapter(new CardsAdapter(getActivity(), scans, cards));
            setListShown(true);
            setEmptyText(getString(R.string.no_scanned_cards));
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

        registerForContextMenu(getListView());
        if (getListAdapter() == null) {
            getLoaderManager().initLoader(0, null, mLoaderCallbacks).startLoading();
        }
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        //noinspection StringConcatenation
        Log.d(TAG, "Clicked " + id + " " + groupPosition + " " + childPosition);
        Uri uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id);
        Intent intent = new Intent(getActivity(), CardInfoActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        startActivity(intent);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cards_menu, menu);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            MenuItem item = menu.findItem(R.id.import_mct_file);
            if (item != null)
                item.setVisible(false);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.card_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.delete_card) {
            long id = ((ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo()).id;
            Uri uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id);
            getActivity().getContentResolver().delete(uri, null, null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            ClipboardManager clipboard;
            String xml;
            Intent i;
            Uri uri;

            switch (item.getItemId()) {
                case R.id.import_clipboard: {
                    clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                    if (clipboard == null) {
                        Toast.makeText(getActivity(), R.string.clipboard_error, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    ClipData d = clipboard.getPrimaryClip();
                    if (d == null) {
                        Toast.makeText(getActivity(), R.string.no_data_in_clipboard, Toast.LENGTH_SHORT).show();
                    } else {
                        ClipData.Item ci = d.getItemAt(0);
                        xml = ci.coerceToText(getActivity()).toString();

                        Collection<Uri> uris = ExportHelper.importCards(xml, new XmlCardFormat(), getActivity());

                        updateListView();
                        Iterator<Uri> it = uris.iterator();
                        onCardsImported(getActivity(), uris.size(), it.hasNext() ? it.next() : null);
                    }
                } return true;

                case R.id.import_mct_file:
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        return true;
                    }
                    uri = Uri.fromFile(Environment.getExternalStorageDirectory());
                    i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    i.setType("*/*");
                    String[] mctMimetypes = {
                            "text/plain",
                            // Fallback for cases where we didn't get a good mime type from the
                            // OS, this allows most "other" files to be selected.
                            "application/octet-stream",
                    };
                    i.putExtra(Intent.EXTRA_MIME_TYPES, mctMimetypes);
                    startActivityForResult(Intent.createChooser(i, Localizer.INSTANCE.localizeString(R.string.select_file)), REQUEST_SELECT_FILE_MCT);
                    return true;

                case R.id.import_file:
                    // Some files are text/xml, some are application/xml.
                    // In Android 4.4 and later, we can say the right thing!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        uri = Uri.fromFile(Environment.getExternalStorageDirectory());
                        i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.putExtra(Intent.EXTRA_STREAM, uri);
                        i.setType("*/*");
                        String[] mimetypes = {
                                "application/xml",
                                "text/xml",
                                "text/plain",
                                // Fallback for cases where we didn't get a good mime type from the
                                // OS, this allows most "other" files to be selected.
                                "application/octet-stream",
                        };
                        i.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                        startActivityForResult(Intent.createChooser(i, Localizer.INSTANCE.localizeString(R.string.select_file)), REQUEST_SELECT_FILE);
                    } else {
                        // Failsafe, used in the emulator for local files
                        new ReadTask(this).execute(Uri.fromFile(new File(SD_IMPORT_PATH)));
                    }
                    return true;

                case R.id.copy_xml: {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ExportHelper.exportCardsXml(bos, getActivity());
                    ExportHelper.copyXmlToClipboard(getActivity(), bos.toString("UTF-8"));
                    return true;
                }

                case R.id.share_xml:
                    new ShareTask().execute();
                    return true;

                case R.id.deduplicate_cards:
                    new DedupTask(getActivity()).execute();
                    return true;

                case R.id.save_xml:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("text/xml");
                        i.putExtra(Intent.EXTRA_TITLE, STD_EXPORT_FILENAME);
                        startActivityForResult(Intent.createChooser(i, Localizer.INSTANCE.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE);
                    } else {
                        File file = new File(SD_EXPORT_PATH);
                        ExportHelper.exportCardsXml(FileUtils.openOutputStream(file), getActivity());
                        Toast.makeText(getActivity(), R.string.saved_xml, Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
        return false;
    }

    private static class DedupTask extends AsyncTask<Void, Integer, Pair<String, Integer>> {
        private final WeakReference<Context> mContext;

        private DedupTask(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Pair<String, Integer> doInBackground(Void... voids) {
            try {
                Set<Long> tf = ExportHelper.findDuplicates(MetrodroidApplication.getInstance());
                if (tf == null || tf.isEmpty())
                    return new Pair<>(null, 0);
                return new Pair<>(null, ExportHelper.deleteSet(MetrodroidApplication.getInstance(),
                        tf));
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                return new Pair<>(Utils.getErrorMessage(ex), null);
            }
        }

        @Override
        protected void onPostExecute(Pair<String, Integer> res) {
            String err = res.first;
            Integer tf = res.second;
            Context context = mContext.get();

            if (context == null)
                return;

            if (err != null) {
                new AlertDialog.Builder(context)
                        .setMessage(err)
                        .show();
                return;
            }

            Toast.makeText(context,
                    Localizer.INSTANCE.localizePlural(R.plurals.cards_deduped, tf, tf),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static class ShareTask extends AsyncTask<Void, Integer, Pair<String, File>> {

        @Override
        protected Pair<String, File> doInBackground(Void... voids) {
            try {
                File folder = new File(MetrodroidApplication.getInstance().getCacheDir(), "share");
                folder.mkdirs();
                File tf = File.createTempFile("cards", ".xml",
                        folder);
                OutputStream os = new FileOutputStream(tf);
                ExportHelper.exportCardsXml(os, MetrodroidApplication.getInstance());
                os.close();
                return new Pair<>(null, tf);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                return new Pair<>(Utils.getErrorMessage(ex), null);
            }
        }

        @Override
        protected void onPostExecute(Pair<String, File> res) {
            String err = res.first;
            File tf = res.second;

            if (err != null) {
                new AlertDialog.Builder(MetrodroidApplication.getInstance())
                        .setMessage(err)
                        .show();
                return;
            }

            Intent i = new Intent(Intent.ACTION_SEND);
            @SuppressWarnings("StringConcatenation") Uri apkURI = FileProvider.getUriForFile(
                    MetrodroidApplication.getInstance(),
                    MetrodroidApplication.getInstance().getPackageName() + ".provider", tf);
            i.setType("text/xml");
            i.putExtra(Intent.EXTRA_STREAM, apkURI);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            MetrodroidApplication.getInstance().startActivity(i);
        }
    }

    private static class SaveTask extends AsyncTask<Uri, Integer, String> {

        @Override
        protected String doInBackground(Uri... uris) {
            try {
                OutputStream os = MetrodroidApplication.getInstance().getContentResolver().openOutputStream(uris[0]);
                if (os == null)
                    return "openOutputStream failed";
                ExportHelper.exportCardsXml(os, MetrodroidApplication.getInstance());
                os.close();
                return null;
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                return Utils.getErrorMessage(ex);
            }
        }

        @Override
        protected void onPostExecute(String err) {
            if (err == null) {
                Toast.makeText(MetrodroidApplication.getInstance(), R.string.saved_xml_custom, Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(MetrodroidApplication.getInstance())
                    .setMessage(err)
                    .show();
        }
    }

    private abstract static class CommonReadTask extends AsyncTask<Uri, Integer, Pair<String, Collection<Uri>>> {
        private final WeakReference<CardsFragment> mCardsFragment;
        private final CardImporter<? extends Card> mCardImporter;

        private CommonReadTask(CardsFragment cardsFragment,
                               @NonNull CardImporter<? extends Card> cardImporter) {
            mCardsFragment = new WeakReference<>(cardsFragment);
            mCardImporter = cardImporter;
        }

        @Override
        protected Pair<String, Collection<Uri>> doInBackground(Uri... uris) {
            try {
                final ContentResolver cr = MetrodroidApplication.getInstance().getContentResolver();

                Log.d(TAG, "REQUEST_SELECT_FILE content_type = " + cr.getType(uris[0]));
                InputStream stream = cr.openInputStream(uris[0]);
                assert stream != null; // Will be handled by exception handler below...

                Collection<Uri> iuri = ExportHelper.importCards(
                        stream, mCardImporter, MetrodroidApplication.getInstance());

                return new Pair<>(null, iuri);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                return new Pair<>(Utils.getErrorMessage(ex), null);
            }
        }

        @Override
        protected void onPostExecute(Pair<String, Collection<Uri>> res) {
            String err = res.first;
            Collection<Uri> uris = res.second;
            CardsFragment cf = mCardsFragment.get();

            if (cf == null)
                return;

            if (err == null) {
                cf.updateListView();
                Iterator<Uri> it = uris.iterator();
                onCardsImported(cf.getActivity(), uris.size(), it.hasNext() ? it.next() : null);
                return;
            }
            new AlertDialog.Builder(cf.getActivity())
                    .setMessage(err)
                    .show();
        }
    }


    private static class ReadTask extends CommonReadTask {
        private ReadTask(CardsFragment cardsFragment) throws ParserConfigurationException {
            super(cardsFragment, new XmlCardFormat());
        }
    }

    private static class MCTReadTask extends CommonReadTask {
        private MCTReadTask(CardsFragment cardsFragment) {
            super(cardsFragment, new MctCardImporter());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        try {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SELECT_FILE:
                        uri = data.getData();
                        new ReadTask(this).execute(uri);
                        break;

                    case REQUEST_SELECT_FILE_MCT:
                        uri = data.getData();
                        new MCTReadTask(this).execute(uri);
                        break;

                    case REQUEST_SAVE_FILE:
                        uri = data.getData();
                        Log.d(TAG, "REQUEST_SAVE_FILE");
                        new SaveTask().execute(uri);
                        break;
                }
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
    }

    private static void onCardsImported(Context ctx, int uriCount, @Nullable Uri firstUri) {
        Toast.makeText(ctx, Localizer.INSTANCE.localizePlural(
                R.plurals.cards_imported, uriCount, uriCount), Toast.LENGTH_SHORT).show();
        if (uriCount == 1 && firstUri != null) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, firstUri));
        }
    }

    private void updateListView() {
        ((CardsAdapter) ((ExpandableListView) getView().findViewById(android.R.id.list)).getExpandableListAdapter()).notifyDataSetChanged();
    }

    private static class CardsAdapter extends BaseExpandableListAdapter {
        private final LayoutInflater mLayoutInflater;
        private final Map<Pair<Integer, String>, List<Scan>> mScans;
        private final List<Pair<Integer, String>> mCards;

        private CardsAdapter(Context ctxt,
                            Map<Pair<Integer,String>, List<Scan>> scans, List<Pair<Integer,String>> cards) {
            //noinspection StringConcatenation
            Log.d(TAG, "Cards adapter " + cards.size());
            mLayoutInflater = LayoutInflater.from(ctxt);
            mScans = scans;
            mCards = cards;
        }

        @Override
        public int getGroupCount() {
            //noinspection StringConcatenation
            Log.d(TAG, "getgroupcount " + mCards.size());
            return mCards.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return mScans.get(mCards.get(i)).size();
        }

        @Override
        public Object getGroup(int i) {
            //noinspection StringConcatenation
            Log.d(TAG, "getgroup " + i);
            return mScans.get(mCards.get(i));
        }

        @Override
        public Object getChild(int parent, int child) {
            return mScans.get(mCards.get(parent)).get(child);
        }

        @Override
        public long getGroupId(int i) {
            return i + 0x1000000;
        }

        @Override
        public long getChildId(int parent, int child) {
            return mScans.get(mCards.get(parent)).get(child).mId;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int group, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mLayoutInflater.inflate(R.layout.card_name_header,
                        parent, false);
            Scan scan = mScans.get(mCards.get(group)).get(0);
            int type = scan.mType;
            String serial = scan.mSerial;
            String label = scan.mLabel;

            if (scan.mTransitIdentity == null) {
                try {
                    scan.mTransitIdentity = Card.fromXml(scan.mData).parseTransitIdentity();
                } catch (Exception ex) {
                    String error = String.format("Error: %s", Utils.getErrorMessage(ex));
                    scan.mTransitIdentity = new TransitIdentity(error, null);
                }
            }

            TransitIdentity identity = scan.mTransitIdentity;

            TextView textView1 = convertView.findViewById(android.R.id.text1);
            TextView textView2 = convertView.findViewById(android.R.id.text2);

            if (identity != null) {
                textView1.setText(identity.getName());
                if (label != null && !label.isEmpty()) {
                    // This is used for imported cards from mfcdump_to_farebotxml.py
                    // Used for development and testing. We should always show this.
                    textView2.setText(label);
                } else if (Preferences.INSTANCE.getHideCardNumbers()) {
                    textView2.setVisibility(View.GONE);
                    // User doesn't want to show any card numbers.
                } else {
                    // User wants to show card numbers (default).
                    if (identity.getSerialNumber() != null) {
                        textView2.setText(identity.getSerialNumber());
                    } else {
                        // Fall back to showing the serial number of the NFC chip.
                        textView2.setText(serial);
                    }
                }
            } else {
                textView1.setText(R.string.unknown_card);
                if (Preferences.INSTANCE.getHideCardNumbers()) {
                    textView2.setText(String.format("%s", CardType.values()[type].toString()));
                } else {
                    textView2.setText(String.format("%s - %s", CardType.values()[type].toString(), serial));
                }
            }
            return convertView;
        }

        @Override
        public View getChildView(int parent, int child, boolean isLast, View convertView, ViewGroup viewGroup) {
            if (convertView == null)
                convertView = mLayoutInflater.inflate(R.layout.card_scan_item,
                        viewGroup, false);
            Scan scan = mScans.get(mCards.get(parent)).get(child);
            Calendar scannedAt = GregorianCalendar.getInstance();
            scannedAt.setTimeInMillis(scan.mScannedAt);
            scannedAt = TripObfuscator.maybeObfuscateTS(scannedAt);

            TextView textView1 = convertView.findViewById(android.R.id.text1);
            textView1.setText(Localizer.INSTANCE.localizeString(R.string.scanned_at_format, Utils.timeFormat(scannedAt),
                    Utils.dateFormat(scannedAt)));

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }
    }
}
