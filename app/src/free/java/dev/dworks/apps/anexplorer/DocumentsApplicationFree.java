package dev.dworks.apps.anexplorer;

import com.crashlytics.android.Crashlytics;

public class DocumentsApplicationFree extends DocumentsApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.start(this);
    }
}
