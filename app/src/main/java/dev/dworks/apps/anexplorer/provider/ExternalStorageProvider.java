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
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.core.os.EnvironmentCompat;
import androidx.core.util.Pair;
import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.archive.DocumentArchiveHelper;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.DiskInfo;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.ParcelFileDescriptorUtil;
import dev.dworks.apps.anexplorer.misc.StorageUtils;
import dev.dworks.apps.anexplorer.misc.StorageVolume;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.misc.VolumeInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.misc.FileUtils.getTypeForFile;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorString;
import static dev.dworks.apps.anexplorer.provider.UsbStorageProvider.ROOT_ID_USB;

@SuppressLint("DefaultLocale")
public class ExternalStorageProvider extends StorageProvider {
    private static final String TAG = "ExternalStorage";

    private static final boolean LOG_INOTIFY = false;

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".externalstorage.documents";
    public static final String DOWNLOAD_AUTHORITY = "com.android.providers.downloads.documents";
    // docId format: root:path/to/file

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_CAPACITY_BYTES, Root.COLUMN_PATH,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_SUMMARY,
    };
    private boolean showFilesHidden;

    private static class RootInfo {
        public String rootId;
        public int flags;
        public String title;
        public String docId;
        public File path;
        public File visiblePath;
        public boolean reportAvailableBytes;
    }

    public static final String ROOT_ID_HOME = "home";
    public static final String ROOT_ID_PRIMARY_EMULATED = "primary";
    public static final String ROOT_ID_SECONDARY = "secondary";
    public static final String ROOT_ID_DEVICE = "device";
    public static final String ROOT_ID_DOWNLOAD = "download";
    public static final String ROOT_ID_BLUETOOTH = "bluetooth";
    public static final String ROOT_ID_APP_BACKUP = "app_backup";
    public static final String ROOT_ID_RECIEVE_FLES = "receive_files";
    public static final String ROOT_ID_HIDDEN = "hidden";
    public static final String ROOT_ID_BOOKMARK = "bookmark";

    private static final String DIR_ROOT = "/";

    private Handler mHandler;

    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayMap<String, RootInfo> mRoots = new ArrayMap<>();

    @GuardedBy("mObservers")
    private ArrayMap<File, DirectoryObserver> mObservers = new ArrayMap<>();

    @Override
    public boolean onCreate() {
        mHandler = new Handler();
        updateRoots();
        updateSettings();

        return super.onCreate();
    }

    @Override
    public void updateRoots() {
        synchronized (mRootsLock) {
            if(Utils.hasMarshmallow() && !Utils.isWatch(getContext())) {
                updateVolumesLocked2();
            } else {
                updateVolumesLocked();
            }
            includeOtherRoot();
            includeBookmarkRoot();
            Log.d(TAG, "After updating volumes, found " + mRoots.size() + " active roots");
            notifyRootsChanged(getContext());
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void updateVolumesLocked() {
        mRoots.clear();

        int count = 0;
        StorageUtils storageUtils = new StorageUtils(getContext());
        for (StorageVolume storageVolume : storageUtils.getStorageMounts()) {
            final File path = storageVolume.getPathFile();
            String state = EnvironmentCompat.getStorageState(path);
            final boolean mounted = Environment.MEDIA_MOUNTED.equals(state)
                    || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
            if (!mounted) continue;

            final String rootId;
            final String title;
            if (storageVolume.isPrimary()) {
                rootId = ROOT_ID_PRIMARY_EMULATED;
                title = getContext().getString(R.string.root_internal_storage);
            } else if (storageVolume.getUuid() != null) {
                rootId = ROOT_ID_SECONDARY + storageVolume.getUuid();
                String label = storageVolume.getUserLabel();
                title = !TextUtils.isEmpty(label) ? label
                        : getContext().getString(R.string.root_external_storage)
                        + (count > 0 ? " " + count : "");
                count++;
            } else {
                Log.d(TAG, "Missing UUID for " + storageVolume.getPath() + "; skipping");
                continue;
            }

            if (mRoots.containsKey(rootId)) {
                Log.w(TAG, "Duplicate UUID " + rootId + "; skipping");
                continue;
            }

            try {
            	if(null == path.listFiles()){
            		continue;
            	}
                final RootInfo root = new RootInfo();
                mRoots.put(rootId, root);

                root.rootId = rootId;
                root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                        | Root.FLAG_SUPPORTS_SEARCH | Root.FLAG_SUPPORTS_IS_CHILD;
                if(storageVolume.getState().equals(Environment.MEDIA_MOUNTED)) {
                    root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT;
                }
                root.title = title;
                root.path = path;
                root.docId = getDocIdForFile(path);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void updateVolumesLocked2() {
        mRoots.clear();

        VolumeInfo primaryVolume = null;
        final int userId = 0;//UserHandle.myUserId();
        StorageUtils storageUtils = new StorageUtils(getContext());
        for (VolumeInfo volume : storageUtils.getVolumes()) {
            if (!volume.isMountedReadable()) continue;
            final String rootId;
            final String title;
            if (volume.getType() == VolumeInfo.TYPE_EMULATED) {
                // We currently only support a single emulated volume mounted at
                // a time, and it's always considered the primary
                rootId = ROOT_ID_PRIMARY_EMULATED;
                if (VolumeInfo.ID_EMULATED_INTERNAL.equals(volume.getId())) {
                    title =  getContext().getString(R.string.root_internal_storage);
                } else {
                    // This should cover all other storage devices, like an SD card
                    // or USB OTG drive plugged in. Using getBestVolumeDescription()
                    // will give us a nice string like "Samsung SD card" or "SanDisk USB drive"
                    final VolumeInfo privateVol = storageUtils.findPrivateForEmulated(volume);
                    title = StorageUtils.getBestVolumeDescription(getContext(), privateVol);
                }
            } else if (volume.getType() == VolumeInfo.TYPE_PUBLIC) {
                rootId = ROOT_ID_SECONDARY + volume.getFsUuid();
                title = StorageUtils.getBestVolumeDescription(getContext(), volume);
            } else {
                // Unsupported volume; ignore
                continue;
            }
            if (TextUtils.isEmpty(rootId)) {
                Log.d(TAG, "Missing UUID for " + volume.getId() + "; skipping");
                continue;
            }
            if (mRoots.containsKey(rootId)) {
                Log.w(TAG, "Duplicate UUID " + rootId + " for " + volume.getId() + "; skipping");
                continue;
            }
            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_LOCAL_ONLY
                    | Root.FLAG_SUPPORTS_SEARCH | Root.FLAG_SUPPORTS_IS_CHILD;
            final DiskInfo disk = volume.getDisk();

            Log.d(TAG, "Disk for root " + rootId + " is " + disk);
            if (disk != null && disk.isSd()) {
                root.flags |= Root.FLAG_REMOVABLE_SD;
            } else if (disk != null && disk.isUsb()) {
                root.flags |= Root.FLAG_REMOVABLE_USB;
            }
            if (volume.isPrimary()) {
                // save off the primary volume for subsequent "Home" dir initialization.
                primaryVolume = volume;
                root.flags |= Root.FLAG_ADVANCED;
            }
            // Dunno when this would NOT be the case, but never hurts to be correct.
            if (volume.isMountedWritable()) {
                root.flags |= Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT;
            }
            root.title = title;
            if (volume.getType() == VolumeInfo.TYPE_PUBLIC) {
                root.flags |= Root.FLAG_HAS_SETTINGS;
            }
            root.path = volume.getPathForUser(userId);
            root.visiblePath = volume.getPathForUser(userId);
            try {
                root.docId = getDocIdForFile(root.path);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        // Finally, if primary storage is available we add the "Documents" directory.
        // If I recall correctly the actual directory is created on demand
        // by calling either getPathForUser, or getInternalPathForUser.
        if (primaryVolume != null && primaryVolume.isVisible()) {
            final RootInfo root = new RootInfo();
            mRoots.put(root.rootId, root);

            root.rootId = ROOT_ID_HOME;
            root.title = getContext().getString(R.string.root_documents);
            // Only report bytes on *volumes*...as a matter of policy.
            root.reportAvailableBytes = false;
            root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_SEARCH
                    | Root.FLAG_SUPPORTS_IS_CHILD;
            // Dunno when this would NOT be the case, but never hurts to be correct.
            if (primaryVolume.isMountedWritable()) {
                root.flags |= Root.FLAG_SUPPORTS_CREATE;
            }
            // Create the "Documents" directory on disk (don't use the localized title).
            root.visiblePath = new File(
                    primaryVolume.getPathForUser(userId), Environment.DIRECTORY_DOCUMENTS);
            root.path = new File(
                    primaryVolume.getInternalPathForUser(userId), Environment.DIRECTORY_DOCUMENTS);
            try {
                root.docId = getDocIdForFile(root.path);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        // Note this affects content://com.android.externalstorage.documents/root/39BD-07C5
        // as well as content://com.android.externalstorage.documents/document/*/children,
        // so just notify on content://com.android.externalstorage.documents/.
    }

    private void includeOtherRoot() {
    	try {
            final String rootId = ROOT_ID_DEVICE;
            final File path = Utils.hasNougat() ? Environment.getRootDirectory() : new File(DIR_ROOT);

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPER_ADVANCED | Root.FLAG_SUPPORTS_SEARCH ;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_device_storage);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    	try {
            final String rootId = ROOT_ID_DOWNLOAD;
            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_downloads);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		}

        try {
            final String rootId = ROOT_ID_APP_BACKUP;
            final File path = Environment.getExternalStoragePublicDirectory("AppBackup");

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_app_backup);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    	
    	try {
            final String rootId = ROOT_ID_BLUETOOTH;
            File path = Environment.getExternalStoragePublicDirectory("Bluetooth");
            if(null == path){
            	path = Environment.getExternalStoragePublicDirectory("Download/Bluetooth");
            }
            if(null != path){

                final RootInfo root = new RootInfo();
                mRoots.put(rootId, root);

                root.rootId = rootId;
                root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                        | Root.FLAG_SUPPORTS_SEARCH;
                if (isEmpty(path)) {
                    root.flags |= Root.FLAG_EMPTY;
                }
                root.title = getContext().getString(R.string.root_bluetooth);
                root.path = path;
                root.docId = getDocIdForFile(path);
            }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

        try {
            final String rootId = ROOT_ID_RECIEVE_FLES;
            final File path = Environment.getExternalStoragePublicDirectory("Download/AnExplorer");

            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);

            root.rootId = rootId;
            root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH;
            if (isEmpty(path)) {
                root.flags |= Root.FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_receive);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}

    private boolean isEmpty(File file) {
        return null != file  && (!file.isDirectory()
                || null == file.list()
                || (null != file.list() && file.list().length == 0));
    }

    private void includeBookmarkRoot() {
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(ExplorerProvider.buildBookmark(), null, null, null, null);
            while (cursor.moveToNext()) {
                try {
                    final String rootId = ROOT_ID_BOOKMARK + " " +getCursorString(cursor, ExplorerProvider.BookmarkColumns.ROOT_ID);
                    final File path = new File(getCursorString(cursor, ExplorerProvider.BookmarkColumns.PATH));

                    final RootInfo root = new RootInfo();
                    mRoots.put(rootId, root);

                    root.rootId = rootId;
                    root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                            | Root.FLAG_SUPPORTS_SEARCH;
                    root.title = getCursorString(cursor, ExplorerProvider.BookmarkColumns.TITLE);
                    root.path = path;
                    root.docId = getDocIdForFile(path);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load some roots from " + ExplorerProvider.AUTHORITY + ": " + e);
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(cursor);
        }
    }

    public void updateSettings(){
        showFilesHidden = SettingsActivity.getDisplayFileHidden(getContext());
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

    public static boolean isDownloadAuthority(Intent intent){
    	if(null != intent.getData()){
        	String authority = intent.getData().getAuthority();
        	return ExternalStorageProvider.DOWNLOAD_AUTHORITY.equals(authority);
    	}
    	return false;
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
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
            } else {
                flags |= Document.FLAG_SUPPORTS_WRITE;
            }
            flags |= Document.FLAG_SUPPORTS_DELETE;
            flags |= Document.FLAG_SUPPORTS_RENAME;
            flags |= Document.FLAG_SUPPORTS_MOVE;
            flags |= Document.FLAG_SUPPORTS_COPY;
            flags |= Document.FLAG_SUPPORTS_ARCHIVE;
            flags |= Document.FLAG_SUPPORTS_BOOKMARK;
            flags |= Document.FLAG_SUPPORTS_EDIT;

            if(isTelevision()) {
                flags |= Document.FLAG_DIR_PREFERS_GRID;
            }
        }

        final String mimeType = getTypeForFile(file);
        if (DocumentArchiveHelper.isSupportedArchiveType(mimeType)) {
            flags |= Document.FLAG_ARCHIVE;
        }

        final String displayName = file.getName();
        if (!showFilesHidden && !TextUtils.isEmpty(displayName)) {
            if(displayName.charAt(0) == '.'){
                return;
            }
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
                if(ROOT_ID_PRIMARY_EMULATED.equals(root.rootId)
                        || root.rootId.startsWith(ROOT_ID_SECONDARY)
                        || root.rootId.startsWith(ROOT_ID_DEVICE)) {
                    final File file = root.rootId.startsWith(ROOT_ID_DEVICE)
                            ? Environment.getRootDirectory() : root.path;
                    row.add(Root.COLUMN_AVAILABLE_BYTES, file.getFreeSpace());
                    row.add(Root.COLUMN_CAPACITY_BYTES, file.getTotalSpace());
                }
            }
        }
        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocId, String docId) {
        try {
            if (mArchiveHelper.isArchivedDocument(docId)) {
                return mArchiveHelper.isChildDocument(parentDocId, docId);
            }
            // Archives do not contain regular files.
            if (mArchiveHelper.isArchivedDocument(parentDocId)) {
                return false;
            }

            final File parent = getFileForDocId(parentDocId).getCanonicalFile();
            final File doc = getFileForDocId(docId).getCanonicalFile();
            return FileUtils.contains(parent, doc);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to determine if " + docId + " is child of " + parentDocId + ": " + e);
        }
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName)
            throws FileNotFoundException {
        displayName = FileUtils.buildValidFatFilename(displayName);

        final File parent = getFileForDocId(docId);
        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent document isn't a directory");
        }

        final File file = FileUtils.buildUniqueFile(parent, mimeType, displayName);
        DocumentFile documentFile = getDocumentFile(docId, parent);
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            DocumentFile newFile = documentFile.createDirectory(displayName);
            if (!newFile.exists()) {
                throw new IllegalStateException("Failed to mkdir " + file);
            }
        } else {
            DocumentFile newFile = documentFile.createFile(mimeType, displayName);
            if (!newFile.exists()) {
                throw new IllegalStateException("Failed to touch " + file);
            }
        }
        final String afterDocId = getDocIdForFile(file);
        notifyDocumentsChanged(afterDocId);
        return afterDocId;
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        final File file = getFileForDocId(docId);
        DocumentFile documentFile = getDocumentFile(docId, file);

        if (!documentFile.delete()) {
            throw new IllegalStateException("Failed to delete " + file);
        }

        FileUtils.removeMediaStore(getContext(), file);
        notifyDocumentsChanged(docId);
    }

    @Override
    public String renameDocument(String docId, String displayName) throws FileNotFoundException {
        // Since this provider treats renames as generating a completely new
        // docId, we're okay with letting the MIME type change.
        displayName = FileUtils.buildValidFatFilename(displayName);

        final File before = getFileForDocId(docId);
        DocumentFile documentFile = getDocumentFile(docId, before);
        final File after = new File(before.getParentFile(), displayName);
        if (after.exists()) {
            throw new IllegalStateException("Already exists " + after);
        }
        if (!documentFile.renameTo(displayName)) {
            throw new IllegalStateException("Failed to rename to " + after);
        }
        final String afterDocId = getDocIdForFile(after);
        if (!TextUtils.equals(docId, afterDocId)) {
            notifyDocumentsChanged(afterDocId);
            return afterDocId;
        } else {
            return null;
        }
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        final String afterDocId = copy(sourceDocumentId, targetParentDocumentId);
        notifyDocumentsChanged(afterDocId);
        return afterDocId;
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId,
                               String targetParentDocumentId)
            throws FileNotFoundException {
        final String afterDocId = move(sourceDocumentId, targetParentDocumentId);
        notifyDocumentsChanged(afterDocId);
        return afterDocId;
    }

    @Override
    public String compressDocument(String parentDocumentId, ArrayList<String> documentIds) throws FileNotFoundException {
        final File fileFrom = getFileForDocId(parentDocumentId);
        ArrayList<File> files = new ArrayList<>();
        for (String documentId : documentIds){
            files.add(getFileForDocId(documentId));
        }
        if (!FileUtils.compressFile(fileFrom, files)) {
            throw new IllegalStateException("Failed to extract " + fileFrom);
        }
        notifyDocumentsChanged(parentDocumentId);
        return getDocIdForFile(fileFrom);
    }

    @Override
    public String uncompressDocument(String documentId) throws FileNotFoundException {
        final File fileFrom = getFileForDocId(documentId);
        if (!FileUtils.uncompress(fileFrom)) {
            throw new IllegalStateException("Failed to extract " + fileFrom);
        }
        notifyDocumentsChanged(documentId);
        return getDocIdForFile(fileFrom);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.queryDocument(documentId, projection);
        }

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        updateSettings();
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(
            String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(parentDocumentId) ||
                DocumentArchiveHelper.isSupportedArchiveType(getDocumentType(parentDocumentId))) {
            return mArchiveHelper.queryChildDocuments(parentDocumentId, projection, sortOrder);
        }

        final File parent = getFileForDocId(parentDocumentId);
        final MatrixCursor result = new DirectoryCursor(
                resolveDocumentProjection(projection), parentDocumentId, parent);
        updateSettings();
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
            parent = mRoots.get(rootId).path;
        }
        updateSettings();
        for (File file : FileUtils.searchDirectory(parent.getPath(), query)) {
        	includeFile(result, null, file);
		}
        return result;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.getDocumentType(documentId);
        }

        final File file = getFileForDocId(documentId);
        return getTypeForFile(file);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.openDocument(documentId, mode, signal);
        }

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
    public AssetFileDescriptor openDocumentThumbnail(
            String documentId, Point sizeHint, CancellationSignal signal)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.openDocumentThumbnail(documentId, sizeHint, signal);
        }

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
        } catch (Exception e){
            return DocumentsContract.openImageThumbnail(file);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
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

    private class DirectoryObserver extends FileObserver {
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
                switch ((event & NOTIFY_EVENTS)){
                    case MOVED_FROM:
                    case MOVED_TO:
                    case CREATE:
                    case DELETE:
                        mResolver.notifyChange(mNotifyUri, null, false);
                        FileUtils.updateMediaStore(getContext(), FileUtils.makeFilePath(mFile, path));
                        break;
                }
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

    private DocumentFile getDocumentFile(String docId, File file) throws FileNotFoundException {
        return DocumentsApplication.getSAFManager(getContext()).getDocumentFile(docId, file);
    }

    private void notifyDocumentsChanged(String docId){
        if(docId.startsWith(ROOT_ID_SECONDARY)){
            final String rootId = getParentRootIdForDocId(docId);
            Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
            getContext().getContentResolver().notifyChange(uri, null, false);
        }
    }

    private String copy(String sourceDocumentId,
                        String targetParentDocumentId) throws FileNotFoundException {

        final String afterDocId;
        final File source = getFile(sourceDocumentId);
        final File target = getFile(targetParentDocumentId);

        boolean isSourceOther = isFromOtherProvider(sourceDocumentId);
        boolean isTargetOther = isFromOtherProvider(targetParentDocumentId);

        if((isSourceOther || isTargetOther)){
            DocumentFile sourceDirectory = getDocumentFile(sourceDocumentId, source);
            DocumentFile targetDirectory = getDocumentFile(targetParentDocumentId, target);
            if (!FileUtils.moveDocument(getContext(), sourceDirectory, targetDirectory)) {
                throw new IllegalStateException("Failed to copy " + source);
            }
            afterDocId = targetParentDocumentId;
        } else {
            if (!FileUtils.moveDocument(source, target, null)) {
                throw new IllegalStateException("Failed to copy " + source);
            }
            afterDocId = getDocIdForFile(target);
        }

        return afterDocId;
    }

    private String move(String sourceDocumentId,
                        String targetParentDocumentId) throws FileNotFoundException {

        final String afterDocId;
        final File source = getFile(sourceDocumentId);
        final File target = getFile(targetParentDocumentId);

        boolean isSourceOther = isFromOtherProvider(sourceDocumentId);
        boolean isTargetOther = isFromOtherProvider(targetParentDocumentId);

        if((isSourceOther || isTargetOther)){
            DocumentFile sourceDirectory = getDocumentFile(sourceDocumentId, source);
            DocumentFile targetDirectory = getDocumentFile(targetParentDocumentId, target);
            if (!FileUtils.moveDocument(getContext(), sourceDirectory, targetDirectory)) {
                throw new IllegalStateException("Failed to move " + source);
            } else {
                if(!sourceDirectory.delete()){
                    throw new IllegalStateException("Failed to move " + source);
                }
            }
            afterDocId = targetParentDocumentId;
        } else {
            final File after = new File(target, source.getName());

            if (after.exists()) {
                throw new IllegalStateException("Already exists " + after);
            }
            if (!source.renameTo(after)) {
                throw new IllegalStateException("Failed to move to " + after);
            } else {
                notifyDocumentsChanged(targetParentDocumentId);
                FileUtils.updateMediaStore(getContext(), source.getPath());
            }
            afterDocId = getDocIdForFile(target);
        }

        return afterDocId;
    }

    private File getFile(String documentId) throws FileNotFoundException {
        if(documentId.startsWith(ROOT_ID_USB)){
            return null;
        }
        return getFileForDocId(documentId);
    }

    private boolean isFromOtherProvider(String documentId){
        return documentId.startsWith(ROOT_ID_SECONDARY) || documentId.startsWith(ROOT_ID_USB);
    }
}