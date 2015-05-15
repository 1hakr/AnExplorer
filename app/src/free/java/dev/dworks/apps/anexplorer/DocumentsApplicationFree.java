package dev.dworks.apps.anexplorer;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;

import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import io.fabric.sdk.android.Fabric;

public class DocumentsApplicationFree extends DocumentsApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        AnalyticsManager.initializeAnalyticsTracker(getApplicationContext());
        Fabric.with(this, new Crashlytics());
    }
}
