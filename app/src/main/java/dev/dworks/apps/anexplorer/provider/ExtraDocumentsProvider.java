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
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Files.FileColumns;
import android.support.provider.DocumentFile;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.core.util.Pair;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.archive.DocumentArchiveHelper;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.ImageUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.ParcelFileDescriptorUtil;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.model.RootInfo;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.misc.FileUtils.getTypeForFile;
import static dev.dworks.apps.anexplorer.model.DocumentsContract.Document.MIME_TYPE_DIR;


/**
 * Presents a {@link DocumentsContract} view of {MediaProvider} external
 * contents.
 */
public class ExtraDocumentsProvider extends StorageProvider {
    @SuppressWarnings("unused")
	private static final String TAG = "ExtraDocumentsProvider";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".extra.documents";
    // docId format: root:id

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
        public File path;
        public File visiblePath;
    }

    public static final String ROOT_ID_WHATSAPP = "whatsapp";
    public static final String ROOT_ID_TELEGRAM = "telegram";
    public static final String ROOT_ID_TELEGRAMX = "telegramx";

    private Handler mHandler;

    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayMap<String, RootInfo> mRoots = new ArrayMap<>();

    @Override
    public boolean onCreate() {
        mHandler = new Handler();
        updateRoots();
        return super.onCreate();
    }

    @Override
    public void updateRoots() {
        synchronized (mRootsLock) {
            includeRoots();
            Log.d(TAG, "After updating volumes, found " + mRoots.size() + " active roots");
            notifyRootsChanged(getContext());
        }
    }

    private void includeRoots() {

        try {
            final String rootId = ROOT_ID_WHATSAPP;
            final String rootPath = Environment.getExternalStoragePublicDirectory("WhatsApp").getAbsolutePath()+"/Media";
            final File path = new File(rootPath);

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_whatsapp);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            final String rootId = ROOT_ID_TELEGRAM;
            final String rootPath = Environment.getExternalStoragePublicDirectory("Telegram").getAbsolutePath();
            final File path = new File(rootPath);

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_telegram);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            final String rootId = ROOT_ID_TELEGRAMX;
            final String rootPath = Environment.getExternalStoragePublicDirectory("Android").getAbsolutePath()
                    +"/data/org.thunderdog.challegram/files";
            final File path = new File(rootPath);

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_telegramx);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void notifyRootsChanged(Context context) {
        context.getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }

    public static void notifyDocumentsChanged(Context context, String rootId) {
        Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        context.getContentResolver().notifyChange(uri, null, false);
    }

    private String getDocIdForFile(File file) throws FileNotFoundException {
        return getDocIdForFileMaybeCreate(file, false);
    }

    private String getDocIdForFileMaybeCreate(File file, boolean createNewDir)
            throws FileNotFoundException {
        String path = file.getAbsolutePath();

        // Find the most-specific root path
        boolean visiblePath = false;
        RootInfo mostSpecificRoot = getMostSpecificRootForPath(path, false);

        if (mostSpecificRoot == null) {
            // Try visible path if no internal path matches. MediaStore uses visible paths.
            visiblePath = true;
            mostSpecificRoot = getMostSpecificRootForPath(path, true);
        }

        if (mostSpecificRoot == null) {
            throw new FileNotFoundException("Failed to find root that contains " + path);
        }

        // Start at first char of path under root
        final String rootPath = visiblePath
                ? mostSpecificRoot.visiblePath.getAbsolutePath()
                : mostSpecificRoot.path.getAbsolutePath();
        if (rootPath.equals(path)) {
            path = "";
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length());
        } else {
            path = path.substring(rootPath.length() + 1);
        }

        if (!file.exists() && createNewDir) {
            Log.i(TAG, "Creating new directory " + file);
            if (!file.mkdir()) {
                Log.e(TAG, "Could not create directory " + file);
            }
        }

        return mostSpecificRoot.rootId + ':' + path;
    }

    private RootInfo getMostSpecificRootForPath(String path, boolean visible) {
        // Find the most-specific root path
        RootInfo mostSpecificRoot = null;
        String mostSpecificPath = null;
        synchronized (mRootsLock) {
            for (int i = 0; i < mRoots.size(); i++) {
                final RootInfo root = mRoots.valueAt(i);
                final File rootFile = visible ? root.visiblePath : root.path;
                if (rootFile != null) {
                    final String rootPath = rootFile.getAbsolutePath();
                    if (path.startsWith(rootPath) && (mostSpecificPath == null
                            || rootPath.length() > mostSpecificPath.length())) {
                        mostSpecificRoot = root;
                        mostSpecificPath = rootPath;
                    }
                }
            }
        }

        return mostSpecificRoot;
    }

    protected final File getFileForDocId(String docId) throws FileNotFoundException {
        return getFileForDocId(docId, false);
    }

    protected File getFileForDocId(String docId, boolean visible) throws FileNotFoundException {
        return getFileForDocId(docId, visible, true);
    }

    private File getFileForDocId(String docId, boolean visible, boolean mustExist)
            throws FileNotFoundException {
        RootInfo root = getRootFromDocId(docId);
        return buildFile(root, docId, visible, mustExist);
    }

    private Pair<RootInfo, File> resolveDocId(String docId, boolean visible)
            throws FileNotFoundException {
        RootInfo root = getRootFromDocId(docId);
        return Pair.create(root, buildFile(root, docId, visible, true));
    }

    private RootInfo getRootFromDocId(String docId) throws FileNotFoundException {
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);

        RootInfo root;
        synchronized (mRootsLock) {
            root = mRoots.get(tag);
        }
        if (root == null) {
            throw new FileNotFoundException("No root for " + tag);
        }

        return root;
    }

    private File buildFile(RootInfo root, String docId, boolean visible, boolean mustExist)
            throws FileNotFoundException {
        final int splitIndex = docId.indexOf(':', 1);
        final String path = docId.substring(splitIndex + 1);

        File target = visible ? root.visiblePath : root.path;
        if (target == null) {
            return null;
        }
        if (!target.exists()) {
            target.mkdirs();
        }
        target = new File(target, path);
        if (mustExist && !target.exists()) {
            throw new FileNotFoundException("Missing file for " + docId + " at " + target);
        }
        return target;
    }

    private void includeFile(MatrixCursor result, String docId, File file)
            throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }

        DocumentFile documentFile = getDocumentFile(docId, file);

        int flags = 0;

        if (documentFile.canWrite()) {
            if (file.isDirectory()) {
                // flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
            } else {
                // flags |= Document.FLAG_SUPPORTS_WRITE;
                flags |= Document.FLAG_SUPPORTS_DELETE;
                flags |= Document.FLAG_SUPPORTS_RENAME;
                flags |= Document.FLAG_SUPPORTS_MOVE;
                flags |= Document.FLAG_SUPPORTS_COPY;
                flags |= Document.FLAG_SUPPORTS_EDIT;
            }
            flags |= Document.FLAG_SUPPORTS_BOOKMARK;
            flags |= Document.FLAG_DIR_PREFERS_GRID;
        }

        final String mimeType = getTypeForFile(file);

        String displayName = file.getName();
        if (!TextUtils.isEmpty(displayName)) {
            if(displayName.charAt(0) == '.'){
                return;
            }
            displayName = displayName.replaceAll("\\b( ?WhatsApp|Telegram)\\b", "");
        }
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
        if(file.isDirectory() && null != file.list()){
            row.add(Document.COLUMN_SUMMARY, FileUtils.formatFileCount(file.list().length));
        }

        // Only publish dates reasonably after epoch
        long lastModified = file.lastModified();
        if (lastModified > 31536000000L) {
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        }
    }

    private DocumentFile getDocumentFile(String docId, File file) throws FileNotFoundException {
        return DocumentsApplication.getSAFManager(getContext()).getDocumentFile(docId, file);
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
        synchronized (mRootsLock) {
            for (RootInfo root : mRoots.values()) {
                final RowBuilder row = result.newRow();
                row.add(Root.COLUMN_ROOT_ID, root.rootId);
                row.add(Root.COLUMN_FLAGS, root.flags);
                row.add(Root.COLUMN_TITLE, root.title);
                row.add(Root.COLUMN_DOCUMENT_ID, root.docId);
                row.add(Root.COLUMN_PATH, root.path);
            }
        }
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final File parent = getFileForDocId(parentDocumentId);
        final MatrixCursor result = new MatrixCursor(
                resolveDocumentProjection(projection));
        for (File file : parent.listFiles()) {
            if (file.getName().startsWith("temp") || isEmptyFolder(file)){
                continue;
            }
            includeFile(result, null, file);
        }
        return result;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        final File parent;
        synchronized (mRootsLock) {
            parent = mRoots.get(rootId).path;
        }
        for (File file : FileUtils.searchDirectory(parent.getPath(), query)) {
            includeFile(result, null, file);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {

        final File file = getFileForDocId(documentId);
        if(Utils.hasKitKat()){
            final int pfdMode = ParcelFileDescriptor.parseMode(mode);
            if (pfdMode == ParcelFileDescriptor.MODE_READ_ONLY) {
                return ParcelFileDescriptor.open(file, pfdMode);
            } else {
                try {
                    // When finished writing, kick off media scanner
                    return ParcelFileDescriptor.open(file, pfdMode, mHandler, new ParcelFileDescriptor.OnCloseListener() {
                        @Override
                        public void onClose(IOException e) {
                            FileUtils.updateMediaStore(getContext(), file.getPath());
                        }
                    });
                } catch (IOException e) {
                    throw new FileNotFoundException("Failed to open for writing: " + e);
                }
            }
        }
        else{
            return ParcelFileDescriptor.open(file, ParcelFileDescriptorUtil.parseMode(mode));
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

    @Override
    public AssetFileDescriptor openDocumentThumbnail(
            String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        return openOrCreateDocumentThumbnail(documentId, sizeHint, signal);
    }

    public AssetFileDescriptor openOrCreateDocumentThumbnail(
            String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
//        final ContentResolver resolver = getContext().getContentResolver();
        final File file = getFileForDocId(docId);
        final String mimeType = getTypeForFile(file);

        final String typeOnly = mimeType.split("/")[0];

        final long token = Binder.clearCallingIdentity();
        try {
            if (file.isDirectory()) {

                final File previewFile = FileUtils.getPreviewFile(file);
                if (null != previewFile){
                    return null;
                }
                final String mimeTypePreview = getTypeForFile(previewFile);
                final String typeOnlyPreview = mimeTypePreview.split("/")[0];
                if (docId.startsWith(ROOT_ID_WHATSAPP)){
                    return getMediaThumbnail(previewFile, typeOnlyPreview, signal);
                } else {
                    return null;
                }
            } else {
                return getMediaThumbnail(file, typeOnly, signal);
            }
        } catch (Exception e){
            return DocumentsContract.openImageThumbnail(file);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private AssetFileDescriptor getMediaThumbnail(File file, String typeOnly,
                                                  CancellationSignal signal) throws FileNotFoundException {
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
            return DocumentsContract.openImageThumbnail(file);
        }
    }

    private boolean isEmpty(File file) {
        return null != file  && (!file.isDirectory() || null == file.list() || file.list().length == 0);
    }

    private boolean isEmptyFolder(File file) {
        if(null != file  && file.isDirectory()){
            String[] list = file.list();
            if (null == file.list()){
              return true;
            }
            int count  = list.length;
            if (count == 0){
                return true;
            } else if (count == 1){
                return list[0].compareToIgnoreCase(".nomedia") == 0;
            }
        }
        return false;
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
