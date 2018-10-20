package dev.dworks.apps.anexplorer.misc;

import android.app.Activity;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class SecurityHelper {

    public static final String TAG = "SecurityHelper";
    public static final int REQUEST_CONFIRM_CREDENTIALS = 1212;
    private final KeyguardManager mKeyguardManager;
    public Activity mActivity;
    private Fragment mFragment;

    public interface SecurityCallback {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    public SecurityHelper(Activity activity){
        mActivity = activity;
        mKeyguardManager = (KeyguardManager) mActivity.getSystemService(Context.KEYGUARD_SERVICE);
    }

    public SecurityHelper(Fragment fragment){
        mFragment = fragment;
        mKeyguardManager = (KeyguardManager) mFragment.getActivity().getSystemService(Context.KEYGUARD_SERVICE);
    }

    public boolean isDeviceSecure(){
        if(Utils.hasMarshmallow()){
            return mKeyguardManager.isDeviceSecure();
        } else {
            return mKeyguardManager.isKeyguardSecure();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void authenticate(String title, String description){
        if (isDeviceSecure()) {
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(title, description);
            if (intent != null) {
                startActivity(intent, REQUEST_CONFIRM_CREDENTIALS);
            }
        }
    }

    public void startActivity(Intent intent, int requestCode) {
        if(null != mActivity){
            mActivity.startActivityForResult(intent, requestCode);
        } else if (null != mFragment) {
            mFragment.startActivityForResult(intent, requestCode);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void authenticate(){
        authenticate(null, null);
    }
}
