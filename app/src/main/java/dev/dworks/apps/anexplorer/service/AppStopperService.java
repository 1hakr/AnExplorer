package dev.dworks.apps.anexplorer.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;

import dev.dworks.apps.anexplorer.apps.AppDetailsAutomatorManager;

import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.ACTION_FORCE_STOP_REQUEST;
import static dev.dworks.apps.anexplorer.misc.PackageManagerUtils.EXTRA_PACKAGE_NAMES;

/**
 * Created by HaKr on 31/10/16.
 */

public class AppStopperService extends AccessibilityService {

    private static final String TAG = AppStopperService.class.getSimpleName();

    private AppDetailsAutomatorManager mForceStopManager;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int eventType = accessibilityEvent.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (mForceStopManager.isForceStopRequested()) {
                    mForceStopManager.onAccessibilityEvent(accessibilityEvent);
                }
                break;
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        registerBroadCastReceiver();
        mForceStopManager = new AppDetailsAutomatorManager(getApplicationContext());
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        super.onDestroy();
    }

    /**
     * Registers the phone state observing broadcast receiver.
     */
    private void registerBroadCastReceiver() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    } else if (ACTION_FORCE_STOP_REQUEST.equals(action)) {
                        ArrayList<String> packageNames = intent.getStringArrayListExtra(EXTRA_PACKAGE_NAMES);
                        mForceStopManager.setPackageNames(packageNames);
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(ACTION_FORCE_STOP_REQUEST);
            registerReceiver(mBroadcastReceiver, filter, null, null);
        }
    }
}
