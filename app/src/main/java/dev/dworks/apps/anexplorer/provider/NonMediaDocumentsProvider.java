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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Files.FileColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.io.FileNotFoundException;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.archive.DocumentArchiveHelper;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;


/**
 * Presents a {@link DocumentsContract} view of {MediaProvider} external
 * contents.
 */
public class NonMediaDocumentsProvider extends StorageProvider {
    @SuppressWarnings("unused")
	private static final String TAG = "NonMediaDocumentsProvider";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".nonmedia.documents";
    // docId format: root:id

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
            Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_MIME_TYPES
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE,
    };

    private static final String[] DOCUMENT_MIMES =
            new String[] {
                    "application/pdf",
                    "application/epub+zip",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.openxmlformats-officedocument.presentationml.template",
                    "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                    "application/vnd.oasis.opendocument.text",
                    "application/vnd.oasis.opendocument.text-master",
                    "application/vnd.oasis.opendocument.text-template",
                    "application/vnd.oasis.opendocument.text-web",
                    "application/vnd.stardivision.writer",
                    "application/vnd.stardivision.writer-global",
                    "application/vnd.sun.xml.writer",
                    "application/vnd.sun.xml.writer.global",
                    "application/vnd.sun.xml.writer.template",
                    "application/x-abiword",
                    "application/x-kword",
            };



    private static final String[] ARCHIVE_MIMES =
            new String[] {
                    "application/mac-binhex40",
                    "application/rar",
                    "application/zip",
                    "application/x-apple-diskimage",
                    "application/x-debian-package",
                    "application/x-gtar",
                    "application/x-iso9660-image",
                    "application/x-lha",
                    "application/x-lzh",
                    "application/x-lzx",
                    "application/x-stuffit",
                    "application/x-tar",
                    "application/x-webarchive",
                    "application/x-webarchive-xml",
                    "application/gzip",
                    "application/x-7z-compressed",
                    "application/x-deb",
                    "application/x-rar-compressed"
            };

    private static final String[] APK_MIMES =
            new String[] {
                    "application/vnd.android.package-archive"
            };

    private static final String DOCUMENT_MIME_TYPES =
            joinNewline(DOCUMENT_MIMES);

    private static final String ARCHIVE_MIME_TYPES =
            joinNewline(ARCHIVE_MIMES);

    private static final String APK_MIME_TYPES =
            joinNewline(APK_MIMES);

    public static final String TYPE_DOCUMENT_ROOT = "document_root";
    public static final String TYPE_DOCUMENT = "document";

    public static final String TYPE_ARCHIVE_ROOT = "archive_root";
    public static final String TYPE_ARCHIVE = "archive";

    public static final String TYPE_APK_ROOT = "apk_root";
    public static final String TYPE_APK = "apk";

    private static String joinNewline(String[] args) {
        return TextUtils.join("\n", args);
    }

    private void copyNotificationUri(MatrixCursor result, Uri uri) {
        result.setNotificationUri(getContext().getContentResolver(), uri);//cursor.getNotificationUri());
    }

    @Override
    public boolean onCreate() {
        return super.onCreate();
    }

    public static void notifyRootsChanged(Context context) {
        context.getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }

    public static void notifyDocumentsChanged(Context context, String rootId) {
        Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        context.getContentResolver().notifyChange(uri, null, false);
    }

    private static class Ident {
        public String type;
        public long id;
    }

    private static Ident getIdentForDocId(String docId) {
        final Ident ident = new Ident();
        final int split = docId.indexOf(':');
        if (split == -1) {
            ident.type = docId;
            ident.id = -1;
        } else {
            ident.type = docId.substring(0, split);
            ident.id = Long.parseLong(docId.substring(split + 1));
        }
        return ident;
    }

    private static String getDocIdForIdent(String type, long id) {
        return type + ":" + id;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        if(!isTelevision()){
            includeFileRoot(result, TYPE_DOCUMENT_ROOT, R.string.root_document, DOCUMENT_MIME_TYPES, true);
        }
        includeFileRoot(result, TYPE_ARCHIVE_ROOT, R.string.root_archive, ARCHIVE_MIME_TYPES, false);
        includeFileRoot(result, TYPE_APK_ROOT, R.string.root_apk, APK_MIME_TYPES, false);

        return result;
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        final Ident ident = getIdentForDocId(docId);

        if (mArchiveHelper.isArchivedDocument(docId)) {
            return mArchiveHelper.queryDocument(docId, projection);
        }

        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;

        try {
            if (TYPE_DOCUMENT_ROOT.equals(ident.type)) {
                includeFileRootDocument(result, TYPE_DOCUMENT_ROOT, R.string.root_document);
            } else if (TYPE_DOCUMENT.equals(ident.type)) {
                queryLikeFile(resolver, cursor, result, DOCUMENT_MIMES, "text");
            } else if (TYPE_ARCHIVE_ROOT.equals(ident.type)) {
                includeFileRootDocument(result, TYPE_ARCHIVE_ROOT, R.string.root_archive);
            } else if (TYPE_ARCHIVE.equals(ident.type)) {
                queryFile(resolver, cursor, result, ARCHIVE_MIMES, TYPE_ARCHIVE);
            } else if (TYPE_APK_ROOT.equals(ident.type)) {
                includeFileRootDocument(result, TYPE_APK_ROOT, R.string.root_apk);
            } else if (TYPE_APK.equals(ident.type)) {
                queryFile(resolver, cursor, result, APK_MIMES, TYPE_APK);
            } else {
                throw new UnsupportedOperationException("Unsupported document " + docId);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String docId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if (mArchiveHelper.isArchivedDocument(docId) ||
                DocumentArchiveHelper.isSupportedArchiveType(getDocumentType(docId))) {
            return mArchiveHelper.queryChildDocuments(docId, projection, sortOrder);
        }

        final Ident ident = getIdentForDocId(docId);
        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            if (TYPE_DOCUMENT_ROOT.equals(ident.type)) {
                queryLikeFile(resolver, cursor, result, DOCUMENT_MIMES, "text");
            } else if (TYPE_ARCHIVE_ROOT.equals(ident.type)) {
                queryFile(resolver, cursor, result, ARCHIVE_MIMES, TYPE_ARCHIVE);
            } else if (TYPE_APK_ROOT.equals(ident.type)) {
                queryFile(resolver, cursor, result, APK_MIMES, TYPE_APK);
            } else {
                throw new UnsupportedOperationException("Unsupported document " + docId);
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
        final ContentResolver resolver = getContext().getContentResolver();
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            if (TYPE_DOCUMENT_ROOT.equals(rootId)) {
                queryRecentFile(resolver, cursor, result, DOCUMENT_MIMES);
            } else {
                throw new UnsupportedOperationException("Unsupported root " + rootId);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    private void queryLikeFile(ContentResolver resolver, Cursor cursor, MatrixCursor result, String[] mimeType, String like) {
        // single file
        cursor = resolver.query(FILE_URI,
                FileQuery.PROJECTION,
                FileColumns.MIME_TYPE + " IN "+ "("+toString(mimeType)+")"
                + " OR " + FileColumns.MIME_TYPE + " LIKE "+ "'"+like+"%'",
                null,
                null);
        copyNotificationUri(result, FILE_URI);
        while (cursor.moveToNext()) {
            includeFile(result, cursor, TYPE_DOCUMENT);
        }
    }

    private void queryFile(ContentResolver resolver, Cursor cursor, MatrixCursor result, String[] mimeType, String type) {
        // single file
        cursor = resolver.query(FILE_URI,
                FileQuery.PROJECTION, FileColumns.MIME_TYPE + " IN "+ "("+toString(mimeType)+")" , null,
                null);
        copyNotificationUri(result, FILE_URI);
        while (cursor.moveToNext()) {
            includeFile(result, cursor, type);
        }
    }

    private void queryRecentFile(ContentResolver resolver, Cursor cursor, MatrixCursor result, String[] mimeType) {
        // single file
        cursor = resolver.query(FILE_URI,
                FileQuery.PROJECTION, FileColumns.MIME_TYPE + " IN "+ "("+toString(mimeType)+")" , null,
                FileQuery.DATE_MODIFIED + " DESC");
        copyNotificationUri(result, FILE_URI);
        while (cursor.moveToNext() && result.getCount() < 64) {
            includeFile(result, cursor, TYPE_DOCUMENT);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String docId, String mode, CancellationSignal signal)
            throws FileNotFoundException {

        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Media is read-only");
        }

        if (mArchiveHelper.isArchivedDocument(docId)) {
            return mArchiveHelper.openDocument(docId, mode, signal);
        }

        final Uri target = getUriForDocumentId(docId);

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            return getContext().getContentResolver().openFileDescriptor(target, mode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.getDocumentType(documentId);
        }
        return super.getDocumentType(documentId);
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.isChildDocument(parentDocumentId, documentId);
        }
        // Archives do not contain regular files.
        if (mArchiveHelper.isArchivedDocument(parentDocumentId)) {
            return false;
        }
        return super.isChildDocument(parentDocumentId, documentId);
    }

    private Uri getUriForDocumentId(String docId) {
        final Ident ident = getIdentForDocId(docId);
        if (TYPE_DOCUMENT.equals(ident.type) && ident.id != -1) {
            return ContentUris.withAppendedId( FILE_URI, ident.id);
        } else if (TYPE_ARCHIVE.equals(ident.type) && ident.id != -1) {
            return ContentUris.withAppendedId( FILE_URI, ident.id);
        } else if (TYPE_APK.equals(ident.type) && ident.id != -1) {
            return ContentUris.withAppendedId( FILE_URI, ident.id);
        } else {
            throw new UnsupportedOperationException("Unsupported document " + docId);
        }
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        final Uri target = getUriForDocumentId(docId);

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            getContext().getContentResolver().delete(target, null, null);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(
            String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(docId)) {
            return mArchiveHelper.openDocumentThumbnail(docId, sizeHint, signal);
        }
        final Ident ident = getIdentForDocId(docId);

        final long token = Binder.clearCallingIdentity();
        try {
            if (TYPE_DOCUMENT.equals(ident.type)) {
                return openOrCreateImageThumbnailCleared(ident.id, signal);
            } else if (TYPE_APK.equals(ident.type)) {
                final long id = getAlbumForAudioCleared(ident.id);
                return openOrCreateAudioThumbnailCleared(id, signal);
            } else {
                throw new UnsupportedOperationException("Unsupported document " + docId);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isEmpty(Uri uri, String type) {
        final ContentResolver resolver = getContext().getContentResolver();
        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            String[] mimeType;
            if (TYPE_DOCUMENT_ROOT.equals(type)) {
                mimeType = DOCUMENT_MIMES;
            } else if (TYPE_ARCHIVE_ROOT.equals(type)) {
                mimeType = ARCHIVE_MIMES;
            } else if (TYPE_APK_ROOT.equals(type)) {
                mimeType = APK_MIMES;
            } else {
                return true;
            }
            cursor = resolver.query(FILE_URI,
                    FileQuery.PROJECTION,
                    FileColumns.MIME_TYPE + " IN "+ "("+toString(mimeType)+")" , null, null);
            return (cursor == null) || (cursor.getCount() == 0);
        } finally {
            IoUtils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
    }

    private void includeFileRoot(MatrixCursor result, String root_type, int name_id,
                                 String mime_types, boolean supports_recent) {
        int flags = Root.FLAG_LOCAL_ONLY;
        if (isEmpty(FILE_URI, root_type)) {
            flags |= Root.FLAG_EMPTY;
        }

        if(supports_recent){
            flags |= Root.FLAG_SUPPORTS_RECENTS;
        }
        final RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, root_type);
        row.add(Root.COLUMN_FLAGS, flags);
        row.add(Root.COLUMN_TITLE, getContext().getString(name_id));
        row.add(Root.COLUMN_DOCUMENT_ID, root_type);
        row.add(Root.COLUMN_MIME_TYPES, mime_types);
    }

    private void includeFileRootDocument(MatrixCursor result, String root_type, int name_id) {
        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, root_type);
        row.add(Document.COLUMN_DISPLAY_NAME, getContext().getString(name_id));
        int flags = Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_SUPPORTS_DELETE;
        if(!isWatch()) {
            flags |= Document.FLAG_DIR_PREFERS_GRID;
        }
        row.add(Document.COLUMN_FLAGS, flags);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    private interface FileQuery {
        String[] PROJECTION = new String[] {
                FileColumns._ID,
                FileColumns.TITLE,
                FileColumns.MIME_TYPE,
                FileColumns.SIZE,
                FileColumns.DATA,
                FileColumns.DATE_MODIFIED };

        int _ID = 0;
        int TITLE = 1;
        int MIME_TYPE = 2;
        int SIZE = 3;
        int DATA = 4;
        int DATE_MODIFIED = 5;
    }

    private void includeFile(MatrixCursor result, Cursor cursor, String type) {
        final long id = cursor.getLong(FileQuery._ID);
        final String docId = getDocIdForIdent(type, id);

        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, cursor.getString(FileQuery.TITLE));
        row.add(Document.COLUMN_SIZE, cursor.getLong(FileQuery.SIZE));
        row.add(Document.COLUMN_MIME_TYPE, cursor.getString(FileQuery.MIME_TYPE));
        row.add(Document.COLUMN_PATH, cursor.getString(FileQuery.DATA));
        row.add(Document.COLUMN_LAST_MODIFIED,
                cursor.getLong(FileQuery.DATE_MODIFIED) * DateUtils.SECOND_IN_MILLIS);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_SUPPORTS_DELETE);
    }

    public static String toString(String[] list) {
        if (list == null) {
            return "";
        }
        if (list.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("'"+list[0]+"'");
        for (int i = 1; i < list.length; i++) {
            sb.append(", ");
            sb.append("'"+list[i]+"'");
        }
        return sb.toString();
    }
}
