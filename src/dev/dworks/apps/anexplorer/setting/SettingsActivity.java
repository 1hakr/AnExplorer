/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
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

package dev.dworks.apps.anexplorer.setting;

import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.ViewCompat;

public class SettingsActivity extends PreferenceActivity {
	
    private static final String KEY_ADVANCED_DEVICES = "advancedDevices";
    private static final String KEY_FILE_SIZE = "fileSize";
    private static final String KEY_FOLDER_SIZE = "folderSize";
    private static final String KEY_FILE_THUMBNAIL = "fileThumbnail";
    public static final String KEY_ROOT_MODE = "rootMode";
    public static final String KEY_TRANSLUCENT_MODE = "translucentMode";
    public static final String KEY_AS_DIALOG = "asDialog";
    
    private static final String KEY_PIN = "pin";
    private static final String PIN_ENABLED = "pin_enable";
	
	public boolean changed = false;
	private boolean mShowAsDialog;
	private Resources res;
    
    public static boolean getDisplayAdvancedDevices(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ADVANCED_DEVICES, true);
    }

    public static boolean getDisplayFileSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILE_SIZE, true);
    }

    public static boolean getDisplayFolderSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FOLDER_SIZE, false);
    }
    
    public static boolean getDisplayFileThumbnail(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILE_THUMBNAIL, true);
    }
    
    public static boolean getRootMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ROOT_MODE, false);
    }
    
    public static boolean getTranslucentMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_TRANSLUCENT_MODE, false);
    }
    
    public static boolean getAsDialog(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_AS_DIALOG, false);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowHomeEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
        }
        
        res = getResources();
        mShowAsDialog = res.getBoolean(R.bool.show_as_dialog);

        if (mShowAsDialog) {
        	if(SettingsActivity.getAsDialog(this)){
                // backgroundDimAmount from theme isn't applied; do it manually
                final WindowManager.LayoutParams a = getWindow().getAttributes();
                a.dimAmount = 0.6f;
                getWindow().setAttributes(a);

                getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                getWindow().setFlags(~0, WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // Inset ourselves to look like a dialog
                final Point size = new Point();
                getWindowManager().getDefaultDisplay().getSize(size);

                final int width = (int) res.getFraction(R.dimen.dialog_width, size.x, size.x);
                final int height = (int) res.getFraction(R.dimen.dialog_height, size.y, size.y);
                final int insetX = (size.x - width) / 2;
                final int insetY = (size.y - height) / 2;

                final Drawable before = getWindow().getDecorView().getBackground();
                final Drawable after = new InsetDrawable(before, insetX, insetY, insetX, insetY);
                ViewCompat.setBackground(getWindow().getDecorView(), after);

                // Dismiss when touch down in the dimmed inset area
                getWindow().getDecorView().setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            final float x = event.getX();
                            final float y = event.getY();
                            if (x < insetX || x > v.getWidth() - insetX || y < insetY
                                    || y > v.getHeight() - insetY) {
                                finish();
                                return true;
                            }
                        }
                        return false;
                    }
                });	
        	}
        }
        
        changed = getTranslucentMode(this);
    }

	/** {@inheritDoc} */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected boolean isValidFragment(String fragmentName) {
    	recreate();
    	return true;
    }
    
    public void setRestart(boolean restart){
    	this.changed = restart;
    }
    
	public static final boolean isPinEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PIN_ENABLED, false)
        		&& isPinProtected(context);
    }
	
    public static final boolean isPinProtected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PIN, "") != "";
    }
    
    public static void setPin(Context context, String pin) {
    	PreferenceManager.getDefaultSharedPreferences(context).edit().putString(KEY_PIN, hashKeyForPIN(pin)).commit();
    }
    
    public static boolean checkPin(Context context, String pin) {
        pin = hashKeyForPIN(pin);
        String hashed = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PIN, "");
        if (TextUtils.isEmpty(pin))
            return TextUtils.isEmpty(hashed);
        return pin.equals(hashed);
    }
    
    private static String hashKeyForPIN(String value) {
        if (TextUtils.isEmpty(value))
            return null;
        try {
            //MessageDigest digester = MessageDigest.getInstance("MD5");
            //return Base64.encodeToString(value.getBytes(), Base64.DEFAULT);
        }
        catch (Exception e) {
        }
        return value;
    }
    
/*    public static String hashKeyForPIN(String key) {
        String cacheKey = key;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        cacheKey = Base64.encodeToString(key.getBytes(), Base64.DEFAULT);
        return cacheKey;
    }*/
    
    @Override
    public void finish() {
    	if(getTranslucentMode(this) != changed){
    		setResult(RESULT_FIRST_USER);
    	}
    	super.finish();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putBoolean("changed", changed);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
    	super.onRestoreInstanceState(state);
    	changed = state.getBoolean("changed");
    }
}