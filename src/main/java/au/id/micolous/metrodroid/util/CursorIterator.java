package au.id.micolous.metrodroid.util;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CursorIterator implements Iterator<Cursor>, Closeable {
    @NonNull
    private final Cursor mCursor;
    private boolean mMovedToNext;

    public CursorIterator(@NonNull Cursor cursor) {
        mCursor = cursor;
    }

    @Override
    public void close() {
        mCursor.close();
    }

    @Override
    public boolean hasNext() {
        return !mCursor.isLast();
    }

    @Override
    public Cursor next() {
        if (mCursor.moveToNext()) {
            return mCursor;
        } else {
            throw new NoSuchElementException();
        }
    }
}
