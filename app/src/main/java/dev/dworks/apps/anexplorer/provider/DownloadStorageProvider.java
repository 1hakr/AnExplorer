/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.provider;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.DownloadManagerUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;

/**
 * Presents a {@link DocumentsContract} view of {@link DownloadManager}
 * contents.
 */
@SuppressLint("InlinedApi")
public class DownloadStorageProvider extends DocumentsProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".downloads.documents";
    private static final String DOC_ID_ROOT = "downloads";
    
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
            Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_SUMMARY, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE,
    };

    private DownloadManager mDm;

    @Override
    public boolean onCreate() {
        mDm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManagerUtils.setAccessAllDownloads(mDm);
        //mDm.setAccessAllDownloads(true);
        return true;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @SuppressWarnings("unused")
	private void copyNotificationUri(MatrixCursor result, Uri uri) {
        result.setNotificationUri(getContext().getContentResolver(), uri);
    }

    static void onDownloadProviderDelete(Context context, long id) {
        final Uri uri = DocumentsContract.buildDocumentUri(AUTHORITY, Long.toString(id));
        context.revokeUriPermission(uri, ~0);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        final RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, DOC_ID_ROOT);
        row.add(Root.COLUMN_FLAGS,
                Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_RECENTS | Root.FLAG_SUPPORTS_CREATE);
        row.add(Root.COLUMN_ICON, R.drawable.ic_root_download);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_downloads));
        row.add(Root.COLUMN_DOCUMENT_ID, DOC_ID_ROOT);
        return result;
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName)
            throws FileNotFoundException {
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            throw new FileNotFoundException("Directory creation not supported");
        }

        final File parent = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        parent.mkdirs();

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            displayName = removeExtension(mimeType, displayName);
            File file = new File(parent, addExtension(mimeType, displayName));

            // If conflicting file, try adding counter suffix
            int n = 0;
            while (file.exists() && n++ < 32) {
                file = new File(parent, addExtension(mimeType, displayName + " (" + n + ")"));
            }

            try {
                if (!file.createNewFile()) {
                    throw new IllegalStateException("Failed to touch " + file);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to touch " + file + ": " + e);
            }

            return Long.toString(mDm.addCompletedDownload(
                    file.getName(), file.getName(), false, mimeType, file.getAbsolutePath(), 0L,
                    false));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            if (mDm.remove(Long.parseLong(docId)) != 1) {
                throw new IllegalStateException("Failed to delete " + docId);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        if (DOC_ID_ROOT.equals(docId)) {
            includeDefaultDocument(result);
        } else {
            // Delegate to real provider
            final long token = Binder.clearCallingIdentity();
            Cursor cursor = null;
            try {
                cursor = mDm.query(new Query().setFilterById(Long.parseLong(docId)));
                //copyNotificationUri(result, cursor);
                if (cursor.moveToFirst()) {
                    includeDownloadFromCursor(result, cursor);
                }
            } finally {
                IoUtils.closeQuietly(cursor);
                Binder.restoreCallingIdentity(token);
            }
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String docId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
        	Query query = new Query();
        	DownloadManagerUtils.setOnlyIncludeVisibleInDownloadsUi(query);
        	//query.setOnlyIncludeVisibleInDownloadsUi(true);
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
        	//query.setFilterByStatus(DownloadManager.STATUS_PENDING | DownloadManager.STATUS_PAUSED | DownloadManager.STATUS_RUNNING | DownloadManager.STATUS_FAILED);
            cursor = mDm.query(query);//.setOnlyIncludeVisibleInDownloadsUi(true)
                    //.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL));
            //copyNotificationUri(result, cursor);
            while (cursor.moveToNext()) {
                includeDownloadFromCursor(result, cursor);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    @Override
    public Cursor queryChildDocumentsForManage(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = mDm.query(
                    new DownloadManager.Query());//.setOnlyIncludeVisibleInDownloadsUi(true));
            //copyNotificationUri(result, cursor);
            while (cursor.moveToNext()) {
                includeDownloadFromCursor(result, cursor);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = mDm.query(new DownloadManager.Query()//.setOnlyIncludeVisibleInDownloadsUi(true)
                    .setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL));
            //copyNotificationUri(result, cursor);
            while (cursor.moveToNext() && result.getCount() < 12) {
                final String mimeType = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE));
                final String uri = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIAPROVIDER_URI));

                // Skip images that have been inserted into the MediaStore so we
                // don't duplicate them in the recents list.
                if (mimeType == null
                        || (mimeType.startsWith("image/") && !TextUtils.isEmpty(uri))) {
                    continue;
                }

                includeDownloadFromCursor(result, cursor);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            final long id = Long.parseLong(docId);
            final ContentResolver resolver = getContext().getContentResolver();
            return resolver.openFileDescriptor(mDm.getUriForDownloadedFile(id), mode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

/*    @Override
    public AssetFileDescriptor openDocumentThumbnail(
            String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        // TODO: extend ExifInterface to support fds
        final ParcelFileDescriptor pfd = openDocument(docId, "r", signal);
        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }*/

    private void includeDefaultDocument(MatrixCursor result) {
        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, DOC_ID_ROOT);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_FLAGS,
                Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_DIR_SUPPORTS_CREATE);
    }

    @SuppressLint("InlinedApi")
	private void includeDownloadFromCursor(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
        final String docId = String.valueOf(id);

        final String displayName = cursor.getString(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
        String summary = cursor.getString(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION));
        String mimeType = cursor.getString(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE));
        if (mimeType == null) {
            // Provide fake MIME type so it's openable
            mimeType = "vnd.android.document/file";
        }
        Long size = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        if (size == -1) {
            size = null;
        }

        final int status = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
        switch (status) {
            case DownloadManager.STATUS_SUCCESSFUL:
                break;
            case DownloadManager.STATUS_PAUSED:
                summary = getContext().getString(R.string.download_queued);
                break;
            case DownloadManager.STATUS_PENDING:
                summary = getContext().getString(R.string.download_queued);
                break;
            case DownloadManager.STATUS_RUNNING:
                final long progress = cursor.getLong(cursor.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                if (size != null) {
                    final long percent = progress * 100 / size;
                    summary = getContext().getString(R.string.download_running_percent, percent);
                } else {
                    summary = getContext().getString(R.string.download_running);
                }
                break;
            case DownloadManager.STATUS_FAILED:
            default:
                summary = getContext().getString(R.string.download_error);
                break;
        }

        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_WRITE;
        if (mimeType != null && mimeType.startsWith("image/")) {
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final long lastModified = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));

        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SUMMARY, summary);
        row.add(Document.COLUMN_SIZE, size);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        row.add(Document.COLUMN_FLAGS, flags);
    }

    /**
     * Remove file extension from name, but only if exact MIME type mapping
     * exists. This means we can reapply the extension later.
     */
    private static String removeExtension(String mimeType, String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1);
            final String nameMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType.equals(nameMime)) {
                return name.substring(0, lastDot);
            }
        }
        return name;
    }

    /**
     * Add file extension to name, but only if exact MIME type mapping exists.
     */
    private static String addExtension(String mimeType, String name) {
        final String extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType);
        if (extension != null) {
            return name + "." + extension;
        }
        return name;
    }
}
