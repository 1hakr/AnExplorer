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

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

import java.util.List;

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
    public static final String KEY_ACTIONBAR_COLOR = "actionBarColor";
    public static final String KEY_FOLDER_ANIMATIONS = "folderAnimations";
    
    private static final String KEY_PIN = "pin";
    private static final String PIN_ENABLED = "pin_enable";
	
	public boolean translucentMode = false;
	private boolean mShowAsDialog;
	private Resources res;
	private int actionBarColor;
    
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
    
    public static int getActionBarColor(Context context) {
    	int newColor = context.getResources().getColor(R.color.actionbar_color);
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_ACTIONBAR_COLOR, newColor);
    }
    
    public static boolean getFolderAnimation(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FOLDER_ANIMATIONS, false);
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
        changeActionBarColor(0);
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
        
        translucentMode = getTranslucentMode(this);
        actionBarColor = getActionBarColor(this);
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
    protected void onResume() {
    	super.onResume();
    	if(getActionBarColor(this) != actionBarColor){
    		changeActionBarColor(0);
    	}
    }
    
    @Override
    public void finish() {
    	if((getTranslucentMode(this) != translucentMode)){
    		setResult(RESULT_FIRST_USER);
    	}
    	super.finish();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putBoolean("changed", translucentMode);
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle state) {
    	super.onRestoreInstanceState(state);
    	translucentMode = state.getBoolean("changed");
    }
    
    private final Handler handler = new Handler();
	private Drawable oldBackground;
	public void changeActionBarColor(int newColor) {

		int color = newColor != 0 ? newColor : SettingsActivity.getActionBarColor(this);
		Drawable colorDrawable = new ColorDrawable(color);
		Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);
		LayerDrawable ld = new LayerDrawable(new Drawable[] { colorDrawable, bottomDrawable });

		if (oldBackground == null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
				ld.setCallback(drawableCallback);
			} else {
				getActionBar().setBackgroundDrawable(ld);
			}

		} else {
			TransitionDrawable td = new TransitionDrawable(new Drawable[] { oldBackground, ld });
			// workaround for broken ActionBarContainer drawable handling on
			// pre-API 17 builds
			// https://github.com/android/platform_frameworks_base/commit/a7cc06d82e45918c37429a59b14545c6a57db4e4
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
				td.setCallback(drawableCallback);
			} else {
				getActionBar().setBackgroundDrawable(td);
			}
			td.startTransition(200);
		}

		oldBackground = ld;
		
		// http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(true);
	}
	
	private Drawable.Callback drawableCallback = new Drawable.Callback() {
		@Override
		public void invalidateDrawable(Drawable who) {
			getActionBar().setBackgroundDrawable(who);
		}

		@Override
		public void scheduleDrawable(Drawable who, Runnable what, long when) {
			handler.postAtTime(what, when);
		}

		@Override
		public void unscheduleDrawable(Drawable who, Runnable what) {
			handler.removeCallbacks(what);
		}
	};
}