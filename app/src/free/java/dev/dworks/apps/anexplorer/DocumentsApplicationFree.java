package dev.dworks.apps.anexplorer;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;

public class DocumentsApplicationFree extends DocumentsApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        if(!BuildConfig.DEBUG) {
            AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
        }
    }
}
