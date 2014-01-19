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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.CancellationSignal;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.StorageVolume;
import dev.dworks.apps.anexplorer.misc.StorageUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.model.GuardedBy;

public class ExternalStorageProvider extends DocumentsProvider {
    private static final String TAG = "ExternalStorage";

    private static final boolean LOG_INOTIFY = false;

    public static final String AUTHORITY = "dev.dworks.apps.anexplorer.externalstorage.documents";

    // docId format: root:path/to/file

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES,
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
    }

    public static final String ROOT_ID_PRIMARY_EMULATED = "primary";
    public static final String ROOT_ID_SECONDARY = "secondary";

    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayList<RootInfo> mRoots;
    @GuardedBy("mRootsLock")
    private HashMap<String, RootInfo> mIdToRoot;
    @GuardedBy("mRootsLock")
    private HashMap<String, File> mIdToPath;

    @GuardedBy("mObservers")
    private Map<File, DirectoryObserver> mObservers = Maps.newHashMap();

    @Override
    public boolean onCreate() {
        mRoots = Lists.newArrayList();
        mIdToRoot = Maps.newHashMap();
        mIdToPath = Maps.newHashMap();

        updateVolumes();
        includeOtherRoot();
        return true;
    }

    public void updateVolumes() {
        synchronized (mRootsLock) {
            updateVolumesLocked();
            includeOtherRoot();
        }
    }

    private void updateVolumesLocked() {
        mRoots.clear();
        mIdToPath.clear();
        mIdToRoot.clear();
        
        int count = 0;
        StorageUtils storageUtils = new StorageUtils(getContext());
        for (StorageVolume volume : storageUtils.getStorageMounts()) {
            final File path = volume.getPathFile();
            if(Utils.hasKitKat()){
	        	String state = Environment.getStorageState(path);
	            final boolean mounted = Environment.MEDIA_MOUNTED.equals(state)
	                    || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
	            if (!mounted) continue;
            }
            final String rootId;
            if (volume.isPrimary && volume.isEmulated) {
                rootId = ROOT_ID_PRIMARY_EMULATED;
            } else if (volume.getUuid() != null) {
                rootId = ROOT_ID_SECONDARY + volume.getLabel();
            } else {
                Log.d(TAG, "Missing UUID for " + volume.getPath() + "; skipping");
                continue;
            }

            if (mIdToPath.containsKey(rootId)) {
                Log.w(TAG, "Duplicate UUID " + rootId + "; skipping");
                continue;
            }

            try {
            	if(null == path.listFiles()){
            		continue;
            	}
                mIdToPath.put(rootId, path);
                final RootInfo root = new RootInfo();
                
                root.rootId = rootId;
                root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_SEARCH;
                if (ROOT_ID_PRIMARY_EMULATED.equals(rootId)) {
                    root.title = getContext().getString(R.string.root_internal_storage);
                } else {
                	count++;
                    root.title = getContext().getString(R.string.root_external_storage) + " " + count;// + volume.getLabel();
                }
                root.docId = getDocIdForFile(path);
                mRoots.add(root);
                mIdToRoot.put(rootId, root);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        Log.d(TAG, "After updating volumes, found " + mRoots.size() + " active roots");

        getContext().getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }
    
    private void includeOtherRoot() {
    	try {
            final String rootId = "phone";
            final File path = Environment.getRootDirectory();
            mIdToPath.put(rootId, path);

            final RootInfo root = new RootInfo();
            root.rootId = rootId;
            root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            root.title = getContext().getString(R.string.root_phone_storage);
            root.docId = getDocIdForFile(path);
            mRoots.add(root);
            mIdToRoot.put(rootId, root);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    	try {
            final String rootId = "download";
            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            mIdToPath.put(rootId, path);

            final RootInfo root = new RootInfo();
            root.rootId = rootId;
            root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            root.title = getContext().getString(R.string.root_downloads);
            root.docId = getDocIdForFile(path);
            mRoots.add(root);
            mIdToRoot.put(rootId, root);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    	try {
            final String rootId = "bluetooth";
            File path = Environment.getExternalStoragePublicDirectory("Bluetooth");
            if(null == path){
            	path = Environment.getExternalStoragePublicDirectory("Download/Bluetooth");
            }
            if(null != path){
                mIdToPath.put(rootId, path);

                final RootInfo root = new RootInfo();
                root.rootId = rootId;
                root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                        | Root.FLAG_SUPPORTS_SEARCH;
                root.title = getContext().getString(R.string.root_bluetooth);
                root.docId = getDocIdForFile(path);
                mRoots.add(root);
                mIdToRoot.put(rootId, root);	
            }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private String getDocIdForFile(File file) throws FileNotFoundException {
        String path = file.getAbsolutePath();

        // Find the most-specific root path
        Map.Entry<String, File> mostSpecific = null;
        synchronized (mRootsLock) {
            for (Map.Entry<String, File> root : mIdToPath.entrySet()) {
                final String rootPath = root.getValue().getPath();
                if (path.startsWith(rootPath) && (mostSpecific == null
                        || rootPath.length() > mostSpecific.getValue().getPath().length())) {
                    mostSpecific = root;
                }
            }
        }

        if (mostSpecific == null) {
            throw new FileNotFoundException("Failed to find root that contains " + path);
        }

        // Start at first char of path under root
        final String rootPath = mostSpecific.getValue().getPath();
        if (rootPath.equals(path)) {
            path = "";
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length());
        } else {
            path = path.substring(rootPath.length() + 1);
        }

        return mostSpecific.getKey() + ':' + path;
    }

    private File getFileForDocId(String docId) throws FileNotFoundException {
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);
        final String path = docId.substring(splitIndex + 1);

        File target;
        synchronized (mRootsLock) {
            target = mIdToPath.get(tag);
        }
        if (target == null) {
            throw new FileNotFoundException("No root for " + tag);
        }
        if (!target.exists()) {
            target.mkdirs();
        }
        target = new File(target, path);
        if (!target.exists()) {
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

        int flags = 0;

        if (file.canWrite()) {
            if (file.isDirectory()) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
            } else {
                flags |= Document.FLAG_SUPPORTS_WRITE;
            }
            flags |= Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_EDIT ;
        }

        final String displayName = file.getName();
        final String mimeType = getTypeForFile(file);
        if (mimeType.startsWith("image/")
        		|| mimeType.startsWith("audio/")
        		|| mimeType.startsWith("video/") 
        		|| mimeType.startsWith("application/vnd.android.package-archive")) {
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
        	row.add(Document.COLUMN_SUMMARY, file.list().length + " files");
        }

        // Only publish dates reasonably after epoch
        long lastModified = file.lastModified();
        if (lastModified > 31536000000L) {
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        synchronized (mRootsLock) {
            for (String rootId : mIdToPath.keySet()) {
                final RootInfo root = mIdToRoot.get(rootId);
                final File path = mIdToPath.get(rootId);

                final RowBuilder row = result.newRow();
                row.add(Root.COLUMN_ROOT_ID, root.rootId);
                row.add(Root.COLUMN_FLAGS, root.flags);
                row.add(Root.COLUMN_TITLE, root.title);
                row.add(Root.COLUMN_DOCUMENT_ID, root.docId);
                if(ROOT_ID_PRIMARY_EMULATED.equals(root.rootId) || root.rootId.startsWith(ROOT_ID_SECONDARY)){
                	row.add(Root.COLUMN_AVAILABLE_BYTES, path.getFreeSpace());
                }
            }
        }
        return result;
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName)
            throws FileNotFoundException {
        final File parent = getFileForDocId(docId);
        File file;

        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            file = new File(parent, displayName);
            if (!file.mkdir()) {
                throw new IllegalStateException("Failed to mkdir " + file);
            }
        } else {
            displayName = removeExtension(mimeType, displayName);
            file = new File(parent, addExtension(mimeType, displayName));

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
        }
        return getDocIdForFile(file);
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        final File file = getFileForDocId(docId);
        if (!FileUtils.deleteFile(file)) {
            throw new IllegalStateException("Failed to delete " + file);
        }
    }
    
    @Override
    public void moveDocument(String documentIdFrom, String documentIdTo, boolean deleteAfter) throws FileNotFoundException {
    	final File fileFrom = getFileForDocId(documentIdFrom);
    	final File fileTo = getFileForDocId(documentIdTo);
        if (!FileUtils.moveFile(fileFrom, fileTo, null)) {
            throw new IllegalStateException("Failed to copy " + fileFrom);
        }
        else{
        	if (deleteAfter) {
                if (!FileUtils.deleteFile(fileFrom)) {
                    throw new IllegalStateException("Failed to delete " + fileFrom);
                }
			}
        }
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final File parent = getFileForDocId(parentDocumentId);
        final MatrixCursor result = new DirectoryCursor(
                resolveDocumentProjection(projection), parentDocumentId, parent);
        for (File file : parent.listFiles()) {
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
            parent = mIdToPath.get(rootId);
        }

/*        final LinkedList<File> pending = new LinkedList<File>();
        pending.add(parent);
        while (!pending.isEmpty() && result.getCount() < 24) {
            final File file = pending.removeFirst();
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    pending.add(child);
                }
            }
            if (file.getName().toLowerCase().contains(query)) {
                includeFile(result, null, file);
            }
        }*/
        for (File file : FileUtils.searchDirectory(parent.getPath(), query)) {
        	includeFile(result, null, file);
		}
        return result;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        return getTypeForFile(file);
    }

    @Override
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);//ParcelFileDescriptor.parseMode(mode));
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
        final File file = getFileForDocId(docId);
        final String mimeType = getTypeForFile(file);
    	
        final String typeOnly = mimeType.split("/")[0];
    
        final long token = Binder.clearCallingIdentity();
        try {
            if ("audio".equals(typeOnly)) {
                final long id = getAlbumForPathCleared(file.getPath());
                return openOrCreateAudioThumbnailCleared(id, signal);
            } else if ("image".equals(typeOnly)) {
                final long id = getImageForPathCleared(file.getPath());
                return openOrCreateImageThumbnailCleared(id, signal);
            } else if ("video".equals(typeOnly)) {
                final long id = getVideoForPathCleared(file.getPath());
                return openOrCreateVideoThumbnailCleared(id, signal);
            } else {
            	return DocumentsContract.openImageThumbnail(file);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }

        return "application/octet-stream";
    }

    /**
     * Remove file extension from name, but only if exact MIME type mapping
     * exists. This means we can reapply the extension later.
     */
    private static String removeExtension(String mimeType, String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
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

    private void startObserving(File file, Uri notifyUri) {
        synchronized (mObservers) {
            DirectoryObserver observer = mObservers.get(file);
            if (observer == null) {
                observer = new DirectoryObserver(
                        file, getContext().getContentResolver(), notifyUri);
                observer.startWatching();
                mObservers.put(file, observer);
            }
            observer.mRefCount++;

            if (LOG_INOTIFY) Log.d(TAG, "after start: " + observer);
        }
    }

    private void stopObserving(File file) {
        synchronized (mObservers) {
            DirectoryObserver observer = mObservers.get(file);
            if (observer == null) return;

            observer.mRefCount--;
            if (observer.mRefCount == 0) {
                mObservers.remove(file);
                observer.stopWatching();
            }

            if (LOG_INOTIFY) Log.d(TAG, "after stop: " + observer);
        }
    }

    private static class DirectoryObserver extends FileObserver {
        private static final int NOTIFY_EVENTS = ATTRIB | CLOSE_WRITE | MOVED_FROM | MOVED_TO
                | CREATE | DELETE | DELETE_SELF | MOVE_SELF;

        private final File mFile;
        private final ContentResolver mResolver;
        private final Uri mNotifyUri;

        private int mRefCount = 0;

        public DirectoryObserver(File file, ContentResolver resolver, Uri notifyUri) {
            super(file.getAbsolutePath(), NOTIFY_EVENTS);
            mFile = file;
            mResolver = resolver;
            mNotifyUri = notifyUri;
        }

        @Override
        public void onEvent(int event, String path) {
            if ((event & NOTIFY_EVENTS) != 0) {
                if (LOG_INOTIFY) Log.d(TAG, "onEvent() " + event + " at " + path);
                mResolver.notifyChange(mNotifyUri, null, false);
            }
        }

        @Override
        public String toString() {
            return "DirectoryObserver{file=" + mFile.getAbsolutePath() + ", ref=" + mRefCount + "}";
        }
    }

    private class DirectoryCursor extends MatrixCursor {
        private final File mFile;

        public DirectoryCursor(String[] columnNames, String docId, File file) {
            super(columnNames);

            final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
            setNotificationUri(getContext().getContentResolver(), notifyUri);

            mFile = file;
            startObserving(mFile, notifyUri);
        }

        @Override
        public void close() {
            super.close();
            stopObserving(mFile);
        }
    }
    
    private interface AudioAlbumThumbnailQuery {
        final String[] PROJECTION = new String[] {
        		Audio.Media._ID,
        		Audio.Media.ALBUM_ID,
        		Audio.Media.DATE_MODIFIED };

        final int _ID = 0;
        final int ALBUM_ID = 1;
        final int DATE_MODIFIED = 2;
    }

    private long getAlbumForPathCleared(String path) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
            		AudioAlbumThumbnailQuery.PROJECTION, Audio.Media.DATA + " LIKE ?",
            		new String[] { path.replaceAll("'", "''") }, Audio.Media.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(AudioAlbumThumbnailQuery.ALBUM_ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No Audio found for album");
    }

    private interface AudioThumbnailQuery {
        final String[] PROJECTION = new String[] {
                Audio.Albums.ALBUM_ART };

        final int _DATA = 0;
    }

    private ParcelFileDescriptor openAudioThumbnailCleared(long id, CancellationSignal signal)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(Audio.Albums.EXTERNAL_CONTENT_URI,
            		AudioThumbnailQuery.PROJECTION, Audio.Albums._ID + "=" + id,
                    null, null);
            if (cursor.moveToFirst()) {
                final String data = cursor.getString(AudioThumbnailQuery._DATA);
                return ParcelFileDescriptor.open(
                        new File(data), ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    private AssetFileDescriptor openOrCreateAudioThumbnailCleared(
            long id, CancellationSignal signal) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        ParcelFileDescriptor pfd = openAudioThumbnailCleared(id, signal);
        if (pfd == null) {
            // No thumbnail yet, so generate. This is messy, since we drop the
            // Bitmap on the floor, but its the least-complicated way.
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Images.Thumbnails.getThumbnail(resolver, id, Images.Thumbnails.MINI_KIND, opts);

            pfd = openAudioThumbnailCleared(id, signal);
        }

        if (pfd == null) {
            // Phoey, fallback to full image
            final Uri fullUri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id);
            pfd = resolver.openFileDescriptor(fullUri, "r");
        }

        final int orientation = queryOrientationForImage(id, signal);
        final Bundle extras;
        if (orientation != 0) {
            extras = new Bundle(1);
            extras.putInt(DocumentsContract.EXTRA_ORIENTATION, orientation);
        } else {
            extras = null;
        }

        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }


    private interface ImagesBucketThumbnailQuery {
        final String[] PROJECTION = new String[] {
                ImageColumns._ID,
                ImageColumns.BUCKET_ID,
                ImageColumns.DATE_MODIFIED };

        final int _ID = 0;
        final int BUCKET_ID = 1;
        final int DATE_MODIFIED = 2;
    }

    private long getImageForPathCleared(String path) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Media.EXTERNAL_CONTENT_URI,
                    ImagesBucketThumbnailQuery.PROJECTION, ImageColumns.DATA + "= ? ",
                    new String[] { path.replaceAll("'", "''") }, ImageColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(ImagesBucketThumbnailQuery._ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No image found for bucket");
    }

    private interface ImageThumbnailQuery {
        final String[] PROJECTION = new String[] {
                Images.Thumbnails.DATA };

        final int _DATA = 0;
    }

    private ParcelFileDescriptor openImageThumbnailCleared(long id, CancellationSignal signal)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    ImageThumbnailQuery.PROJECTION, Images.Thumbnails.IMAGE_ID + "=" + id, null,
                    null);
            if (cursor.moveToFirst()) {
                final String data = cursor.getString(ImageThumbnailQuery._DATA);
                return ParcelFileDescriptor.open(
                        new File(data), ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    private AssetFileDescriptor openOrCreateImageThumbnailCleared(
            long id, CancellationSignal signal) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        ParcelFileDescriptor pfd = openImageThumbnailCleared(id, signal);
        if (pfd == null) {
            // No thumbnail yet, so generate. This is messy, since we drop the
            // Bitmap on the floor, but its the least-complicated way.
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Images.Thumbnails.getThumbnail(resolver, id, Images.Thumbnails.MINI_KIND, opts);

            pfd = openImageThumbnailCleared(id, signal);
        }

        if (pfd == null) {
            // Phoey, fallback to full image
            final Uri fullUri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id);
            pfd = resolver.openFileDescriptor(fullUri, "r");
        }

        final int orientation = queryOrientationForImage(id, signal);
        final Bundle extras;
        if (orientation != 0) {
            extras = new Bundle(1);
            extras.putInt(DocumentsContract.EXTRA_ORIENTATION, orientation);
        } else {
            extras = null;
        }

        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    private interface VideosBucketThumbnailQuery {
        final String[] PROJECTION = new String[] {
                VideoColumns._ID,
                VideoColumns.BUCKET_ID,
                VideoColumns.DATE_MODIFIED };

        final int _ID = 0;
        final int BUCKET_ID = 1;
        final int DATE_MODIFIED = 2;
    }

    private long getVideoForPathCleared(String path)throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Video.Media.EXTERNAL_CONTENT_URI,
                    VideosBucketThumbnailQuery.PROJECTION, VideoColumns.DATA + "=? ",
                    new String[] { path.replaceAll("'", "''") }, VideoColumns.DATE_MODIFIED + " DESC");
            if (cursor.moveToFirst()) {
                return cursor.getLong(VideosBucketThumbnailQuery._ID);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        throw new FileNotFoundException("No video found for bucket");
    }

    private interface VideoThumbnailQuery {
        final String[] PROJECTION = new String[] {
                Video.Thumbnails.DATA };

        final int _DATA = 0;
    }

    private AssetFileDescriptor openVideoThumbnailCleared(long id, CancellationSignal signal)
            throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    VideoThumbnailQuery.PROJECTION, Video.Thumbnails.VIDEO_ID + "=" + id, null,
                    null);
            if (cursor.moveToFirst()) {
                final String data = cursor.getString(VideoThumbnailQuery._DATA);
                return new AssetFileDescriptor(ParcelFileDescriptor.open(
                        new File(data), ParcelFileDescriptor.MODE_READ_ONLY), 0,
                        AssetFileDescriptor.UNKNOWN_LENGTH);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        return null;
    }

    private AssetFileDescriptor openOrCreateVideoThumbnailCleared(
            long id, CancellationSignal signal) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();

        AssetFileDescriptor afd = openVideoThumbnailCleared(id, signal);
        if (afd == null) {
            // No thumbnail yet, so generate. This is messy, since we drop the
            // Bitmap on the floor, but its the least-complicated way.
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Video.Thumbnails.getThumbnail(resolver, id, Video.Thumbnails.MINI_KIND, opts);

            afd = openVideoThumbnailCleared(id, signal);
        }

        return afd;
    }

    private interface ImageOrientationQuery {
        final String[] PROJECTION = new String[] {
                ImageColumns.ORIENTATION };

        final int ORIENTATION = 0;
    }

    private int queryOrientationForImage(long id, CancellationSignal signal) {
        final ContentResolver resolver = getContext().getContentResolver();

        Cursor cursor = null;
        try {
            cursor = resolver.query(Images.Media.EXTERNAL_CONTENT_URI,
                    ImageOrientationQuery.PROJECTION, ImageColumns._ID + "=" + id, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(ImageOrientationQuery.ORIENTATION);
            } else {
                Log.w(TAG, "Missing orientation data for " + id);
                return 0;
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }
}