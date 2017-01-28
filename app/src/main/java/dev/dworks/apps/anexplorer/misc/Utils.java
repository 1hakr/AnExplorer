/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
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

package dev.dworks.apps.anexplorer.misc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;

import java.io.File;
import java.util.List;
import java.util.Locale;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES;

public class Utils {

    public static final long KB_IN_BYTES = 1024;
    public static final String DIRECTORY_APPBACKUP = "AppBackup";

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_ROOT = "root";
    public static final String EXTRA_DOC = "doc";
    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_CONNECTION_ID = "connection_id";
    public static final String EXTRA_IGNORE_STATE = "ignoreState";

    static final String[] BinaryPlaces = { "/data/bin/", "/system/bin/", "/system/xbin/", "/sbin/",
        "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/",
        "/data/local/" };
	private static final StringBuilder sBuilder = new StringBuilder(50);
	private static final java.util.Formatter sFormatter = new java.util.Formatter(
	            sBuilder, Locale.getDefault());

	public static String formatDateRange(Context context, long start, long end) {
		final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;

		synchronized (sBuilder) {
			sBuilder.setLength(0);
			return DateUtils.formatDateRange(context, sFormatter, start, end,
					flags, null).toString();
		}
	}
	
    public static boolean hasFroyo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasIceCreamSandwich() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }
    
    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
    
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasLollipopMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean hasNougatMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public static boolean hasMoreHeap(){
    	return Runtime.getRuntime().maxMemory() > 20971520;
    }
    
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isLowRamDevice(Context context) {
    	if(Utils.hasKitKat()){
    		final ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    		return am.isLowRamDevice();
    	}
    	return !hasMoreHeap();
	}

    public static boolean isTablet(Context context) {
		return context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}
    
	public static int parseMode(String mode) {
        final int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Bad mode '" + mode + "'");
        }
        return modeBits;
    }
    
    /**
     * Recursively delete everything in {@code dir}.
     */
    public static void deleteContents(File dir){
        File[] files = dir.listFiles();
        if (files == null) {
        	return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                
            }
        }
    }
    
    public static boolean isRooted(){
        for (String p : Utils.BinaryPlaces) {
            File su = new File(p + "su");
            if (su.exists()) {
                return true;
            }
        }
        return false;//RootTools.isRootAvailable();
    }
    
    public static String formatTime(Context context, long when) {
		// TODO: DateUtils should make this easier
		Time then = new Time();
		then.set(when);
		Time now = new Time();
		now.setToNow();

		int flags = DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL;

		if (then.year != now.year) {
			flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
		} else if (then.yearDay != now.yearDay) {
			flags |= DateUtils.FORMAT_SHOW_DATE;
		} else {
			flags |= DateUtils.FORMAT_SHOW_TIME;
		}

		return DateUtils.formatDateTime(context, when, flags);
	}

    public static long getDirectorySize(File dir) {
		long result = 0L;
		if (dir.listFiles() != null && dir.listFiles().length > 0) {
			for (File eachFile : dir.listFiles()) {
				result += eachFile.isDirectory() && eachFile.canRead() ? getDirectorySize(eachFile) : eachFile.length();
			}
		} else if (!dir.isDirectory()) {
			result = dir.length();
		}
		return result;
	}

    public static boolean hasSoftNavBar(Context context){
        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        return !hasMenuKey && !hasBackKey;

    }

    private static final int BRIGHTNESS_THRESHOLD = 150;
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }

    public static final int PRESSED_COLOR_LIGHTUP = 255 / 25;
    public static int getLightColor(int color, int amount) {
        return Color.argb(Math.min(255, Color.alpha(color)), Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount), Math.min(255, Color.blue(color) + amount));
    }

    public static int getLightColor(int color) {
        int amount = PRESSED_COLOR_LIGHTUP;
        return Color.argb(Math.min(255, Color.alpha(color)), Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount), Math.min(255, Color.blue(color) + amount));
    }

    public static int getStatusBarColor(int color1) {
        int color2 = Color.parseColor("#000000");
        return blendColors(color1, color2, 0.9f);
    }

    public static int getActionButtonColor(int color1) {
        int color2 = Color.parseColor("#ffffff");
        return blendColors(color1, color2, 0.9f);
    }

    public static int blendColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    public static int getComplementaryColor(int colorToInvert) {
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(colorToInvert), Color.green(colorToInvert),
                Color.blue(colorToInvert), hsv);
        hsv[0] = (hsv[0] + 180) % 360;
        return Color.HSVToColor(hsv);
    }


    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isActivityAlive(Activity activity) {
        return !(null == activity
                || (null != activity && Utils.hasJellyBeanMR1() ? activity.isDestroyed() : activity.isFinishing()));
    }

    public static boolean isAPK(String mimeType){
        return MimePredicate.mimeMatches(DocumentsContract.Document.MIME_TYPE_APK, mimeType);
    }

    public static boolean isDir(String mimeType){
        return MimePredicate.mimeMatches(DocumentsContract.Document.MIME_TYPE_DIR, mimeType);
    }

    public static boolean isProVersion(){
        return BuildConfig.FLAVOR.contains("Pro") || BuildConfig.FLAVOR.contains("Underground");
    }

    public static boolean isOtherBuild(){
        return BuildConfig.FLAVOR.contains("other");
    }

    public static boolean isGoogleBuild(){
        return BuildConfig.FLAVOR.contains("google");
    }

    public static boolean isAmazonBuild(){
        return BuildConfig.FLAVOR.contains("amazon");
    }

    public static Uri getAppUri(){
        if(isAmazonBuild()){
            return Uri.parse("amzn://apps/android?p=" + BuildConfig.APPLICATION_ID);
        }

        return Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID);
    }

    public static Uri getAppShareUri(){
        if(isAmazonBuild()){
            return Uri.parse("https://www.amazon.com/gp/mas/dl/android?p=" + BuildConfig.APPLICATION_ID);
        }

        return Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
    }

    public static Uri getAppStoreUri(){
        if(isAmazonBuild()){
            return Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + BuildConfig.APPLICATION_ID + "&showAll=1");
        }
        return Uri.parse("https://play.google.com/store/apps/dev?id=8683545855643814241");
    }

    public static boolean hasFeature(Context context, String feature) {
        return context.getPackageManager().hasSystemFeature(feature);
    }

    public static boolean hasLeanback(Context context) {
        return hasFeature(context, PackageManager.FEATURE_LEANBACK);
    }

    public static boolean hasWiFi(Context context) {
        return hasFeature(context, PackageManager.FEATURE_WIFI);
    }

    public static boolean isTelevision(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    public static void showRetrySnackBar(View view, String text, View.OnClickListener listener){
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
        if (null != listener) {
            snackbar.setAction("RETRY", listener)
                    .setActionTextColor(SettingsActivity.getActionBarColor(view.getContext()));
        }
        snackbar.show();
    }

    public static void showRetrySnackBar(Activity activity, String text, View.OnClickListener listener){
        Snackbar snackbar = Snackbar.make(activity.findViewById(R.id.content_view), text, Snackbar.LENGTH_INDEFINITE);
        if (null != listener) {
            snackbar.setAction("RETRY", listener)
                    .setActionTextColor(SettingsActivity.getActionBarColor(activity));
        }
        snackbar.show();
    }

    public static Bitmap getVector2Bitmap(Context context, int id) {
        VectorDrawableCompat vectorDrawable = VectorDrawableCompat.create(context.getResources(), id, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    public static int dpToPx(int dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_MEDIUM);
        return Math.round(px);
    }

    public static void changeThemeStyle(AppCompatDelegate delegate) {
        int nightMode = Integer.valueOf(SettingsActivity.getThemeStyle());
        AppCompatDelegate.setDefaultNightMode(nightMode);
        delegate.setLocalNightMode(nightMode);
    }

    public static void tintWidget(View view) {
        tintWidget(view, SettingsActivity.getActionBarColor(view.getContext()));
    }

    public static void tintButton(Button view) {
        view.setTextColor(SettingsActivity.getActionBarColor(view.getContext()));
    }

    public static void tintWidget(View view, int color) {
        Drawable wrappedDrawable = DrawableCompat.wrap(view.getBackground());
        DrawableCompat.setTint(wrappedDrawable.mutate(), color);
        view.setBackgroundDrawable(wrappedDrawable);
    }

    public static boolean isRTL() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == android.support.v4.view.ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public static File getAppsBackupFile(Context context){
        final RootInfo root = DocumentsApplication.getRootsCache(context).getPrimaryRoot();
        File rootFile = (null != root) ? new File(root.path) : Environment.getExternalStorageDirectory();
        return new File(rootFile, DIRECTORY_APPBACKUP);
    }

    public static float dp2px(Resources resources, float dp) {
        final float scale = resources.getDisplayMetrics().density;
        return  dp * scale + 0.5f;
    }

    public static float sp2px(Resources resources, float sp){
        final float scale = resources.getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

    /**
     * Returns true when running Android TV
     *
     * @param c Context to detect UI Mode.
     * @return true when device is running in tv mode, false otherwise.
     */
    public static String getDeviceType(Context c) {
        UiModeManager uiModeManager = (UiModeManager) c.getSystemService(Context.UI_MODE_SERVICE);
        int modeType = uiModeManager.getCurrentModeType();
        switch (modeType){
            case Configuration.UI_MODE_TYPE_TELEVISION:
                return "TELEVISION";
            case Configuration.UI_MODE_TYPE_WATCH:
                return "WATCH";
            case Configuration.UI_MODE_TYPE_NORMAL:
                String type = isTablet(c) ? "TABLET" : "PHONE";
                return type;
            case Configuration.UI_MODE_TYPE_UNDEFINED:
                return "UNKOWN";
            default:
                return "";
        }
    }

    public static ColorStateList getColorStateList(Context context, int colorRes) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_pressed}  // pressed
        };

        int color = ContextCompat.getColor(context, colorRes);

        int[] colors = new int[]{color, color, color, color};
        return new ColorStateList(states, colors);
    }

    public static boolean isQSTile(Intent intent){
        if(null != intent.getAction()){
            String action = intent.getAction();
            return ACTION_QS_TILE_PREFERENCES.equals(action);
        }
        return false;
    }

    public static String getSuffix(){
        return Utils.isProVersion() ? " Pro" : ""
                + (DocumentsApplication.isTelevision()? " for Android TV" : "");
    }

    public static void openFeedback(Activity activity){
        ShareCompat.IntentBuilder
                .from(activity)
                .setEmailTo(new String[]{"hakr@dworks.in"})
                .setSubject("AnExplorer Feedback" + getSuffix())
                .setType("text/email")
                .setChooserTitle("Send Feedback")
                .startChooser();
    }

    public static void openPlaystore(Context çontext){
        Intent intent = new Intent(Intent.ACTION_VIEW, Utils.getAppUri());
        if(Utils.isIntentAvailable(çontext, intent)) {
            çontext.startActivity(intent);
        }
    }
}