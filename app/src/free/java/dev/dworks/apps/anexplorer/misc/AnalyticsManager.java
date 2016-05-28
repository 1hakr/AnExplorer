/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.R;

import static dev.dworks.apps.anexplorer.misc.LogUtils.LOGD;

public class AnalyticsManager {
    private static Context sAppContext = null;

    private static FirebaseAnalytics mFirebaseAnalytics;
    private static GoogleAnalytics mGoogleAnalytics;
    private static Tracker mTracker;
    private final static String TAG = LogUtils.makeLogTag(AnalyticsManager.class);

    private static boolean canSend() {
        return sAppContext != null && mTracker != null
                && !BuildConfig.DEBUG ;
    }

    public static void sendScreenView(String screenName) {
        if (canSend()) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
            LOGD(TAG, "Screen View recorded: " + screenName);
        } else {
            LOGD(TAG, "Screen View NOT recorded (analytics disabled or not ready).");
        }
    }

    public static void sendView(String screenName) {
        if (canSend()) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
            LOGD(TAG, "Screen View recorded: " + screenName);
        } else {
            LOGD(TAG, "Screen View NOT recorded (analytics disabled or not ready).");
        }
    }

    public static void sendEvent(String category, String action, String label, long value) {
        if (canSend()) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(value)
                    .build());

            LOGD(TAG, "Event recorded:");
            LOGD(TAG, "\tCategory: " + category);
            LOGD(TAG, "\tAction: " + action);
            LOGD(TAG, "\tLabel: " + label);
            LOGD(TAG, "\tValue: " + value);
        } else {
            LOGD(TAG, "Analytics event ignored (analytics disabled or not ready).");
        }
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, 0);
    }

	public static void startTracking(Activity activity) {
        if (canSend()) {
            mGoogleAnalytics.reportActivityStart(activity);
        } else {
            LOGD(TAG, "Analytics event ignored (analytics disabled or not ready).");
        }
	}

	public static void stopTracking(Activity activity) {
        if (canSend()) {
            mGoogleAnalytics.reportActivityStop(activity);
        } else {
            LOGD(TAG, "Analytics event ignored (analytics disabled or not ready).");
        }
	}

    public static synchronized void setTracker(Tracker tracker) {
        mTracker = tracker;
    }

    public Tracker getTracker() {
        return mTracker;
    }

    public static synchronized void initializeAnalyticsTracker(Context context) {
        sAppContext = context;

        //GA
        if (mTracker == null) {
            int useProfile;
            useProfile = R.xml.analytics;
            mGoogleAnalytics = GoogleAnalytics.getInstance(context);
            mTracker = mGoogleAnalytics.newTracker(useProfile);
            mTracker.enableAdvertisingIdCollection(true);
        }

        //FBA
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }
}
