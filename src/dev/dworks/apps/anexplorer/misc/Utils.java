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

import java.io.File;
import java.util.Locale;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.format.DateUtils;
import android.text.format.Time;
import dev.dworks.apps.anexplorer.R;

public class Utils {

    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    public static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

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
    
    public static boolean hasMoreHeap(){
    	return Runtime.getRuntime().maxMemory() > 20971520;
    }
    
    public static boolean isLowRamDevice(Context context) {
    	if(Utils.hasKitKat()){
    		final ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    		return am.isLowRamDevice();
    	}
    	return !hasMoreHeap();
	}

    public static boolean isTablet(Context context) {
		return context.getResources().getBoolean(R.bool.show_as_dialog);//().smallestScreenWidthDp >= 720;
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
            } else {
            }
        }
        return false;
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
    
    private static final int BRIGHTNESS_THRESHOLD = 150;
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }
}