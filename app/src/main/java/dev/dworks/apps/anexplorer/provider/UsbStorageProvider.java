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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.misc.CancellationSignal;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.MimeTypes;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.model.GuardedBy;
import dev.dworks.apps.anexplorer.usb.UsbMassStorageDevice;
import dev.dworks.apps.anexplorer.usb.UsbRootFile;
import dev.dworks.apps.anexplorer.usb.fs.FileSystem;
import dev.dworks.apps.anexplorer.usb.fs.UsbFile;

@SuppressLint("DefaultLocale")
public class UsbStorageProvider extends StorageProvider {
    private static final String TAG = "USBStorage";

    private static final boolean LOG_INOTIFY = false;

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".usbstorage.documents";
    // docId format: root:path/to/file

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_TOTAL_BYTES, Root.COLUMN_PATH,
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
        public String path;
    }

    public static final String ROOT_ID_USB = "usb";

    private Handler mHandler;

    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayList<RootInfo> mRoots;
    @GuardedBy("mRootsLock")
    private HashMap<String, RootInfo> mIdToRoot;
    @GuardedBy("mRootsLock")
    private HashMap<String, UsbRootFile> mIdToPath;

    @GuardedBy("mObservers")
    private Map<File, DirectoryObserver> mObservers = Maps.newHashMap();

    private static final String ACTION_USB_PERMISSION =
            "dev.dworks.apps.anexplorer.action.USB_PERMISSION";
    private UsbDevice mDevice;
    private UsbManager mUsbManager;
    private UsbMassStorageDevice device;

    @Override
    public boolean onCreate() {
        mHandler = new Handler();
        mUsbManager = (UsbManager)getContext().getSystemService(Context.USB_SERVICE);

        mRoots = Lists.newArrayList();
        mIdToRoot = Maps.newLinkedHashMap();
        mIdToPath = Maps.newLinkedHashMap();

        updateVolumes();
        return true;
    }

    public void updateVolumes() {
        synchronized (mRootsLock) {
            updateVolumesLocked();
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            String deviceName = usbDevice.getDeviceName();
            if (ACTION_USB_PERMISSION.equals(action)) {
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                if (permission) {
                    setupDevice();
                } else {
                    // so we don't ask for permission again
                }
            }
        }
    };

    /**
     * Searches for connected mass storage devices, and initializes them if it
     * could find some.
     */
    private void discoverDevice(Intent intent) {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(getContext());

        if (devices.length == 0) {
            Log.w(TAG, "no device found!");
            return;
        }

        // we only use the first device
        device = devices[0];

        String action = intent.getAction();
        Log.d(TAG, "Device attached");
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        getContext().registerReceiver(mUsbReceiver, filter);
        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (!mUsbManager.hasPermission(usbDevice)) {
            mUsbManager.requestPermission(usbDevice, mPermissionIntent);
        } else {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                setupDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (mDevice != null && mDevice.equals(usbDevice)) {
                    setupDevice();
                }
            }
        }
    }

    /**
     * Sets the device up and shows the contents of the root directory.
     */
    private void setupDevice() {
        try {
            device.init();

            // we always use the first partition of the device
            FileSystem fs = device.getPartitions().get(0).getFileSystem();
            UsbFile root = fs.getRootDirectory();
            fs.getVolumeLabel();

            //listView.setAdapter(adapter = new UsbFileListAdapter(this, root));
        } catch (IOException e) {
            Log.e(TAG, "error setting up device", e);
        } catch (Exception e) {
            Log.e(TAG, "error setting up device", e);
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void updateVolumesLocked() {
        mRoots.clear();
        mIdToPath.clear();
        mIdToRoot.clear();

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if(deviceList.isEmpty()){
            return;
        }

        try {
            UsbDevice device =  deviceList.entrySet().iterator().next().getValue();
            final String rootId = ROOT_ID_USB + device.getDeviceId();
            final UsbRootFile path = new UsbRootFile(device);
            mIdToPath.put(rootId, path);
            final RootInfo root = new RootInfo();

            root.rootId = rootId;
            root.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED
                    | Root.FLAG_SUPPORTS_SEARCH | Root.FLAG_SUPPORTS_IS_CHILD;
            root.title = path.getName();
            root.docId = getDocIdForFile(path);
            root.path = path.getPath();
            mRoots.add(root);
            mIdToRoot.put(rootId, root);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Log.d(TAG, "After updating volumes, found " + mRoots.size() + " active roots");

        getContext().getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
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

    private String getDocIdForFile(UsbRootFile file) throws FileNotFoundException {
        String path = file.getAbsolutePath();

        // Find the most-specific root path
        Map.Entry<String, UsbRootFile> mostSpecific = null;
        synchronized (mRootsLock) {
            for (Map.Entry<String, UsbRootFile> root : mIdToPath.entrySet()) {
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

    private UsbRootFile getFileForDocId(String docId) throws FileNotFoundException {
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);
        final String path = docId.substring(splitIndex + 1);

        UsbRootFile target;
        synchronized (mRootsLock) {
            target = mIdToPath.get(tag);
        }
        if (target == null) {
            throw new FileNotFoundException("No root for " + tag);
        }
/*        if (!target.exists()) {
            target.mkdirs();
        }
        target = new File(target, path);
        if (!target.exists()) {
            throw new FileNotFoundException("Missing file for " + docId + " at " + target);
        }*/
        return target;
    }

    private void includeFile(MatrixCursor result, String docId, UsbRootFile file)
            throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }

        int flags = 0;

        // Damn SAF
        boolean isWriteDisabled = !TextUtils.isEmpty(docId);

/*        if (file.canWrite() && !isWriteDisabled) {
            if (file.isDirectory()) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                flags |= Document.FLAG_SUPPORTS_DELETE;
                flags |= Document.FLAG_SUPPORTS_RENAME;
            } else {
                flags |= Document.FLAG_SUPPORTS_WRITE;
                flags |= Document.FLAG_SUPPORTS_DELETE;
                flags |= Document.FLAG_SUPPORTS_RENAME;
            }
            flags |= Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_EDIT ;
        }*/

        final String displayName = file.getName();
/*        final String mimeType = getTypeForFile(file);
        if(MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType)){
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }*/
        
        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        //row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_PATH, file.getAbsolutePath());
        row.add(Document.COLUMN_FLAGS, flags);
/*        if(file.isDirectory() && null != file.list()){
        	row.add(Document.COLUMN_SUMMARY, FileUtils.formatFileCount(file.list().length));
        }

        // Only publish dates reasonably after epoch
        long lastModified = file.lastModified();
        if (lastModified > 31536000000L) {
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        }*/
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        synchronized (mRootsLock) {
            for (String rootId : mIdToPath.keySet()) {
                final RootInfo root = mIdToRoot.get(rootId);
                final UsbRootFile path = mIdToPath.get(rootId);

                final RowBuilder row = result.newRow();
                row.add(Root.COLUMN_ROOT_ID, root.rootId);
                row.add(Root.COLUMN_ICON, R.drawable.ic_root_usb);
                row.add(Root.COLUMN_FLAGS, root.flags);
                row.add(Root.COLUMN_TITLE, root.title);
                row.add(Root.COLUMN_DOCUMENT_ID, root.docId);
                row.add(Root.COLUMN_PATH, root.path);
/*                if(ROOT_ID_PRIMARY_EMULATED.equals(root.rootId)
                        || root.rootId.startsWith(ROOT_ID_SECONDARY) || root.rootId.startsWith(ROOT_ID_PHONE)){
                	row.add(Root.COLUMN_AVAILABLE_BYTES, path.getFreeSpace());
                    row.add(Root.COLUMN_TOTAL_BYTES, path.getTotalSpace());
                }*/
            }
        }
        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocId, String docId) {
/*        try {
            final File parent = getFileForDocId(parentDocId).getCanonicalFile();
            final File doc = getFileForDocId(docId).getCanonicalFile();
            return FileUtils.contains(parent, doc);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to determine if " + docId + " is child of " + parentDocId + ": " + e);
        }*/
        return false;
    }

    @Override
    public String createDocument(String docId, String mimeType, String displayName)
            throws FileNotFoundException {
        final UsbRootFile parent = getFileForDocId(docId);
/*        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("Parent document isn't a directory");
        }

        File file;
        if (Document.MIME_TYPE_DIR.equals(mimeType)) {
            file = new File(parent, displayName);
            if (!file.mkdir()) {
                throw new IllegalStateException("Failed to mkdir " + file);
            }
        } else {
            displayName = FileUtils.removeExtension(mimeType, displayName);
            file = new File(parent, FileUtils.addExtension(mimeType, displayName));

            // If conflicting file, try adding counter suffix
            int n = 0;
            while (file.exists() && n++ < 32) {
                file = new File(parent, FileUtils.addExtension(mimeType, displayName + " (" + n + ")"));
            }

            try {
                if (!file.createNewFile()) {
                    throw new IllegalStateException("Failed to touch " + file);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to touch " + file + ": " + e);
            }
        }*/
        return getDocIdForFile(parent);
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
/*        final File file = getFileForDocId(docId);
        if (!FileUtils.deleteFile(file)) {
            throw new IllegalStateException("Failed to delete " + file);
        }*/
    }
    
    @Override
    public void moveDocument(String documentIdFrom, String documentIdTo, boolean deleteAfter) throws FileNotFoundException {
/*    	final File fileFrom = getFileForDocId(documentIdFrom);
    	final File fileTo = getFileForDocId(documentIdTo);
        if (!FileUtils.moveFile(fileFrom, fileTo, null)) {
            throw new IllegalStateException("Failed to copy " + fileFrom);
        }
        else{
        	if (deleteAfter) {
                if (!FileUtils.deleteFile(fileFrom)) {
                    throw new IllegalStateException("Failed to delete " + fileFrom);
                }
                else{
                    FileUtils.updateMedia(getContext(), fileFrom.getPath());
                }
			}
        }*/
    }
    
    @Override
    public String renameDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
/*        final File parent = getFileForDocId(parentDocumentId);
        File file;
    	
		if(parent.isDirectory()){
			file = new File(parent.getParentFile(), FileUtils.removeExtension(mimeType, displayName));
		}
		else{
			displayName = FileUtils.removeExtension(mimeType, displayName);
            file = new File(parent.getParentFile(), FileUtils.addExtension(mimeType, displayName));
		}
		
		if(parent.canWrite()){
			parent.renameTo(file);
		}
		
		return getDocIdForFile(file);*/
        return  "";
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
    	String mimeType = getDocumentType(parentDocumentId);
        final UsbRootFile parent = getFileForDocId(parentDocumentId);
        final MatrixCursor result = new DirectoryCursor(
                resolveDocumentProjection(projection), parentDocumentId, parent);
/*        for (File file : parent.listFiles()) {
            includeFile(result, null, file);
        }*/
        return result;
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        final UsbRootFile file = getFileForDocId(documentId);
        return "";//getTypeForFile(file);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
/*        final File file = getFileForDocId(documentId);
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
                            FileUtils.updateMedia(getContext(), file.getPath());
                        }
                    });
                } catch (IOException e) {
                    throw new FileNotFoundException("Failed to open for writing: " + e);
                }
            }
        }
        else{
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);//ParcelFileDescriptor.parseMode(mode));
        }*/
        return null;
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
/*        final File file = getFileForDocId(docId);
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
        } finally {
            Binder.restoreCallingIdentity(token);
        }*/
        return null;
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
            final String mime = MimeTypes.getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }

        return "application/octet-stream";
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
                //notify roots changed
                //mResolver.notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
                switch ((event & NOTIFY_EVENTS)){
                    case MOVED_FROM:
                    case MOVED_TO:
                    case CREATE:
                    case DELETE:
                        mResolver.notifyChange(mNotifyUri, null, false);
                        FileUtils.updateMedia(getContext(), FileUtils.makeFilePath(mFile, path));
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
        private final UsbRootFile mFile;

        public DirectoryCursor(String[] columnNames, String docId, UsbRootFile file) {
            super(columnNames);

            final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
            setNotificationUri(getContext().getContentResolver(), notifyUri);

            mFile = file;
            //startObserving(mFile, notifyUri);
        }

        @Override
        public void close() {
            super.close();
            //stopObserving(mFile);
        }
    }
}