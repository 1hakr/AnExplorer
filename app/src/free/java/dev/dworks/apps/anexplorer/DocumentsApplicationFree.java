package dev.dworks.apps.anexplorer;

import com.crashlytics.android.Crashlytics;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import io.fabric.sdk.android.Fabric;

public class DocumentsApplicationFree extends DocumentsApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
        if(!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
    }
}
