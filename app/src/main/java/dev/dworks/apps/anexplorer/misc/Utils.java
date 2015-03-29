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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.List;
import java.util.Locale;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

public class Utils {

    public static final long KB_IN_BYTES = 1024;

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

    public static class Preconditions {

	    /**
	     * Ensures that an object reference passed as a parameter to the calling
	     * method is not null.
	     *
	     * @param reference an object reference
	     * @return the non-null reference that was validated
	     * @throws NullPointerException if {@code reference} is null
	     */
	    public static <T> T checkNotNull(T reference) {
	        if (reference == null) {
	            throw new NullPointerException();
	        }
	        return reference;
	    }

	    /**
	     * Ensures that an object reference passed as a parameter to the calling
	     * method is not null.
	     *
	     * @param reference an object reference
	     * @param errorMessage the exception message to use if the check fails; will
	     *     be converted to a string using {@link String#valueOf(Object)}
	     * @return the non-null reference that was validated
	     * @throws NullPointerException if {@code reference} is null
	     */
	    public static <T> T checkNotNull(T reference, Object errorMessage) {
	        if (reference == null) {
	            throw new NullPointerException(String.valueOf(errorMessage));
	        }
	        return reference;
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

        if(!hasMenuKey && !hasBackKey) {
            return true;
        }
        return false;

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
        if (null == activity
                || (null != activity && Utils.hasJellyBeanMR1() ? activity.isDestroyed() : activity.isFinishing())) {
            return false;
        }
        return true;
    }

    public static boolean isAPK(String mimeType){
        return MimePredicate.mimeMatches(DocumentsContract.Document.MIME_TYPE_APK, mimeType);
    }

    public static boolean isDir(String mimeType){
        return MimePredicate.mimeMatches(DocumentsContract.Document.MIME_TYPE_DIR, mimeType);
    }

    public static boolean isProVersion(){
        return BuildConfig.FLAVOR.contains("Pro");
    }
}