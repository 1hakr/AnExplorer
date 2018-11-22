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
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.SparseArray;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor;
import dev.dworks.apps.anexplorer.cursor.MatrixCursor.RowBuilder;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.misc.PackageManagerUtils;
import dev.dworks.apps.anexplorer.misc.StorageUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Document;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;

import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isTelevision;

/**
 * Presents a {@link DocumentsContract} view of Apps contents.
 */
@SuppressLint("DefaultLocale")
public class AppsProvider extends DocumentsProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".apps.documents";
	// docId format: user_apps:com.package
	// docId format: system_apps:com.package
	// docId format: process:com.package

    public static final String ROOT_ID_USER_APP = "user_apps:";
    public static final String ROOT_ID_SYSTEM_APP = "system_apps:";
    public static final String ROOT_ID_PROCESS = "process:";
    
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
            Root.COLUMN_TITLE, Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_CAPACITY_BYTES,
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
		List<PackageInfo> allAppList = packageManager.getInstalledPackages(getAppListFlag());
		List<RunningAppProcessInfo> processList = getRunningAppProcessInfo(getContext());
		int countUserApps = 0;
		int countSystemApps = 0;
		int countProcesses = null != processList? processList.size() : 0;
		for (PackageInfo packageInfo : allAppList) {
			ApplicationInfo appInfo = packageInfo.applicationInfo;
			if(isSystemApp(appInfo)){
				countSystemApps++;
			} else {
				countUserApps++;
			}
		}
		row.add(Root.COLUMN_ROOT_ID, ROOT_ID_USER_APP);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY  | Root.FLAG_ADVANCED | Root.FLAG_SUPER_ADVANCED | Root.FLAG_SUPPORTS_SEARCH);
        row.add(Root.COLUMN_ICON, R.drawable.ic_root_apps);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_apps));
		row.add(Root.COLUMN_SUMMARY, String.valueOf(countUserApps) + " apps");
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_USER_APP);
        row.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_DATA, false));
        row.add(Root.COLUMN_CAPACITY_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_DATA, true));

		final RowBuilder row1 = result.newRow();
		row1.add(Root.COLUMN_ROOT_ID, ROOT_ID_SYSTEM_APP);
		row1.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY  | Root.FLAG_ADVANCED | Root.FLAG_SUPER_ADVANCED | Root.FLAG_SUPPORTS_SEARCH);
		row1.add(Root.COLUMN_ICON, R.drawable.ic_root_apps);
		row1.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_system_apps));
		row1.add(Root.COLUMN_SUMMARY, String.valueOf(countSystemApps) + " apps");
		row1.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_SYSTEM_APP);
		row1.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_DATA, false));
		row1.add(Root.COLUMN_CAPACITY_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_DATA, true));

		final RowBuilder row2 = result.newRow();
		row2.add(Root.COLUMN_ROOT_ID, ROOT_ID_PROCESS);
		row2.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_ADVANCED | Root.FLAG_SUPER_ADVANCED | Root.FLAG_SUPPORTS_SEARCH);
		row2.add(Root.COLUMN_ICON, R.drawable.ic_root_process);
		row2.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_processes));
		row2.add(Root.COLUMN_SUMMARY, String.valueOf(countProcesses) + " processes");
		row2.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_PROCESS);
		row2.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_RAM, false));
		row2.add(Root.COLUMN_CAPACITY_BYTES, storageUtils.getPartionSize(StorageUtils.PARTITION_RAM, true));

        return result;
    }
    
    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

		if(rootId.startsWith(ROOT_ID_USER_APP)) {
    		List<PackageInfo> allAppList = packageManager.getInstalledPackages( getAppListFlag());
    		for (PackageInfo packageInfo : allAppList) {
    			includeAppFromPackage(result, rootId, packageInfo, false, query.toLowerCase());
    		}
    	}
		else if(rootId.startsWith(ROOT_ID_SYSTEM_APP)) {
			List<PackageInfo> allAppList = packageManager.getInstalledPackages( getAppListFlag());
			for (PackageInfo packageInfo : allAppList) {
				includeAppFromPackage(result, rootId, packageInfo, true, query.toLowerCase());
			}
		}
		else if(rootId.startsWith(ROOT_ID_PROCESS)) {
			List<RunningAppProcessInfo> runningProcessesList = activityManager.getRunningAppProcesses();
			for (RunningAppProcessInfo processInfo : runningProcessesList) {
				includeAppFromProcess(result, rootId, processInfo, query.toLowerCase());
			}
    	}
    	return result;
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
    	final String packageName = getPackageForDocId(docId);
        final long token = Binder.clearCallingIdentity();
        try {
        	if (docId.startsWith(ROOT_ID_USER_APP)) {
				PackageManagerUtils.uninstallApp(getContext(), packageName);
        	}
        	else if(docId.startsWith(ROOT_ID_PROCESS)) {
        		activityManager.killBackgroundProcesses(getPackageForDocId(docId));
        	}
        	notifyDocumentsChanged(docId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

	@Override
	public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
		final String packageName = getPackageForDocId(sourceDocumentId);
		String fromFilePath = "";
		String fileName = "";
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
			ApplicationInfo appInfo = packageInfo.applicationInfo;
			fromFilePath = appInfo.sourceDir;
			fileName = (String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : appInfo.packageName);
			fileName += getAppVersion(packageInfo.versionName);
		} catch (Exception e) {
		}

		final File fileFrom = new File(fromFilePath);
		final File fileTo = Utils.getAppsBackupFile(getContext());
		if(!fileTo.exists()){
			fileTo.mkdir();
		}
		if (!FileUtils.moveDocument(fileFrom, fileTo, fileName)) {
			throw new IllegalStateException("Failed to copy " + fileFrom);
		}
		else{
			FileUtils.updateMediaStore(getContext(), FileUtils.makeFilePath(fileTo.getPath(),
					fileName +"."+ FileUtils.getExtFromFilename(fileFrom.getPath())));
		}
		return fromFilePath;
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
        final MatrixCursor result = new DocumentCursor(resolveDocumentProjection(projection), docId);

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
        	if (docId.startsWith(ROOT_ID_USER_APP)) {
        		List<PackageInfo> allAppList = packageManager.getInstalledPackages(getAppListFlag());
        		for (PackageInfo packageInfo : allAppList) {
        			includeAppFromPackage(result, docId, packageInfo, false, null);
        		}
        	}
			else if (docId.startsWith(ROOT_ID_SYSTEM_APP)) {
				List<PackageInfo> allAppList = packageManager.getInstalledPackages(getAppListFlag());
				for (PackageInfo packageInfo : allAppList) {
					includeAppFromPackage(result, docId, packageInfo, true, null);
				}
			}
        	else if(docId.startsWith(ROOT_ID_PROCESS)) {
        		if(Utils.hasOreo()){
					List<PackageInfo> allAppList = packageManager.getInstalledPackages(getAppListFlag());
					for (PackageInfo packageInfo : allAppList) {
						includeAppFromPackage(result, docId, packageInfo, false, null);
						includeAppFromPackage(result, docId, packageInfo, true, null);
					}
				}
				else if(Utils.hasNougat()){
					List<RunningServiceInfo> runningServices = activityManager.getRunningServices(1000);
					for (RunningServiceInfo process : runningServices) {
						includeAppFromService(result, docId, process, null);
					}
				}
				else if (Utils.hasLollipopMR1()) {
					List<AndroidAppProcess> runningAppProcesses = AndroidProcesses.getRunningAppProcesses();
					for (AndroidAppProcess process : runningAppProcesses) {
						includeAppFromProcess(result, docId, process, null);
					}
				} else {
					List<RunningAppProcessInfo> runningProcessesList = activityManager.getRunningAppProcesses();
					for (RunningAppProcessInfo processInfo : runningProcessesList) {
						includeAppFromProcess(result, docId, processInfo, null);
					}
				}
        	}
        } finally {
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
            //final long id = Long.parseLong(docId);
            //final ContentResolver resolver = getContext().getContentResolver();
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
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_SUPPORTS_DELETE);
    }

	private void includeAppFromProcess(MatrixCursor result, String docId, RunningAppProcessInfo processInfo, String query ) {

		if (processInfo.importance != RunningAppProcessInfo.IMPORTANCE_EMPTY
				&& processInfo.importance != RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
			String process = processInfo.processName;
			process = process.substring(process.lastIndexOf(".") + 1, process.length());
			String summary = "";
			String displayName = "";
			ApplicationInfo appInfo = null;
			try {
				appInfo = packageManager.getPackageInfo(processInfo.processName, PackageManager.GET_ACTIVITIES).applicationInfo;
				displayName = process ;//(String) (appInfo.loadLabel(packageManager) != null ? appInfo.loadLabel(packageManager) : appInfo.packageName);
			} catch (Exception e) { }
			
			if (TextUtils.isEmpty(displayName)) {
				displayName = process;
			}
			
			if (null != query && !displayName.toLowerCase().contains(query)) {
				return;
			}
			final String path = null != appInfo ? appInfo.sourceDir : "";
			final String mimeType = Document.MIME_TYPE_APK;
			
	        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
			if(isTelevision()) {
				flags |= Document.FLAG_DIR_PREFERS_GRID;
			}
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

	private void includeAppFromPackage(MatrixCursor result, String docId, PackageInfo packageInfo,
									   boolean showSystem, String query) {

		ApplicationInfo appInfo = packageInfo.applicationInfo;
		if(showSystem == isSystemApp(appInfo)){
			String displayName = "";
			final String packageName = packageInfo.packageName;
            String summary = packageName;
            displayName = packageName;

			if (null != query && !displayName.toLowerCase().contains(query)) {
				return;
			}
			final String path = appInfo.sourceDir;
			final String mimeType = Document.MIME_TYPE_APK;

			int flags = Document.FLAG_SUPPORTS_COPY | Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
			if(isTelevision()) {
				flags |= Document.FLAG_DIR_PREFERS_GRID;
			}

	        final long size = new File(appInfo.sourceDir).length();
	        final long lastModified = packageInfo.lastUpdateTime;
	        final RowBuilder row = result.newRow();
	        row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
	        row.add(Document.COLUMN_DISPLAY_NAME, getAppName(displayName) + getAppVersion(packageInfo.versionName));
	        row.add(Document.COLUMN_SUMMARY, summary);
	        row.add(Document.COLUMN_SIZE, size);
	        row.add(Document.COLUMN_MIME_TYPE, mimeType);
	        row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
	        row.add(Document.COLUMN_PATH, path);
	        row.add(Document.COLUMN_FLAGS, flags);
		}
    }

	private void includeAppFromProcess(MatrixCursor result, String docId, AndroidAppProcess processInfo, String query ) {

		String process = processInfo.name;
		final String packageName = processInfo.getPackageName();
		process = process.substring(process.lastIndexOf(".") + 1, process.length());
		String summary = "";
		String displayName = "";
		ApplicationInfo appInfo = null;
		try {
			appInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).applicationInfo;
			displayName = process ;
		} catch (Exception e) { }

		if (TextUtils.isEmpty(displayName)) {
			displayName = process;
		}

		if (null != query && !displayName.toLowerCase().contains(query)) {
			return;
		}
		final String path = null != appInfo ? appInfo.sourceDir : "";
		final String mimeType = Document.MIME_TYPE_APK;

		int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
		if(isTelevision()) {
			flags |= Document.FLAG_DIR_PREFERS_GRID;
		}

		int importance = processInfo.foreground ? RunningAppProcessInfo.IMPORTANCE_FOREGROUND : RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
		summary = processTypeCache.get(importance);
		final long size = getProcessSize(processInfo.pid);


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

	private void includeAppFromService(MatrixCursor result, String docId, RunningServiceInfo processInfo, String query ) {

		String process = processInfo.process;
		final String packageName = processInfo.process;
		process = process.substring(process.lastIndexOf(".") + 1, process.length());
		String summary = "";
		String displayName = "";
		ApplicationInfo appInfo = null;
		try {
			appInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).applicationInfo;
			displayName = process ;
		} catch (Exception e) { }

		if (TextUtils.isEmpty(displayName)) {
			displayName = process;
		}

		if (null != query && !displayName.toLowerCase().contains(query)) {
			return;
		}
		final String path = null != appInfo ? appInfo.sourceDir : "";
		final String mimeType = Document.MIME_TYPE_APK;

		int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
		if(isTelevision()) {
			flags |= Document.FLAG_DIR_PREFERS_GRID;
		}

		int importance = processInfo.foreground ? RunningAppProcessInfo.IMPORTANCE_FOREGROUND : RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
		summary = processTypeCache.get(importance);
		final long size = getProcessSize(processInfo.pid);

		final RowBuilder row = result.newRow();
		row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
		row.add(Document.COLUMN_DISPLAY_NAME, displayName);
		row.add(Document.COLUMN_SUMMARY, summary);
		row.add(Document.COLUMN_SIZE, size);
		row.add(Document.COLUMN_MIME_TYPE, mimeType);
		//row.add(Document.COLUMN_LAST_MODIFIED, processInfo.lastActivityTime);
		row.add(Document.COLUMN_PATH, path);
		row.add(Document.COLUMN_FLAGS, flags);
	}

	private static String getAppName(String packageName){
		String name = packageName;
		try {
			int start = packageName.lastIndexOf('.');
			name = start != -1 ? packageName.substring(start+1) : packageName;
			if(name.equalsIgnoreCase("android")){
				start = packageName.substring(0, start).lastIndexOf('.');
				name = start != -1 ? packageName.substring(start+1) : packageName;
			}	
		} catch (Exception e) {
		}
		return capitalize(name);
	}

    private static String capitalize(String string){
        return TextUtils.isEmpty(string) ? string : Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    private static String getAppVersion(String packageVersion){
        return  TextUtils.isEmpty(packageVersion) ? "" : "-" + packageVersion;
    }

    private static int getAppListFlag(){
		return Utils.hasNougat() ? MATCH_UNINSTALLED_PACKAGES : GET_UNINSTALLED_PACKAGES;
	}

	private static boolean isAppUseful(ApplicationInfo appInfo) {
        return appInfo.flags != 0
                && ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                || (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0);
    }

    private static boolean isSystemApp(ApplicationInfo appInfo){
		return appInfo.flags != 0 && (appInfo.flags
				& (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
	}

	public static String getDocIdForApp(String rootId, String packageName){
    	return rootId + packageName;
    }
    
    public static String getPackageForDocId(String docId){
        final int splitIndex = docId.indexOf(':', 1);
        final String packageName = docId.substring(splitIndex + 1);
        return packageName;
    }

	private long getProcessSize(int pid) {
		android.os.Debug.MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(new int[] { pid });
		return memInfos[0].getTotalPss() * 1024;
	}

	/**
	 * Returns a list of application processes that are running on the device.
	 *
	 * @return a list of RunningAppProcessInfo records, or null if there are no
	 * running processes (it will not return an empty list).  This list ordering is not
	 * specified.
	 */
	public static List<RunningAppProcessInfo> getRunningAppProcessInfo(Context ctx) {
		ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcessInfos = new ArrayList<>();
		String prevProcess = "";
		if(Utils.hasOreo()){
			PackageManager packageManager = ctx.getPackageManager();
			List<PackageInfo> allAppList = packageManager.getInstalledPackages( getAppListFlag());
			for (PackageInfo packageInfo : allAppList) {
				RunningAppProcessInfo info = new RunningAppProcessInfo(
						packageInfo.packageName, packageInfo.applicationInfo.uid, null
				);
				info.uid = packageInfo.applicationInfo.uid;
				info.importance = RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
				appProcessInfos.add(info);
			}
			return appProcessInfos;
		} else if(Utils.hasNougat()){
			List<RunningServiceInfo> runningServices = am.getRunningServices(1000);
			for (RunningServiceInfo process : runningServices) {
				RunningAppProcessInfo info = new RunningAppProcessInfo(
						process.process, process.pid, null
				);
				info.uid = process.uid;
				info.importance = process.foreground ? RunningAppProcessInfo.IMPORTANCE_FOREGROUND : RunningAppProcessInfo.IMPORTANCE_BACKGROUND;

				if(!prevProcess.equals(process.process)){
					prevProcess = process.process;
					appProcessInfos.add(info);
				}
			}
			return appProcessInfos;
		} else if (Utils.hasLollipopMR1()) {
			List<AndroidAppProcess> runningAppProcesses = AndroidProcesses.getRunningAppProcesses();
			for (AndroidAppProcess process : runningAppProcesses) {
				RunningAppProcessInfo info = new RunningAppProcessInfo(
						process.name, process.pid, null
				);
				info.uid = process.uid;
                info.importance = process.foreground ? RunningAppProcessInfo.IMPORTANCE_FOREGROUND : RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
				// TODO: Get more information about the process. pkgList, importance, lru, etc.
				appProcessInfos.add(info);
			}
			return appProcessInfos;
		}
		return am.getRunningAppProcesses();
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
}