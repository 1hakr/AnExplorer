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
import android.os.Bundle;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.model.RootInfo;

public class AnalyticsManager {
    private static Context sAppContext = null;

    private final static String TAG = LogUtils.makeLogTag(AnalyticsManager.class);

    public static String FILE_TYPE = "file_type";
    public static String FILE_COUNT = "file_count";
    public static String FILE_MOVE = "file_move";

    private static boolean canSend() {
        return sAppContext != null
                && !BuildConfig.DEBUG ;
    }

    public static synchronized void intialize(Context context) {

    }

    public static void setProperty(String propertyName, String propertyValue){

    }

    public static void logEvent(String eventName){

    }

    public static void logEvent(String eventName, Bundle params){

    }

    public static void logEvent(String eventName, RootInfo rootInfo, Bundle params){

    }

    public static void setCurrentScreen(Activity activity, String screenName){

    }
}
