package dev.dworks.apps.anexplorer.model;

import static dev.dworks.apps.anexplorer.DocumentsActivity.State.MODE_UNKNOWN;
import static dev.dworks.apps.anexplorer.DocumentsActivity.State.SORT_ORDER_UNKNOWN;

import java.io.Closeable;

import android.content.ContentProviderClient;
import android.database.Cursor;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;
    public Exception exception;

    public int mode = MODE_UNKNOWN;
    public int sortOrder = SORT_ORDER_UNKNOWN;

    @Override
    public void close() {
        IoUtils.closeQuietly(cursor);
        ContentProviderClientCompat.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}