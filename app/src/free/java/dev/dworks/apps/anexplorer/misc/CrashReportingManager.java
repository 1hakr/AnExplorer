package dev.dworks.apps.anexplorer.misc;

import com.google.firebase.crash.FirebaseCrash;

import dev.dworks.apps.anexplorer.BuildConfig;

/**
 * Created by HaKr on 23/05/16.
 */

public class CrashReportingManager {

    public static void logException(Exception e) {
        if(BuildConfig.DEBUG){
            e.printStackTrace();
        } else {
            FirebaseCrash.report(e);
        }
    }
}
