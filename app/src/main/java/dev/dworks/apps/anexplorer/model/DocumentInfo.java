/*
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

package dev.dworks.apps.anexplorer.model;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ProtocolException;
import java.text.Collator;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.cursor.RootCursorWrapper;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.provider.DocumentsProvider;

/**
 * Representation of a {@link Document}.
 */
public class DocumentInfo implements Durable, Parcelable {
    private static final int VERSION_INIT = 1;
    private static final int VERSION_SPLIT_URI = 2;

    private static final Collator sCollator;

    static {
        sCollator = Collator.getInstance();
        sCollator.setStrength(Collator.SECONDARY);
    }

    public String authority;
    public String documentId;
    public String mimeType;
    public String displayName;
    public long lastModified;
    public int flags;
    public String summary;
    public long size;
    public int icon;
    public String path;

    /** Derived fields that aren't persisted */
    public Uri derivedUri;

    public DocumentInfo() {
        reset();
    }

    @Override
    public void reset() {
        authority = null;
        documentId = null;
        mimeType = null;
        displayName = null;
        lastModified = -1;
        flags = 0;
        summary = null;
        size = -1;
        icon = 0;
        path = null;
        
        derivedUri = null;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        final int version = in.readInt();
        switch (version) {
            case VERSION_INIT:
                throw new ProtocolException("Ignored upgrade");
            case VERSION_SPLIT_URI:
                authority = DurableUtils.readNullableString(in);
                documentId = DurableUtils.readNullableString(in);
                mimeType = DurableUtils.readNullableString(in);
                displayName = DurableUtils.readNullableString(in);
                lastModified = in.readLong();
                flags = in.readInt();
                summary = DurableUtils.readNullableString(in);
                size = in.readLong();
                icon = in.readInt();
                path = DurableUtils.readNullableString(in);
                deriveFields();
                break;
            default:
                throw new ProtocolException("Unknown version " + version);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(VERSION_SPLIT_URI);
        DurableUtils.writeNullableString(out, authority);
        DurableUtils.writeNullableString(out, documentId);
        DurableUtils.writeNullableString(out, mimeType);
        DurableUtils.writeNullableString(out, displayName);
        out.writeLong(lastModified);
        out.writeInt(flags);
        DurableUtils.writeNullableString(out, summary);
        out.writeLong(size);
        out.writeInt(icon);
        DurableUtils.writeNullableString(out, path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        DurableUtils.writeToParcel(dest, this);
    }

    public static final Creator<DocumentInfo> CREATOR = new Creator<DocumentInfo>() {
        @Override
        public DocumentInfo createFromParcel(Parcel in) {
            final DocumentInfo doc = new DocumentInfo();
            DurableUtils.readFromParcel(in, doc);
            return doc;
        }

        @Override
        public DocumentInfo[] newArray(int size) {
            return new DocumentInfo[size];
        }
    };

    public static DocumentInfo fromDirectoryCursor(Cursor cursor) {
        final String authority = getCursorString(cursor, RootCursorWrapper.COLUMN_AUTHORITY);
        return fromCursor(cursor, authority);
    }

    public static DocumentInfo fromCursor(Cursor cursor, String authority) {
        final DocumentInfo info = new DocumentInfo();
        info.updateFromCursor(cursor, authority);
        return info;
    }

    public void updateFromCursor(Cursor cursor, String authority) {
        this.authority = authority;
        this.documentId = getCursorString(cursor, Document.COLUMN_DOCUMENT_ID);
        this.mimeType = getCursorString(cursor, Document.COLUMN_MIME_TYPE);
        this.documentId = getCursorString(cursor, Document.COLUMN_DOCUMENT_ID);
        this.mimeType = getCursorString(cursor, Document.COLUMN_MIME_TYPE);
        this.displayName = getCursorString(cursor, Document.COLUMN_DISPLAY_NAME);
        this.lastModified = getCursorLong(cursor, Document.COLUMN_LAST_MODIFIED);
        this.flags = getCursorInt(cursor, Document.COLUMN_FLAGS);
        this.summary = getCursorString(cursor, Document.COLUMN_SUMMARY);
        this.size = getCursorLong(cursor, Document.COLUMN_SIZE);
        this.icon = getCursorInt(cursor, Document.COLUMN_ICON);
        this.path = getCursorString(cursor, Document.COLUMN_PATH);
        this.deriveFields();
    }

    public static DocumentInfo fromUri(ContentResolver resolver, Uri uri)
            throws FileNotFoundException {
        final DocumentInfo info = new DocumentInfo();
        info.updateFromUri(resolver, uri);
        return info;
    }

    /**
     * Update a possibly stale restored document against a live
     * {@link DocumentsProvider}.
     */
    public void updateSelf(ContentResolver resolver) throws FileNotFoundException {
        updateFromUri(resolver, derivedUri);
    }

    public void updateFromUri(ContentResolver resolver, Uri uri) throws FileNotFoundException {
        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = DocumentsApplication.acquireUnstableProviderOrThrow(
                    resolver, uri.getAuthority());
            cursor = client.query(uri, null, null, null, null);
            if (!cursor.moveToFirst()) {
                throw new FileNotFoundException("Missing details for " + uri);
            }
            updateFromCursor(cursor, uri.getAuthority());
        } catch (Throwable t) {
            throw asFileNotFoundException(t);
        } finally {
            IoUtils.closeQuietly(cursor);
            ContentProviderClientCompat.releaseQuietly(client);
        }
    }

    private void deriveFields() {
        derivedUri = DocumentsContract.buildDocumentUri(authority, documentId);
    }

    @Override
    public String toString() {
        return "Document{docId=" + documentId + ", name=" + displayName + "}";
    }

    public boolean isCreateSupported() {
        return (flags & Document.FLAG_DIR_SUPPORTS_CREATE) != 0;
    }

    public boolean isThumbnailSupported() {
        return (flags & Document.FLAG_SUPPORTS_THUMBNAIL) != 0;
    }

    public boolean isDirectory() {
        return Document.MIME_TYPE_DIR.equals(mimeType);
    }

    public boolean isGridPreferred() {
        return (flags & Document.FLAG_DIR_PREFERS_GRID) != 0;
    }

    public boolean isDeleteSupported() {
        return (flags & Document.FLAG_SUPPORTS_DELETE) != 0;
    }

    public boolean isMoveSupported() {
        return (flags & Document.FLAG_SUPPORTS_MOVE) != 0;
    }

    public boolean isCopySupported() {
        return (flags & Document.FLAG_SUPPORTS_COPY) != 0;
    }

    public boolean isRemoveSupported() {
        return (flags & Document.FLAG_SUPPORTS_REMOVE) != 0;
    }

    public boolean isRenameSupported() {
        return (flags & Document.FLAG_SUPPORTS_RENAME) != 0;
    }

    public boolean isArchiveSupported() {
        return (flags & Document.FLAG_SUPPORTS_ARCHIVE) != 0;
    }

    public boolean isBookmarkSupported() {
        return (flags & Document.FLAG_SUPPORTS_BOOKMARK) != 0;
    }

    public boolean isWriteSupported() {
        return (flags & Document.FLAG_SUPPORTS_EDIT) != 0;
    }

    public boolean isArchive() {
        return (flags & Document.FLAG_ARCHIVE) != 0;
    }

    public boolean isPartial() {
        return (flags & Document.FLAG_PARTIAL) != 0;
    }

    public boolean isContainer() {
        return isDirectory() || isArchive();
    }

    public boolean isVirtualDocument() {
        return (flags & Document.FLAG_VIRTUAL_DOCUMENT) != 0;
    }

    public boolean isGridTitlesHidden() {
        return (flags & Document.FLAG_DIR_HIDE_GRID_TITLES) != 0;
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getString(index) : null;
    }

    /**
     * Missing or null values are returned as -1.
     */
    public static long getCursorLong(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        if (index == -1) return -1;
        final String value = cursor.getString(index);
        if (value == null) return -1;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Missing or null values are returned as 0.
     */
    public static int getCursorInt(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getInt(index) : 0;
    }


    /**
     * Missing or null values are returned as 0.
     */
    public static boolean getCursorBolean(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) && cursor.getInt(index) == 1;
    }

    public static FileNotFoundException asFileNotFoundException(Throwable t)
            throws FileNotFoundException {
        if (t instanceof FileNotFoundException) {
            throw (FileNotFoundException) t;
        }
        final FileNotFoundException fnfe = new FileNotFoundException(t.getMessage());
        fnfe.initCause(t);
        throw fnfe;
    }

    /**
     * String prefix used to indicate the document is a directory.
     */
    public static final char DIR_PREFIX = '\001';

    /**
     * Compare two strings against each other using system default collator in a
     * case-insensitive mode. Clusters strings prefixed with {@link #DIR_PREFIX}
     * before other items.
     */
    public static int compareToIgnoreCaseNullable(String lhs, String rhs) {
        final boolean leftEmpty = TextUtils.isEmpty(lhs);
        final boolean rightEmpty = TextUtils.isEmpty(rhs);

        if (leftEmpty && rightEmpty) return 0;
        if (leftEmpty) return -1;
        if (rightEmpty) return 1;

        final boolean leftDir = (lhs.charAt(0) == DIR_PREFIX);
        final boolean rightDir = (rhs.charAt(0) == DIR_PREFIX);

        if (leftDir && !rightDir) return -1;
        if (rightDir && !leftDir) return 1;

        return sCollator.compare(lhs, rhs);
    }
}
