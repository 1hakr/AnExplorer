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
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.SparseArray;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.misc.CancellationSignal;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.StorageUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;

/**
 * Presents a {@link DocumentsContract} view of {@link Apps}
 * contents.
 */
public class AppsProvider extends DocumentsProvider {
    public static final String AUTHORITY = "dev.dworks.apps.anexplorer.apps.documents";
    public static final String ROOT_ID_APP = "apps";
    public static final String ROOT_ID_PROCESS = "process";
    
    // docId format: apps: com.package
    // docId format: process: com.package
    
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
            Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_SUMMARY, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE,
    };

	private PackageManager packageManager;
	private ActivityManager activityManager;
	private static SparseArray<String> processTypeCache;

	static {
		processTypeCache = new SparseArray<String>();
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_SERVICE, "Service");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_BACKGROUND, "Background");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_FOREGROUND, "Foreground");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_VISIBLE, "Visible");
		processTypeCache.put(RunningAppProcessInfo.IMPORTANCE_EMPTY, "Empty");
	}
    @Override
    public boolean onCreate() {
		packageManager = getContext().getPackageManager();
		activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        return true;
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
    
    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
    	StorageUtils storageUtils = new StorageUtils(getContext());
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        final RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, ROOT_ID_APP);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_SEARCH);
        row.add(Root.COLUMN_ICON, R.drawable.ic_root_apps);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_apps));
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_APP);
        row.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_DATA, false));
        
        final RowBuilder row1 = result.newRow();
        row1.add(Root.COLUMN_ROOT_ID, ROOT_ID_PROCESS);
        row1.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_SEARCH);
        row1.add(Root.COLUMN_ICON, R.drawable.ic_root_process);
        row1.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_processes));
        row1.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_PROCESS);
        row1.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_RAM, false));
        return result;
    }
    
    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

    	if (ROOT_ID_APP.equals(rootId)) {
    		List<PackageInfo> allAppList = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
    		for (PackageInfo packageInfo : allAppList) {
    			includeAppFromPackage(result, rootId, packageInfo, query.toLowerCase());
    		}	
    	}
    	else{
			List<RunningAppProcessInfo> runningProcessesList = activityManager.getRunningAppProcesses();
			for (RunningAppProcessInfo processInfo : runningProcessesList) {
				includeAppFromProcess(result, rootId, processInfo, query.toLowerCase());
			}
    	}
        final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        result.setNotificationUri(getContext().getContentResolver(), notifyUri);
    	return result;
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        // Delegate to real provider
    	final String rootId = getRootIdForDocId(docId);
    	final String packageName = getPackageForDocId(docId);
        final long token = Binder.clearCallingIdentity();
        try {
        	if (ROOT_ID_APP.equals(rootId)) {
    			try {
    				Uri packageUri = Uri.fromParts("package", packageName,null);
    				if(packageUri != null){
    					Intent intentUninstall = new Intent(Intent.ACTION_DELETE, packageUri);
    					intentUninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    					getContext().startActivity(intentUninstall);
    				}	
    			} catch (Exception e) { }
        	}
        	else{
        		activityManager.killBackgroundProcesses(getPackageForDocId(docId));
        	}
        } finally {
            Binder.restoreCallingIdentity(token);
        }
/*        
        if (!ROOT_ID_APP.equals(rootId)) {
        	//FIXME: Do this only once from client
        	notifyDocumentsChanged(getContext(), getRootIdForDocId(docId));
            notifyRootsChanged(getContext());	
        }*/
    }
    
    @Override
    public void moveDocument(String documentIdFrom, String documentIdTo, boolean deleteAfter) throws FileNotFoundException {
    	final String packageName = getPackageForDocId(documentIdFrom);
    	String fromFilePath = "";
    	String fileName = "";
    	try {
        	PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
        	ApplicationInfo appInfo = packageInfo.applicationInfo;
        	fromFilePath = appInfo.sourceDir;
        	fileName = (String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : appInfo.packageName);
		} catch (Exception e) {
		}

    	final File fileFrom = new File(fromFilePath);
    	final File fileTo = Environment.getExternalStorageDirectory();
        if (!FileUtils.moveFile(fileFrom, fileTo, fileName)) {
            throw new IllegalStateException("Failed to copy " + fileFrom);
        }
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeDefaultDocument(result, docId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String docId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
        	if (ROOT_ID_APP.equals(docId)) {
        		List<PackageInfo> allAppList = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        		for (PackageInfo packageInfo : allAppList) {
        			includeAppFromPackage(result, docId, packageInfo, null);
        		}	
        	}
        	else{
    			List<RunningAppProcessInfo> runningProcessesList = activityManager.getRunningAppProcesses();
    			for (RunningAppProcessInfo processInfo : runningProcessesList) {
    				includeAppFromProcess(result, docId, processInfo, null);
    			}
        	}
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
        result.setNotificationUri(getContext().getContentResolver(), notifyUri);
        notifyRootsChanged(getContext());
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
            return null;//resolver.openFileDescriptor(mDm.getUriForDownloadedFile(id), mode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(
            String docId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        // TODO: extend ExifInterface to support fds
        final ParcelFileDescriptor pfd = openDocument(docId, "r", signal);
        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    private void includeDefaultDocument(MatrixCursor result, String docId) {
        final RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL);
    }

	private void includeAppFromProcess(MatrixCursor result, String docId, RunningAppProcessInfo processInfo, String query ) {

		if (processInfo.importance != RunningAppProcessInfo.IMPORTANCE_EMPTY
				&& processInfo.importance != RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
			String process = (String) (processInfo.processName);
			process = process.substring(process.lastIndexOf(".") + 1, process.length());
			String summary = "";
			String displayName = "";
			ApplicationInfo appInfo = null;
			try {
				appInfo = packageManager.getPackageInfo(processInfo.processName, PackageManager.GET_ACTIVITIES).applicationInfo;
				displayName = (String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : appInfo.packageName);
			} catch (Exception e) { }
			
			if (TextUtils.isEmpty(displayName)) {
				return;
			} else {
				displayName = process;
			}
			if (null != query && !displayName.toLowerCase().contains(query)) {
				return;
			}
			final String path = appInfo.sourceDir;
			final String mimeType = Document.MIME_TYPE_APK;
			
	        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;

			summary = processTypeCache.get(processInfo.importance);
			final long size = getProcessSize(processInfo.pid);
			final String packageName = processInfo.processName;
			
	        final RowBuilder row = result.newRow();
	        row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
	        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
	        row.add(Document.COLUMN_SUMMARY, summary);
	        row.add(Document.COLUMN_SIZE, size);
	        row.add(Document.COLUMN_MIME_TYPE, mimeType);
	        //row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
	        row.add(Document.COLUMN_PATH, path);
	        row.add(Document.COLUMN_FLAGS, flags);
		}
    }

	private void includeAppFromPackage(MatrixCursor result, String docId, PackageInfo packageInfo, String query ) {

		ApplicationInfo appInfo = packageInfo.applicationInfo;
		if(isAppUseful(appInfo)){
			String summary = "";
			String displayName = "";
			try {
				displayName = (String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : appInfo.packageName);
				summary = packageInfo.versionName == null ? "" : packageInfo.versionName;
			} catch (Exception e) { }

			if (null != query && !displayName.toLowerCase().contains(query)) {
				return;
			}
			final String path = appInfo.sourceDir;
			final String mimeType = Document.MIME_TYPE_APK;

			int flags = Document.FLAG_SUPPORTS_EDIT | Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
			
	        final long size = new File(appInfo.sourceDir).length();
			final String packageName = packageInfo.packageName;
	        final long lastModified = packageInfo.lastUpdateTime;
	
	        final RowBuilder row = result.newRow();
	        row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
	        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
	        row.add(Document.COLUMN_SUMMARY, summary);
	        row.add(Document.COLUMN_SIZE, size);
	        row.add(Document.COLUMN_MIME_TYPE, mimeType);
	        row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
	        row.add(Document.COLUMN_PATH, path);
	        row.add(Document.COLUMN_FLAGS, flags);
		}
    }
	private boolean isAppUseful(ApplicationInfo appInfo) {
		 if (appInfo.flags != 0 
				 && ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
				 || (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)) {
             return true;
		 }
		return false;
	}

	public static String getDocIdForApp(String rootId, String packageName){
    	return rootId + ":" + packageName;
    }
    
    public static String getPackageForDocId(String docId){
        final int splitIndex = docId.indexOf(':', 1);
        final String packageName = docId.substring(splitIndex + 1);
        return packageName;
    }
    
    public static String getRootIdForDocId(String docId){
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);
        return tag;
    }

	private long getProcessSize(int pid) {
		android.os.Debug.MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(new int[] { pid });
		return memInfos[0].getTotalPss() * 1024;
	}
}