/*
 * (C) Copyright 2016 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package dev.dworks.apps.anexplorer.provider;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.webkit.MimeTypeMap;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import com.github.mjdev.libaums.partition.Partition;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import androidx.collection.ArrayMap;
import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.libcore.util.Objects;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.MimePredicate;
import dev.dworks.apps.anexplorer.misc.ParcelFileDescriptorUtil;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.usb.UsbUtils;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.misc.FileUtils.getTypeForName;
import static dev.dworks.apps.anexplorer.misc.MimeTypes.BASIC_MIME_TYPE;

public class UsbStorageProvider extends DocumentsProvider {

    private static final String TAG = UsbStorageProvider.class.getSimpleName();

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".usbstorage.documents";
    // docId format: root:/path/to/file

    /**
     * Action string to request the permission to communicate with an UsbDevice.
     */
    private static final String ACTION_USB_PERMISSION = "dev.dworks.apps.anexplorer.action.USB_PERMISSION";

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_CAPACITY_BYTES, Root.COLUMN_PATH,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_SUMMARY,
    };

    private UsbManager usbManager;
    private boolean showFilesHidden;

    public static final String ROOT_ID_USB = "usb";

    private class UsbPartition {
        UsbDevice device;
        FileSystem fileSystem;
        boolean permissionGranted;
    }

    private final ArrayMap<String, UsbPartition> mRoots = new ArrayMap<>();

    private final LruCache<String, UsbFile> mFileCache = new LruCache<>(100);

    @Override
    public boolean onCreate() {
        Context context = getContext();

        updateSettings();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);

        updateRoots();
        return true;
    }

    @Override
    public void updateRoots() {
        mRoots.clear();
        if(Utils.checkUSBDevices()) {
            discoverDevices();
        }
        notifyRootsChanged();
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private void notifyRootsChanged() {
        getContext().getContentResolver().notifyChange(
                DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }

    public static void notifyRootsChanged(Context context) {
        context.getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }

    public static void notifyDocumentsChanged(Context context, String rootId) {
        Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        context.getContentResolver().notifyChange(uri, null, false);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        for (ArrayMap.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            UsbPartition usbPartition = root.getValue();
            UsbDevice usbDevice = usbPartition.device;
            FileSystem fileSystem = usbPartition.fileSystem;
            UsbFile rootDirectory = null;
            String volumeLabel = null;
            Long availableBytes = 0L;
            Long capactityBytes = 0L;
            String documentId = root.getKey() + ROOT_SEPERATOR;

            if(null != fileSystem){
                rootDirectory = fileSystem.getRootDirectory();
                volumeLabel = fileSystem.getVolumeLabel();
                availableBytes = fileSystem.getFreeSpace();
                capactityBytes = fileSystem.getCapacity();
                documentId = getDocIdForFile(rootDirectory);
            }

            String title = UsbUtils.getName(usbDevice);
            if(TextUtils.isEmpty(title)) {
                title = getContext().getString(R.string.root_usb);
            }

            int flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_EDIT | Root.FLAG_LOCAL_ONLY
                    | Root.FLAG_ADVANCED | Root.FLAG_SUPPORTS_IS_CHILD;

            final MatrixCursor.RowBuilder row = result.newRow();
            // These columns are required
            row.add(Root.COLUMN_ROOT_ID, root.getKey());
            row.add(Root.COLUMN_DOCUMENT_ID, documentId);
            row.add(Root.COLUMN_TITLE, title);
            row.add(Root.COLUMN_FLAGS, flags);
            // These columns are optional
            row.add(Root.COLUMN_SUMMARY, volumeLabel);
            row.add(Root.COLUMN_AVAILABLE_BYTES, availableBytes);
            row.add(Root.COLUMN_CAPACITY_BYTES, capactityBytes);
            row.add(Root.COLUMN_PATH, UsbUtils.getPath(usbDevice));
            // Root.COLUMN_MIME_TYPE is another optional column and useful if you have multiple roots with different
            // types of mime types (roots that don't match the requested mime type are automatically hidden)
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        try {
            updateSettings();
            final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
            if(requestPermission()){
                includeFile(result, getFileForDocId(documentId));
            } else {
                includeDefaultDocument(result, documentId);
            }

            return result;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        try {
            updateSettings();
            final MatrixCursor result = new DocumentCursor(resolveDocumentProjection(projection), parentDocumentId);
            UsbFile parent;
            try {
                parent = getFileForDocId(parentDocumentId);
            } catch (Exception e){
                return result;
            }
            for (UsbFile child : parent.listFiles()) {
                includeFile(result, child);
            }
            return result;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        try {
            UsbFile file = getFileForDocId(documentId);

/*            final int accessMode = ParcelFileDescriptorUtil.parseMode(mode);
            if ((accessMode | ParcelFileDescriptor.MODE_READ_ONLY) == ParcelFileDescriptor.MODE_READ_ONLY) {
                return ParcelFileDescriptorUtil.pipeFrom(new UsbFileInputStream(file));
            } else if ((accessMode | ParcelFileDescriptor.MODE_WRITE_ONLY) == ParcelFileDescriptor.MODE_WRITE_ONLY) {
                return ParcelFileDescriptorUtil.pipeTo(new UsbFileOutputStream(file));
            }*/
            final boolean isWrite = (mode.indexOf('w') != -1);
            if (isWrite) {
                return ParcelFileDescriptorUtil.pipeTo(new UsbFileOutputStream(file));
            } else {
                return ParcelFileDescriptorUtil.pipeFrom(new UsbFileInputStream(file));
            }

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        return documentId.startsWith(parentDocumentId);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        try {
            UsbFile parent = getFileForDocId(parentDocumentId);

            UsbFile child;
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                child = parent.createDirectory(displayName);
            } else {
                child = parent.createFile(getFileName(mimeType, displayName));
            }

            final String afterDocId = getDocIdForFile(child);
            notifyDocumentsChanged(afterDocId);
            return afterDocId;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        try {
            UsbFile file = getFileForDocId(documentId);
            file.setName(getFileName(getMimeType(file), displayName));
            mFileCache.remove(documentId);

            final String afterDocId = getDocIdForFile(file);
            notifyDocumentsChanged(afterDocId);
            return afterDocId;

        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        try {
            UsbFile file = getFileForDocId(documentId);
            file.delete();
            mFileCache.remove(documentId);
            notifyDocumentsChanged(documentId);
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        UsbPartition usbPartition = mRoots.get(rootId);
        final UsbFile parent = usbPartition.fileSystem.getRootDirectory();
        updateSettings();
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        //TODO implement actual search
        return result;
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        try {
            final String afterDocId = move(sourceDocumentId, targetParentDocumentId);
            notifyDocumentsChanged(afterDocId);
            return afterDocId;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        try {
            final String afterDocId = copy(sourceDocumentId, targetParentDocumentId);
            notifyDocumentsChanged(afterDocId);
            return afterDocId;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getDocumentType(String documentId) {
        try {
            return getMimeType(getFileForDocId(documentId));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            CrashReportingManager.logException(e);
        }

        return BASIC_MIME_TYPE;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint,
                                         CancellationSignal signal) throws FileNotFoundException {
        try {
            UsbFile file = getFileForDocId(documentId);
            final ParcelFileDescriptor pfd = ParcelFileDescriptorUtil.pipeFrom(new UsbFileInputStream(file));
            return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    private static String getMimeType(UsbFile file) {

        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    private static String getFileName(String mimeType, String displayName) {

        String extension = MimeTypeMap.getFileExtensionFromUrl(displayName).toLowerCase();
        if ((extension == null) ||
                !Objects.equals(mimeType, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) {
                displayName = displayName + "." + extension;
            }
        }
        return displayName;
    }

    private void includeFile(final MatrixCursor result, final UsbFile file) throws FileNotFoundException {

        final String displayName = file.isRoot() ? "" : file.getName();
        if (!showFilesHidden && !TextUtils.isEmpty(displayName)) {
            if(displayName.charAt(0) == '.'){
                return;
            }
        }

        int flags = 0;
        if (file.isDirectory()) {
            flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
        } else {
            flags |= Document.FLAG_SUPPORTS_WRITE;
        }
        flags |= Document.FLAG_SUPPORTS_DELETE;
        flags |= Document.FLAG_SUPPORTS_RENAME;
        flags |= Document.FLAG_SUPPORTS_MOVE;
        flags |= Document.FLAG_SUPPORTS_COPY;
        flags |= Document.FLAG_SUPPORTS_EDIT;

        if(isTelevision()) {
            flags |= Document.FLAG_DIR_PREFERS_GRID;
        }


        final String mimeType = getMimeType(file);
        if(MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType)){
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final MatrixCursor.RowBuilder row = result.newRow();

        row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForFile(file));
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_FLAGS, flags);
        row.add(Document.COLUMN_SIZE, file.isDirectory() ? 0 : file.getLength());

        try {
            if(file.isDirectory() && null != file.list()){
                row.add(Document.COLUMN_SUMMARY, FileUtils.formatFileCount(file.list().length));
            }
        } catch (IOException e) {
            CrashReportingManager.logException(e);
        }

        // Only publish dates reasonably after epoch
        long lastModified = file.isRoot() ? 0 : file.lastModified();
        if (lastModified > 31536000000L) {
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        }
        //row.add(Document.COLUMN_PATH, getUsbPath(file));
    }

    public void discoverDevices(){
        try{
            for (UsbDevice device : usbManager.getDeviceList().values()) {
                if(hasPermission(device)) {
                    discoverDevice(device);
                } else {
                    addRoot(device);
                }
            }
        } catch (Exception e){
            CrashReportingManager.logException(e);
        }
    }

    private void discoverDevice(UsbDevice device) {
        for (UsbMassStorageDevice massStorageDevice : UsbMassStorageDevice.getMassStorageDevices(getContext())) {
            if (device.equals(massStorageDevice.getUsbDevice())) {
                if (hasPermission(device)) {
                    addRoot(massStorageDevice);
                } else {
                    requestPermission(device);
                }
            }
        }
    }

    private void detachDevice(UsbDevice usbDevice) {
        for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            if (root.getValue().device.equals(usbDevice)) {
                Log.d(TAG, "remove rootId " + root.getKey());
                mRoots.remove(root.getKey());
                mFileCache.evictAll();
                notifyRootsChanged();
                break;
            }
        }
    }

    private void addRoot(UsbMassStorageDevice device) {
        try {
            device.init();
            for (Partition partition : device.getPartitions()) {
                UsbPartition usbPartition = new UsbPartition();
                usbPartition.device = device.getUsbDevice();
                usbPartition.fileSystem = partition.getFileSystem();
                usbPartition.permissionGranted = true;
                mRoots.put(getRootId(device.getUsbDevice()), usbPartition);
            }
        } catch (Exception e) {
            Log.e(TAG, "error setting up device", e);
        }
    }

    private void addRoot(UsbDevice device) {
        try {
            UsbPartition usbPartition = new UsbPartition();
            usbPartition.device = device;
            usbPartition.permissionGranted = false;
            mRoots.put(getRootId(device), usbPartition);
        } catch (Exception e) {
            Log.e(TAG, "error setting up device", e);
        }
    }

    private String getDocIdForFile(UsbFile file) throws FileNotFoundException {

        if (file.isRoot()) {
            for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
                FileSystem fileSystem = root.getValue().fileSystem;
                if(null != fileSystem) {
                    if (file.equals(fileSystem.getRootDirectory())) {
                        String documentId = root.getKey() + ROOT_SEPERATOR;
                        mFileCache.put(documentId, file);
                        return documentId;
                    }
                } else {
                    String documentId = root.getKey() + ROOT_SEPERATOR;
                    mFileCache.put(documentId, file);
                    return documentId;
                }
            }
            throw new FileNotFoundException("Missing root entry");
        }

        String documentId = getDocIdForFile(file.getParent()) + DIRECTORY_SEPERATOR + file.getName();
        mFileCache.put(documentId, file);
        return documentId;
    }

    public UsbFile getFileForDocId(String documentId) throws IOException {
        UsbFile file = mFileCache.get(documentId);
        if (null != file)
            return file;

        final int splitIndex = documentId.lastIndexOf(DIRECTORY_SEPERATOR);
        if (splitIndex < 0) {
            String rootId = documentId.substring(0, documentId.length() - 1);
            UsbPartition usbPartition = mRoots.get(rootId);
            if (null == usbPartition) {
                throw new FileNotFoundException("Missing root for " + rootId);
            }

            file = usbPartition.fileSystem.getRootDirectory();
            mFileCache.put(documentId, file);
            return file;
        }

        UsbFile parent = getFileForDocId(documentId.substring(0, splitIndex));
        if (null == parent)
            throw new FileNotFoundException("Missing parent for " + documentId);

        String name = documentId.substring(splitIndex + 1);

        for (UsbFile child : parent.listFiles()) {
            if (name.equals(child.getName())) {
                mFileCache.put(documentId, child);
                return child;
            }
        }

        throw new FileNotFoundException("File not found " + documentId);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            String deviceName = usbDevice.getDeviceName();
            if (UsbStorageProvider.ACTION_USB_PERMISSION.equals(action)) {
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (permission) {
                    discoverDevice(usbDevice);
                    notifyRootsChanged();
                    notifyDocumentsChanged(getContext(), getRootId(usbDevice)+ROOT_SEPERATOR);
                } else {
                    // so we don't ask for permission again
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)
                    || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                updateRoots();
            }
        }
    };

    public boolean hasPermission(UsbDevice device){
        return usbManager.hasPermission(device);
    }

    public void requestPermission(UsbDevice device){
        PendingIntent permissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, permissionIntent);
    }

    public void updateSettings(){
        showFilesHidden = SettingsActivity.getDisplayFileHidden(getContext());
    }

    private boolean requestPermission() {
        for (Map.Entry<String, UsbPartition> root : mRoots.entrySet()) {
            UsbPartition usbPartition = root.getValue();
            if (!usbPartition.permissionGranted) {
                discoverDevice(usbPartition.device);
                return false;
            }
        }
        return true;
    }

    private void includeDefaultDocument(MatrixCursor result, String documentId) {

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, "");
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        int flags = Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_DIR_PREFERS_LAST_MODIFIED
                | Document.FLAG_DIR_HIDE_GRID_TITLES | Document.FLAG_SUPPORTS_DELETE;
        if(!isWatch()) {
            flags |= Document.FLAG_DIR_PREFERS_GRID;
        }
        row.add(Document.COLUMN_FLAGS, flags);
    }

    private String getRootId(UsbDevice usbDevice) {
        return ROOT_ID_USB + Integer.toString(usbDevice.getDeviceId());
    }

    private class DocumentCursor extends MatrixCursor {
        public DocumentCursor(String[] columnNames, String docId) {
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

    private DocumentFile getDocumentFile(String docId) throws FileNotFoundException {
        return DocumentsApplication.getSAFManager(getContext()).getDocumentFile(docId, null);
    }

    private String copy(String sourceDocumentId,
                        String targetParentDocumentId) throws IOException {

        final String afterDocId;
        final UsbFile before = getFile(sourceDocumentId);
        final UsbFile after = getFile(targetParentDocumentId);

        boolean isSourceUSB = sourceDocumentId.startsWith(ROOT_ID_USB);
        boolean isTargetUSB = targetParentDocumentId.startsWith(ROOT_ID_USB);

        if(!(isSourceUSB && isTargetUSB)){
            DocumentFile sourceDirectory = getDocumentFile(sourceDocumentId);
            DocumentFile targetDirectory = getDocumentFile(targetParentDocumentId);
            if (!FileUtils.moveDocument(getContext(), sourceDirectory, targetDirectory)) {
                throw new IllegalStateException("Failed to copy ");
            }
            afterDocId = targetParentDocumentId;
        } else {

            UsbPartition usbPartition = mRoots.get(getRootIdForDocId(sourceDocumentId));
            final FileSystem fileSystem = usbPartition.fileSystem;
            final UsbFile newFile = after.createFile(before.getName());
            InputStream inputStream = UsbFileStreamFactory.createBufferedInputStream(before, fileSystem);
            OutputStream outputStream = UsbFileStreamFactory.createBufferedOutputStream(newFile, fileSystem);

            if (!FileUtils.copy(inputStream, outputStream)) {
                throw new IllegalStateException("Failed to copy " + before);
            }
            afterDocId = getDocIdForFile(after);
        }

        return afterDocId;
    }

    private String move(String sourceDocumentId,
                        String targetParentDocumentId) throws IOException {
        final String afterDocId;
        final UsbFile before = getFile(sourceDocumentId);
        final UsbFile after = getFile(targetParentDocumentId);

        boolean isSourceUSB = sourceDocumentId.startsWith(ROOT_ID_USB);
        boolean isTargetUSB = targetParentDocumentId.startsWith(ROOT_ID_USB);

        if(!(isSourceUSB && isTargetUSB)){
            DocumentFile sourceDirectory = getDocumentFile(sourceDocumentId);
            DocumentFile targetDirectory = getDocumentFile(targetParentDocumentId);
            if (!FileUtils.moveDocument(getContext(), sourceDirectory, targetDirectory)) {
                throw new IllegalStateException("Failed to move ");
            } else {
                if(!sourceDirectory.delete()){
                    throw new IllegalStateException("Failed to move ");
                }
            }
            afterDocId = targetParentDocumentId;
        } else {
            before.moveTo(after);
            afterDocId = getDocIdForFile(after);
        }

        return afterDocId;
    }

    private UsbFile getFile(String documentId) throws IOException {
        if(!documentId.startsWith(ROOT_ID_USB)){
            return null;
        }
        return getFileForDocId(documentId);
    }
}