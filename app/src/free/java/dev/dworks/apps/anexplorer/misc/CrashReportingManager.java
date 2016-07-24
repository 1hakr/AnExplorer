package dev.dworks.apps.anexplorer.misc;

import com.google.firebase.crash.FirebaseCrash;

/**
 * Created by HaKr on 23/05/16.
 */

public class CrashReportingManager {

    public static void logException(Exception e) {
        FirebaseCrash.report(e);
    }
}
