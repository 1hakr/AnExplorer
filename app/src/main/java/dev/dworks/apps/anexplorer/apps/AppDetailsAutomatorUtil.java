package dev.dworks.apps.anexplorer.apps;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import dev.dworks.apps.anexplorer.misc.LogUtils;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.IGNORE_PROCESSES_LIST;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.TIME_OUT_PERFORM_CLICK;

public class AppDetailsAutomatorUtil {

	private static final String TAG = AppDetailsAutomatorUtil.class.getSimpleName();


	/***
	 * Find a matched node by tracing the tree of nodes.
	 * @param source
	 * @param text
	 * @return
	 */
	public static AccessibilityNodeInfo findAccessibilityNodeInfo(AccessibilityNodeInfo source, String text) {
		AccessibilityNodeInfo accessibilityNodeInfo = null;
		if (TextUtils.isEmpty(text)) {
			return accessibilityNodeInfo;
		}
		
		for (int i = 0; i < source.getChildCount(); i++) {
			AccessibilityNodeInfo compareNode = source.getChild(i);
			if (compareNode != null && compareNode.getText() != null) {

				LogUtils.LOGD(TAG, "(findAccessibilityNodeInfo) completeNode : " + compareNode.getClassName() + ", " + compareNode.getText());

				if (text.equals(compareNode.getText())) {
					LogUtils.LOGD(TAG, "(findAccessibilityNodeInfo) Find node : " + compareNode.getClassName() + ", " + compareNode.getText());
					accessibilityNodeInfo = compareNode;
				}
				if (accessibilityNodeInfo == null) {
					accessibilityNodeInfo = findAccessibilityNodeInfo(compareNode, text);
				}
				if (accessibilityNodeInfo == null) {
					compareNode.recycle();
				} else {
					break;
				}
			}
		}
		
		return accessibilityNodeInfo;
	}
	
	/***
	 * Find a matched node by the method, {@link AccessibilityNodeInfo#findAccessibilityNodeInfosByViewId}.
	 * @param accessibilityNodeInfo
	 * @param stringResName
	 * @return
	 */
	public static AccessibilityNodeInfo getAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo, String stringResName) {
		AccessibilityNodeInfo foundNodeInfo = null;
		List<AccessibilityNodeInfo> foundNodeInfos;
		if (Utils.hasJellyBeanMR2()) {
			foundNodeInfos = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(stringResName);
			if ((foundNodeInfos != null) && (!foundNodeInfos.isEmpty())) {
				foundNodeInfo = foundNodeInfos.get(0);
			} else {
				LogUtils.LOGD(TAG, "(getAccessibilityNodeInfo) Not found : " + stringResName);
			}
			while (foundNodeInfos.size() > 1) {
				foundNodeInfos.remove(-1 + foundNodeInfos.size()).recycle();
			}
		}
		return foundNodeInfo;
	}
	
	/***
	 * Check the node whether it is visible.
	 * @param accessibilityNodeInfo
	 * @return
	 */
    public static boolean isVisibleToUser(AccessibilityNodeInfo accessibilityNodeInfo) {
    	if (accessibilityNodeInfo == null) {
    		return false;
    	}
      return accessibilityNodeInfo.isEnabled() && accessibilityNodeInfo.isVisibleToUser();
    }
    
    /***
     * 
     * @param accessibilityNodeInfo
     * @return
     */
    public static boolean performClickAction(AccessibilityNodeInfo accessibilityNodeInfo) {
    	if (!isVisibleToUser(accessibilityNodeInfo)) {
    		return false;
    	}
    	CharSequence nodeInfoText = accessibilityNodeInfo.getText();
    	if (!accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
			LogUtils.LOGD(TAG, "(performAction) Failed to click " + nodeInfoText);
    		return false;
    	}

		LogUtils.LOGD(TAG, "(performAction) Clicked " + nodeInfoText);
    	return true;
    }
    
    public static void checkVisibleToUserTimeOut (AccessibilityNodeInfo accessibilityNodeInfo) {
    	final long currentTime = System.currentTimeMillis();
		while (!isVisibleToUser(accessibilityNodeInfo) && System.currentTimeMillis() - currentTime < TIME_OUT_PERFORM_CLICK) {
			// check node's visibility by time out
		}
    }

	public static void checkInvisibleToUserTimeOut (AccessibilityNodeInfo accessibilityNodeInfo) {
		final long currentTime = System.currentTimeMillis();
		while (isVisibleToUser(accessibilityNodeInfo) && System.currentTimeMillis() - currentTime < TIME_OUT_PERFORM_CLICK) {
			// check node's visibility by time out
		}
	}
    
    /***
     * Check 
     * @param context
     * @param processName
     * @return
     */
    public static boolean ignoreTask(Context context, String processName) {
		for (String systemimPortanceProcesses : IGNORE_PROCESSES_LIST) {
			if (systemimPortanceProcesses.equals(processName)) {
				return true;
			}
		}
        return processName != null && processName.startsWith(context.getPackageName());
    }
}