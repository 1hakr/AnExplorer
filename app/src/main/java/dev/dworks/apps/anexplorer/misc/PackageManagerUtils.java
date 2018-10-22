/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
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

package dev.dworks.apps.anexplorer.misc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import dev.dworks.apps.anexplorer.DocumentsApplication;

public class PackageManagerUtils {

	/** Action */
	public static final String ACTION_FORCE_STOP_REQUEST = "com.android.accessibilityservice.FORCE_STOP_REQUEST";

	/** Action */
	public static final String ACTION_FORCE_STOP_FINISHED = "com.android.accessibilityservice.ACTION_FORCE_STOP_FINISHED";

	/** Extra */
	public static final String EXTRA_ACTION = "action";

	/** 关闭单个 */
	public static final int ACTION_REQUEST_FORCE_STOP = 1 << 0;

	/** Extra */
	public static final String EXTRA_PACKAGE_NAMES = "package_names";

	/**  */
	public static final long TIME_OUT_PERFORM_CLICK = 1500;

	/** task to ignore */
	public static final String[] IGNORE_PROCESSES_LIST = new String[] {
			"system",
			"com.google.process.gapps",
			"com.google.android.gms"
	};

	/**  */
	public static final String FORCE_STOP_STRING_RES_NAME = "com.android.settings:id/force_stop_button";

	/**  */
	public static final String FORCE_STOP_STRING_LEFT_BOTTON = "com.android.settings:id/left_button";

	/**  */
	public static final String FORCE_STOP_STRING_RIGHT_BOTTON = "com.android.settings:id/right_button";

	/**  */
	public static final String OK_STRING_RES_NAME = "android:id/button1";

	public static void startAccessibilitySettings(Context context) {
		Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public static void startApplicationDetailsSettings(Context context, String packageName) {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		Uri uri = Uri.fromParts("package", packageName, null);
		intent.setData(uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	@SuppressWarnings("unused")
	public static boolean deleteApplicationCacheFiles(PackageManager pm, String packageName) {
		Class<?> iPackageDataObserverClass = null;
		boolean deleted = false;
		try {
			Method method = null;
			iPackageDataObserverClass = Class.forName("android.content.pm.IPackageDataObserver");
			Object iPackageDataObserverObject = Proxy.newProxyInstance(
					DocumentsApplication.class.getClassLoader(),
					new Class[] { iPackageDataObserverClass },
					new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							return null;
						}
					});
			method = pm.getClass().getMethod("freeStorage", long.class,  IntentSender.class);
			if (method != null) {
				long desiredFreeStorage = 8 * 1024 * 1024 * 1024; 
				method.invoke(pm, desiredFreeStorage, null);
				deleted = true;
			}
		} catch (Exception e) {
			Throwable t = e.getCause();
			e.printStackTrace();
		}
		
		return deleted;
	}

	public static void forceStopApplication(AccessibilityEvent event) {

		AccessibilityNodeInfo nodeInfo = event.getSource();
		AccessibilityNodeInfoCompat nodeInfoCompat = new AccessibilityNodeInfoCompat(nodeInfo);
		List<AccessibilityNodeInfoCompat> stop_nodes = nodeInfoCompat.findAccessibilityNodeInfosByViewId("com.android.settings:id/left_button");

		if (stop_nodes != null && !stop_nodes.isEmpty()) {
			AccessibilityNodeInfoCompat node;
			for (int i = 0; i < stop_nodes.size(); i++) {
				node = stop_nodes.get(i);
				if (node.getClassName().equals("android.widget.Button")) {
					if (node.isEnabled()) {
						node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
					} else {
						//performGlobalAction(GLOBAL_ACTION_BACK);
					}
					node.recycle();
				}
			}
		}

		List<AccessibilityNodeInfo> ok_nodes = null;
		if (event.getText() != null && event.getText().size() == 4) {
			ok_nodes = nodeInfo.findAccessibilityNodeInfosByText(event.getText().get(3).toString());
		}
		if (ok_nodes != null && !ok_nodes.isEmpty()) {
			AccessibilityNodeInfo node;
			for (int i = 0; i < ok_nodes.size(); i++) {
				node = ok_nodes.get(i);
				if (node.getClassName().equals("android.widget.Button")) {
					node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
					Log.d("action", "click ok");
				}
				node.recycle();
			}
		}
	}

	public static void openAppDetails(Context context, String packageName) {
/*		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		Uri uri = Uri.fromParts("package", packageName, null);
		intent.setData(uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);*/
		Intent intent = new Intent(ACTION_FORCE_STOP_REQUEST);
		intent.putExtra(EXTRA_PACKAGE_NAMES, new String[]{packageName});
		context.sendBroadcast(intent);
	}

	public static void uninstallApp(Context context, String packageName) {
		try {
			Uri packageUri = Uri.fromParts("package", packageName, null);
			if(packageUri != null){
				Intent intentUninstall = new Intent(Intent.ACTION_DELETE, packageUri);
				intentUninstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intentUninstall);
			}
		} catch (Exception e) { }
	}

	public static ComponentName getSettingsComponentName(Context context) {
		return getComponentName(context, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", context.getPackageName(), null)));
	}

	public static ComponentName getComponentName(Context context, Intent intent) {
		PackageManager packageManager = context.getPackageManager();
		ComponentName resolvedComponentName = intent.resolveActivity(packageManager);
		try {
			ActivityInfo activityInfo = packageManager.getActivityInfo(resolvedComponentName, 0);
			if (activityInfo.targetActivity != null) {
				return new ComponentName(resolvedComponentName.getPackageName(), activityInfo.targetActivity);
			}
		} catch (PackageManager.NameNotFoundException e) {
			// TODO nothing
		}
		return resolvedComponentName;
	}
}
