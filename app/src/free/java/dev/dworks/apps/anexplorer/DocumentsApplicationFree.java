package dev.dworks.apps.anexplorer;

import com.crashlytics.android.Crashlytics;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import io.fabric.sdk.android.Fabric;

public class DocumentsApplicationFree extends DocumentsApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        if(!BuildConfig.DEBUG) {
            AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
            Fabric.with(this, new Crashlytics());
        }
    }
}
