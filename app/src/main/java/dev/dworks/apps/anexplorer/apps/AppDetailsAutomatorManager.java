package dev.dworks.apps.anexplorer.apps;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.LinkedList;

import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.LogUtils;
import dev.dworks.apps.anexplorer.misc.PackageManagerUtils;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.ACTION_FORCE_STOP_FINISHED;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.FORCE_STOP_STRING_LEFT_BOTTON;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.FORCE_STOP_STRING_RES_NAME;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.FORCE_STOP_STRING_RIGHT_BOTTON;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.OK_STRING_RES_NAME;

/***
 * 
 * @author zhoudawei
 *
 */
public class AppDetailsAutomatorManager {

	private static final String TAG = AppDetailsAutomatorManager.class.getSimpleName();

	private static final int MSG_PERFORM_STOP = 0x1;
	private static ComponentName sSettingsComponentName;
	private Context mContext;
	
	/** text: com.android.settings:string/finish_application */
	private String mFinishApplication;
	/** text: com.android.settings:string/force_stop */
	private String mForceStop;
	/** text: com.android.settings:string/clear_user_data_text */
	private String mClearUserDataText;
	/** text: com.android.settings:string/uninstall_text */
	private String mUninstallText;
	/** text: com.android.settings:string/dlg_ok */
	private String mDlgOk;
	/** text: com.android.settings:string/dlg_cancel */
	private String mDlgCancel;
	
    LinkedList<String> mPackageNames = new LinkedList<String>();
	
	private boolean mForceStopRequested;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_PERFORM_STOP:
				if (mPackageNames.isEmpty()) {
					forceStopFinished();
					return;
				}
				String packageName = mPackageNames.poll();
				PackageManagerUtils.startApplicationDetailsSettings(mContext, packageName);
				break;
			}
		}
	};
	
	public AppDetailsAutomatorManager(Context context) {
		mContext = context;
		sSettingsComponentName = PackageManagerUtils.getSettingsComponentName(context);
		initSettingsString();
	}
	
	public void initSettingsString() {
		mFinishApplication = getSettingsString("finish_application");
		mForceStop = getSettingsString("force_stop");
		mClearUserDataText = getSettingsString("clear_user_data_text");
		mUninstallText = getSettingsString("uninstall_text");
		mDlgOk = getSettingsString("dlg_ok");
		mDlgCancel = getSettingsString("dlg_cancel");
	}
	
	public boolean isForceStopRequested() {
		return mForceStopRequested;
	}
	
	public void onAccessibilityEvent(AccessibilityEvent event) {
		
		if (sSettingsComponentName == null || !isSettings(event)) {
			return;
		}

		AccessibilityNodeInfo source = event.getSource();
		if (source == null) {
			return;
		}
		
		LogUtils.LOGD(TAG, "source : " + source.getClassName() + ", " + source.getText());

		try {
			if (isAppDetail(event)) {
				handleAppDetail(source);
			} else if (isAlertDialog(event)) {
				handleAlertDialog(source);
			}
		} finally {
			source.recycle();
		}
	}
	
	private boolean isAlertDialog(AccessibilityEvent event) {
		return "android.app.AlertDialog".equals(event.getClassName()) || event.getClassName().toString().endsWith("AlertDialog");
	}
	
	private boolean isAppDetail(AccessibilityEvent event) {
		return sSettingsComponentName.getClassName().equals(event.getClassName());
	}
	
	private boolean isSettings(AccessibilityEvent event) {
		return sSettingsComponentName.getPackageName().equals(event.getPackageName());
	}
	
	private void handleAppDetail(AccessibilityNodeInfo source) {
		AccessibilityNodeInfo forceStopNodeInfo = null;
		forceStopNodeInfo = AppDetailsAutomatorUtil.getAccessibilityNodeInfo(source, FORCE_STOP_STRING_RES_NAME);
		if (forceStopNodeInfo == null) {
			if (Utils.hasLollipop()) {
				forceStopNodeInfo = AppDetailsAutomatorUtil.getAccessibilityNodeInfo(source, FORCE_STOP_STRING_RIGHT_BOTTON);
			} else {
				forceStopNodeInfo = AppDetailsAutomatorUtil.getAccessibilityNodeInfo(source, FORCE_STOP_STRING_LEFT_BOTTON);
			}
		}
		if (forceStopNodeInfo == null) {
			forceStopNodeInfo = AppDetailsAutomatorUtil.findAccessibilityNodeInfo(source, mForceStop);
		}
		if (forceStopNodeInfo == null) {
			forceStopNodeInfo = AppDetailsAutomatorUtil.findAccessibilityNodeInfo(source, mFinishApplication);
		}
		boolean performClick = false;
		if (forceStopNodeInfo != null) {
			AppDetailsAutomatorUtil.checkVisibleToUserTimeOut(forceStopNodeInfo);
			if (AppDetailsAutomatorUtil.isVisibleToUser(forceStopNodeInfo)) {
				AppDetailsAutomatorUtil.performClickAction(forceStopNodeInfo);
				performClick = true;
			}
			forceStopNodeInfo.recycle();
		}
		
		if (!performClick) {
			if (mPackageNames.isEmpty()) {
				source.performAction(AccessibilityService.GLOBAL_ACTION_BACK);
			}
			mHandler.sendEmptyMessage(MSG_PERFORM_STOP);
		}
	}
	
	private void handleAlertDialog(AccessibilityNodeInfo source) {
		AccessibilityNodeInfo okNodeInfo = null;
		okNodeInfo = AppDetailsAutomatorUtil.getAccessibilityNodeInfo(source, OK_STRING_RES_NAME);
		if (okNodeInfo == null) {
			okNodeInfo = AppDetailsAutomatorUtil.findAccessibilityNodeInfo(source, mDlgOk);
		}
		if (okNodeInfo != null) {
			AppDetailsAutomatorUtil.checkVisibleToUserTimeOut(okNodeInfo);
			AppDetailsAutomatorUtil.performClickAction(okNodeInfo);
			AppDetailsAutomatorUtil.checkInvisibleToUserTimeOut(okNodeInfo);
			okNodeInfo.recycle();
		}
	}
	
	private String getSettingsString(String stringResName) {
		String stringRes = null;
		try {
			final String settingsPackageName = sSettingsComponentName.getPackageName();
			final Resources resources = mContext.getPackageManager().getResourcesForApplication(settingsPackageName);
			int stringResId = resources.getIdentifier(stringResName, "string", settingsPackageName);
			if (stringResId > 0) {
				String str = resources.getString(stringResId);
				stringRes = str;
			} else {
			}
		} catch (Exception e) {
			CrashReportingManager.logException(e);
		}
		return stringRes;
	}
    
    public void setPackageNames(ArrayList<String> packageNames) {
    	mPackageNames.clear();
    	if (packageNames != null && packageNames.size() > 0) {
    		for (String packageName : packageNames) {
    			mPackageNames.add(packageName);
    		}
		}
    	requestForceStop();
    }
    
	private void requestForceStop() {
		mForceStopRequested = true;
		mHandler.sendEmptyMessage(MSG_PERFORM_STOP);
	}
	
	private void forceStopFinished() {
		mForceStopRequested = false;
		Intent intent = new Intent(ACTION_FORCE_STOP_FINISHED);
		mContext.sendBroadcast(intent);
	}
}