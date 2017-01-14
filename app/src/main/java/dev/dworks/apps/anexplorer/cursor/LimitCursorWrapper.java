package dev.dworks.apps.anexplorer.cursor;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;

/**
 * Created by HaKr on 15/01/17.
 */

public class LimitCursorWrapper extends AbstractCursor {

    private final Cursor mCursor;
    private final int mCount;

    public LimitCursorWrapper(Cursor cursor, int maxCount) {
        mCursor = cursor;

        final int count = cursor.getCount();
        if (maxCount > 0 && count > maxCount) {
            mCount = maxCount;
        } else {
            mCount = count;
        }
    }

    @Override
    public Bundle getExtras() {
        return mCursor.getExtras();
    }

    @Override
    public void close() {
        super.close();
        mCursor.close();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return mCursor.moveToPosition(newPosition);
    }

    @Override
    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public double getDouble(int column) {
        return mCursor.getDouble(column);
    }

    @Override
    public float getFloat(int column) {
        return mCursor.getFloat(column);
    }

    @Override
    public int getInt(int column) {
        return mCursor.getInt(column);
    }

    @Override
    public long getLong(int column) {
        return mCursor.getLong(column);
    }

    @Override
    public short getShort(int column) {
        return mCursor.getShort(column);
    }

    @Override
    public String getString(int column) {
        return mCursor.getString(column);
    }

    @Override
    public int getType(int column) {
        return mCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return mCursor.isNull(column);
    }
}
