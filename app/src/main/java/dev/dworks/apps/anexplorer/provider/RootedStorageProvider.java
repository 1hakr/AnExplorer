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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.ParcelFileDescriptorUtil;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.root.RootCommands;
import dev.dworks.apps.anexplorer.root.RootFile;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;

public class RootedStorageProvider extends StorageProvider {
    private static final String TAG = "RootedStorage";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".rootedstorage.documents";
    // docId format: root:path/to/file

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_CAPACITY_BYTES, Root.COLUMN_PATH,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_SUMMARY,
    };

    private static class RootInfo {
        public String rootId;
        public int flags;
        public String title;
        public String docId;
        public RootFile path;
    }

    public static final String ROOT_ID_ROOT = "root";

    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayMap<String, RootInfo> mRoots = new ArrayMap<>();

    @Override
    public boolean onCreate() {

        updateRoots();
        return super.onCreate();
    }

    @Override
    public void updateRoots() {
        mRoots.clear();
        try {
            final String rootId = ROOT_ID_ROOT;
            final RootFile path = new RootFile("/");
            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags =  Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED;
            root.title = getContext().getString(R.string.root_root_storage);
            root.path = path;
            root.docId = getDocIdForRootFile(path);
        } catch (FileNotFoundException e) {
            CrashReportingManager.logException(e);
        }

        notifyRootsChanged(getContext());
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    public static void notifyRootsChanged(Context context) {
        context.getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }

    public static void notifyDocumentsChanged(Context context, String rootId) {
        Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        context.getContentResolver().notifyChange(uri, null, false);
    }

    private String getDocIdForRootFile(RootFile file) throws FileNotFoundException {
        String path = file.getAbsolutePath();

        // Find the most-specific root path
        Map.Entry<String, RootInfo> mostSpecific = null;
        synchronized (mRootsLock) {
            for (Map.Entry<String, RootInfo> root : mRoots.entrySet()) {
                final String rootPath = root.getValue().path.getPath();
                if (path.startsWith(rootPath) && (mostSpecific == null
                        || rootPath.length() > mostSpecific.getValue().path.getPath().length())) {
                    mostSpecific = root;
                }
            }
        }

        if (mostSpecific == null) {
            throw new FileNotFoundException("Failed to find root that contains " + path);
        }

        // Start at first char of path under root
        final String rootPath = mostSpecific.getValue().path.getPath();
        if (rootPath.equals(path)) {
            path = "";
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length());
        } else {
            path = path.substring(rootPath.length() + 1);
        }

        return mostSpecific.getKey() + ':' + path;
    }
    
    private RootFile getRootFileForDocId(String docId) throws FileNotFoundException {
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);
        final String path = docId.substring(splitIndex + 1);

        RootInfo root;
        synchronized (mRootsLock) {
            root = mRoots.get(tag);
        }
        if (root == null) {
            throw new FileNotFoundException("No root for " + tag);
        }

        RootFile target = root.path;

        if (target == null) {
            return null;
        }
        target = new RootFile(target.getAbsolutePath() + path);
        return target;
    }

    private void includeRootFile(MatrixCursor result, String docId, RootFile file)
            throws FileNotFoundException {
        if (docId == null) {
            if(!file.isValid()){
                return;
            }
            docId = getDocIdForRootFile(file);
        } else {
            file = getRootFileForDocId(docId);
        }

        int flags = 0;

        if(!file.isValid()){
        	return;
        }
        
        if (file.canWrite()) {
            if (file.isDirectory()) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
            } else {
                flags |= Document.FLAG_SUPPORTS_WRITE;
            }
            flags |= Document.FLAG_SUPPORTS_DELETE;
            flags |= Document.FLAG_SUPPORTS_RENAME;
            flags |= Document.FLAG_SUPPORTS_MOVE;
            flags |= Document.FLAG_SUPPORTS_COPY;
            flags |= Document.FLAG_SUPPORTS_ARCHIVE;
            flags |= Document.FLAG_SUPPORTS_EDIT;

            if(isTelevision()) {
                flags |= Document.FLAG_DIR_PREFERS_GRID;
            }
        }

        final String displayName = file.getName();
        final String mimeType = getTypeForFile(file);
        if(MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType)){
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_PATH, file.getAbsolutePath());
        row.add(Document.COLUMN_FLAGS, flags);
        // Only publish dates reasonably after epoch
        long lastModified = file.getLastModified();
        if (lastModified > 31536000000L) {
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        }
/*        if (file.isDirectory() && null != file.list()) {
            row.add(Document.COLUMN_SUMMARY, file.list().length + " files");
        }*/
    }
    
    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        synchronized (mRootsLock) {
            for (RootInfo root : mRoots.values()) {
                final RowBuilder row = result.newRow();
                row.add(Root.COLUMN_ROOT_ID, root.rootId);
                row.add(Root.COLUMN_FLAGS, root.flags);
                row.add(Root.COLUMN_TITLE, root.title);
                row.add(Root.COLUMN_PATH, root.path);
                row.add(Root.COLUMN_DOCUMENT_ID, root.docId);
            }
        }
        return result;
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName)
            throws FileNotFoundException {
        final RootFile parent = getRootFileForDocId(docId);
        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent document isn't a directory");
        }

        File file;
        String path = parent.getPath();
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            file = new File(parent.getPath(), displayName);
            if (!RootCommands.createRootdir(path, displayName)) {
                throw new IllegalStateException("Failed to mkdir " + file);
            }
        } else {
            displayName = FileUtils.removeExtension(mimeType, displayName);
            file = new File(path, FileUtils.addExtension(mimeType, displayName));

            // If conflicting file, try adding counter suffix
            int n = 0;
            while (file.exists() && n++ < 32) {
                file = new File(path, FileUtils.addExtension(mimeType, displayName + " (" + n + ")"));
            }

            try {
                if (!RootCommands.createRootFile(path, file.getName())) {
                    throw new IllegalStateException("Failed to touch " + file);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to touch " + file + ": " + e);
            }
        }
        notifyDocumentsChanged(docId);
        return getDocIdForRootFile(new RootFile(path, displayName));
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        // Since this provider treats renames as generating a completely new
        // docId, we're okay with letting the MIME type change.
        displayName = FileUtils.buildValidFatFilename(displayName);

        final RootFile before = getRootFileForDocId(documentId);
        final RootFile after = new RootFile(before.getParent(), displayName);

        if(!RootCommands.renameRootTarget(before, after)){
            throw new IllegalStateException("Failed to rename " + before);
        }
        final String afterDocId = getDocIdForRootFile(new RootFile(after.getParent(), displayName));
        if (!TextUtils.equals(documentId, afterDocId)) {
            notifyDocumentsChanged(documentId);
            return afterDocId;
        } else {
            return null;
        }
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        final RootFile before = getRootFileForDocId(sourceDocumentId);
        final RootFile after = getRootFileForDocId(targetParentDocumentId);
        if (!RootCommands.moveCopyRoot(before.getPath(), after.getPath())) {
            throw new IllegalStateException("Failed to copy " + before);
        }
        final String afterDocId = getDocIdForRootFile(after);
        notifyDocumentsChanged(afterDocId);
        return afterDocId;
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {

        final RootFile before = getRootFileForDocId(sourceDocumentId);
        final RootFile after = new RootFile(getRootFileForDocId(targetParentDocumentId).getPath(), before.getName());

        if(!RootCommands.renameRootTarget(before, after)){
            throw new IllegalStateException("Failed to rename " + before);
        }
        final String afterDocId = getDocIdForRootFile(after);
        if (!TextUtils.equals(sourceDocumentId, afterDocId)) {
            notifyDocumentsChanged(afterDocId);
            return afterDocId;
        } else {
            return null;
        }
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        final RootFile file = getRootFileForDocId(docId);
        if (!RootCommands.deleteFileRoot(file.getPath())) {
            throw new IllegalStateException("Failed to delete " + file);
        }
        notifyDocumentsChanged(docId);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeRootFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final RootFile parent = getRootFileForDocId(parentDocumentId);
        final MatrixCursor result = new DirectoryCursor(
                resolveDocumentProjection(projection), parentDocumentId, parent);
        try {
            ArrayList<String> listFiles = RootCommands.listFiles(parent.getPath());
            for (String line : listFiles){
                includeRootFile(result, null, new RootFile(parent, line));
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
        return result;
    }

    @SuppressWarnings("unused")
	@Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        final RootFile parent;
        synchronized (mRootsLock) {
            parent = mRoots.get(rootId).path;
        }

        try {
            ArrayList<String> listFiles = RootCommands.findFiles(parent.getPath(), query);
            for (String line : listFiles){
                includeRootFile(result, null, new RootFile(parent, line));
            }

        } catch (Exception e) {
            CrashReportingManager.logException(e);
        }
        return result;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        final RootFile file = getRootFileForDocId(documentId);
        return getTypeForFile(file);
    }

    @Override
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        final RootFile file = getRootFileForDocId(documentId);
        InputStream is = RootCommands.getFile(file.getPath());

        try {
            return ParcelFileDescriptorUtil.pipeFrom(is);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ParcelFileDescriptor.open(new File(file.getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(
            String documentId, Point sizeHint, CancellationSignal signal)
            throws FileNotFoundException {
        return openOrCreateDocumentThumbnail(documentId, sizeHint, signal);
    }

    public AssetFileDescriptor openOrCreateDocumentThumbnail(
            String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
//        final ContentResolver resolver = getContext().getContentResolver();
        final RootFile file = getRootFileForDocId(docId);
        final String mimeType = getTypeForFile(file);

        final String typeOnly = mimeType.split("/")[0];

        final long token = Binder.clearCallingIdentity();
        try {
            if (MediaDocumentsProvider.TYPE_AUDIO.equals(typeOnly)) {
                final long id = getAlbumForPathCleared(file.getPath());
                return openOrCreateAudioThumbnailCleared(id, signal);
            } else if (MediaDocumentsProvider.TYPE_IMAGE.equals(typeOnly)) {
                final long id = getImageForPathCleared(file.getPath());
                return openOrCreateImageThumbnailCleared(id, signal);
            } else if (MediaDocumentsProvider.TYPE_VIDEO.equals(typeOnly)) {
                final long id = getVideoForPathCleared(file.getPath());
                return openOrCreateVideoThumbnailCleared(id, signal);
            } else {
            	return null;//DocumentsContract.openImageThumbnail(file);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static String getTypeForFile(RootFile file) {
        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        } else {
            return FileUtils.getTypeForName(file.getName());
        }
    }

    private class DirectoryCursor extends MatrixCursor {

        public DirectoryCursor(String[] columnNames, String docId, RootFile file) {
            super(columnNames);
            final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
            setNotificationUri(getContext().getContentResolver(), notifyUri);
        }

        @Override
        public void close() {
            super.close();
        }
    }

    private void notifyDocumentsChanged(String docId){
        final String rootId = getParentRootIdForDocId(docId);
        Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        getContext().getContentResolver().notifyChange(uri, null, false);
    }
}