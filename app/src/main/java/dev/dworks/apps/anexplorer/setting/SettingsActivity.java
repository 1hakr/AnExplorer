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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.SettingsCommonActivity;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.PreferenceUtils;
import dev.dworks.apps.anexplorer.misc.SystemBarTintManager;
import dev.dworks.apps.anexplorer.misc.Utils;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;

public class SettingsActivity extends SettingsCommonActivity {

    public static final String TAG = "Settings";

    private static final int FRAGMENT_OPEN = 99;

    private static final String EXTRA_RECREATE = "recreate";
    public static final String KEY_ADVANCED_DEVICES = "advancedDevices";
    public static final String KEY_FILE_SIZE = "fileSize";
    public static final String KEY_FOLDER_SIZE = "folderSize";
    public static final String KEY_FILE_THUMBNAIL = "fileThumbnail";
    public static final String KEY_FILE_HIDDEN = "fileHidden";
    public static final String KEY_SECURITY_ENABLED = "security_enable";
    public static final String KEY_ROOT_MODE = "rootMode";
    public static final String KEY_PRIMARY_COLOR = "primaryColor";
    public static final String KEY_ACCENT_COLOR = "accentColor";
    public static final String KEY_THEME_STYLE = "themeStyle";
    public static final String KEY_FOLDER_ANIMATIONS = "folderAnimations";
    public static final String KEY_RECENT_MEDIA = "recentMedia";

	private Resources res;
	private int actionBarColor;
    private final Handler handler = new Handler();
    private Drawable oldBackground;
    private boolean mRecreate = false;

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

    public static boolean getDisplayFileHidden(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILE_HIDDEN, false);
    }

    public static boolean getDisplayRecentMedia() {
        return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext())
                .getBoolean(KEY_RECENT_MEDIA, !isWatch());
    }

    public static boolean getRootMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ROOT_MODE, true);
    }
    
    public static int getPrimaryColor(Context context) {
    	int newColor = ContextCompat.getColor(context, R.color.primaryColor);
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_PRIMARY_COLOR, newColor);
    }

    public static int getPrimaryColor() {
        return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext())
                .getInt(KEY_PRIMARY_COLOR, Color.parseColor("#0288D1"));
    }

    public static int getAccentColor() {
        return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext())
                .getInt(KEY_ACCENT_COLOR, Color.parseColor("#EF3A0F"));
    }

    public static void setAccentColor(int color) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_ACCENT_COLOR, color);
        editor.commit();
    }

    public static String getThemeStyle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_THEME_STYLE, String.valueOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    public static String getThemeStyle() {
        return PreferenceManager.getDefaultSharedPreferences(DocumentsApplication.getInstance().getBaseContext())
                .getString(KEY_THEME_STYLE, String.valueOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    public static void setThemeStyle(int style) {
        PreferenceUtils.set(KEY_THEME_STYLE, String.valueOf(style));
    }
    
    public static boolean getFolderAnimation(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FOLDER_ANIMATIONS, true);
    }

    public static boolean isSecurityEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_SECURITY_ENABLED, false);
    }

    public static void setSecurityEnabled(Context context, boolean enable) {
        PreferenceUtils.set(KEY_SECURITY_ENABLED, enable);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        changeActionBarColor(0);
        res = getResources();
        actionBarColor = getPrimaryColor(this);
        if(null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
    	super.onResume();
        changeActionBarColor(0);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivityForResult(intent, FRAGMENT_OPEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == FRAGMENT_OPEN){
            if(resultCode == RESULT_FIRST_USER){
                recreate();
            }
        }
    }

    @Override
    public void recreate() {
        mRecreate = true;
        super.recreate();
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXTRA_RECREATE, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if(state.getBoolean(EXTRA_RECREATE)){
            setResult(RESULT_FIRST_USER);
        }
    }

    public void changeActionBarColor(int newColor) {

		int color = newColor != 0 ? newColor : SettingsActivity.getPrimaryColor(this);
		Drawable colorDrawable = new ColorDrawable(color);

		if (oldBackground == null) {
            getSupportActionBar().setBackgroundDrawable(colorDrawable);

        } else {
			TransitionDrawable td = new TransitionDrawable(new Drawable[] { oldBackground, colorDrawable });
            getSupportActionBar().setBackgroundDrawable(td);
			td.startTransition(200);
		}

		oldBackground = colorDrawable;
        setUpStatusBar();
	}

    public void setUpStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor(this));
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(color);
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

	public static void logSettingEvent(String key){
        AnalyticsManager.logEvent("settings_"+key.toLowerCase());
    }

}