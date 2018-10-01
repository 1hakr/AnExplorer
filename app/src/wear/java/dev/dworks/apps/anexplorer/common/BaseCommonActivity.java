
package dev.dworks.apps.anexplorer.common;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivityDelegate;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.drawer.WearableDrawerView;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

@TargetApi(21)
public abstract class BaseCommonActivity extends AppCompatActivity {

    private static final String TAG = "WearableActivity";
    private boolean mSuperCalled;
    private final WearableActivityDelegate mDelegate;

    public BaseCommonActivity() {
        this.mDelegate = new WearableActivityDelegate(this.callback);
    }

    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mDelegate.onCreate(this);
        setAmbientEnabled();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        WearableDrawerView wearableDrawerView = findViewById(R.id.top_navigation_drawer);
        if(null != wearableDrawerView) {
            wearableDrawerView.setBackgroundColor(SettingsActivity.getPrimaryColor());
        }
    }

    @CallSuper
    protected void onResume() {
        super.onResume();
        this.mDelegate.onResume();
    }

    @CallSuper
    protected void onPause() {
        this.mDelegate.onPause();
        super.onPause();
    }

    @CallSuper
    protected void onStop() {
        this.mDelegate.onStop();
        super.onStop();
    }

    @CallSuper
    protected void onDestroy() {
        this.mDelegate.onDestroy();
        super.onDestroy();
    }

    public final void setAmbientEnabled() {
        this.mDelegate.setAmbientEnabled();
    }

    public final void setAutoResumeEnabled(boolean enabled) {
        this.mDelegate.setAutoResumeEnabled(enabled);
    }

    public final void setAmbientOffloadEnabled(boolean enabled) {
        this.mDelegate.setAmbientOffloadEnabled(enabled);
    }

    public final boolean isAmbient() {
        return this.mDelegate.isAmbient();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mDelegate.dump(prefix, fd, writer, args);
    }

    @CallSuper
    public void onEnterAmbient(Bundle ambientDetails) {
        this.mSuperCalled = true;
    }

    @CallSuper
    public void onUpdateAmbient() {
        this.mSuperCalled = true;
    }

    @CallSuper
    public void onExitAmbient() {
        this.mSuperCalled = true;
    }

    private final WearableActivityDelegate.AmbientCallback callback = new WearableActivityDelegate.AmbientCallback() {
        public void onEnterAmbient(Bundle ambientDetails) {
            BaseCommonActivity.this.mSuperCalled = false;
            BaseCommonActivity.this.onEnterAmbient(ambientDetails);
            if (!BaseCommonActivity.this.mSuperCalled) {
                String var2 = String.valueOf(BaseCommonActivity.this);
                Log.w("WearableActivity", (new StringBuilder(56 + String.valueOf(var2).length())).append("Activity ").append(var2).append(" did not call through to super.onEnterAmbient()").toString());
            }

        }

        public void onExitAmbient() {
            BaseCommonActivity.this.mSuperCalled = false;
            BaseCommonActivity.this.onExitAmbient();
            if (!BaseCommonActivity.this.mSuperCalled) {
                String var1 = String.valueOf(BaseCommonActivity.this);
                Log.w("WearableActivity", (new StringBuilder(55 + String.valueOf(var1).length())).append("Activity ").append(var1).append(" did not call through to super.onExitAmbient()").toString());
            }

        }

        public void onUpdateAmbient() {
            BaseCommonActivity.this.mSuperCalled = false;
            BaseCommonActivity.this.onUpdateAmbient();
            if (!BaseCommonActivity.this.mSuperCalled) {
                String var1 = String.valueOf(BaseCommonActivity.this);
                Log.w("WearableActivity", (new StringBuilder(57 + String.valueOf(var1).length())).append("Activity ").append(var1).append(" did not call through to super.onUpdateAmbient()").toString());
            }

        }
    };
}