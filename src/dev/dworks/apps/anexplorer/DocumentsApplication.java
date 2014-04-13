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

package dev.dworks.apps.anexplorer;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.DateUtils;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.ThumbnailCache;

public class DocumentsApplication extends Application {
	private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;

    private RootsCache mRoots;
    private Point mThumbnailsSize;
    private ThumbnailCache mThumbnails;
    public static Tracker tracker;

    public static String APP_VERSION;
    public static int APP_VERSION_CODE;
    
    public static RootsCache getRootsCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mRoots;
    }

    public static ThumbnailCache getThumbnailsCache(Context context, Point size) {
        final DocumentsApplication app = (DocumentsApplication) context.getApplicationContext();
        final ThumbnailCache thumbnails = app.mThumbnails;
        if (!size.equals(app.mThumbnailsSize)) {
            thumbnails.evictAll();
            app.mThumbnailsSize = size;
        }
        return thumbnails;
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
        final ContentProviderClient client = resolver.acquireContentProviderClient(
                authority);
        if (client == null) {
            throw new RemoteException();//"Failed to acquire provider for " + authority);
        }
        ContentProviderClientCompat.setDetectNotResponding(client, PROVIDER_ANR_TIMEOUT);
        return client;
    }

    @Override
    public void onCreate() {
    	
		GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(this);
		googleAnalytics.getTracker(getString(R.string.ga_trackingId));
		tracker = googleAnalytics.getDefaultTracker();
		
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;

        mRoots = new RootsCache(this);
        mRoots.updateAsync();

        mThumbnails = new ThumbnailCache(memoryClassBytes / 4);

        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        packageFilter.addDataScheme("package");
        registerReceiver(mCacheReceiver, packageFilter);

        final IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mCacheReceiver, localeFilter);
        
    	try {
            final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
    		APP_VERSION = info.versionName;
    		APP_VERSION_CODE = info.versionCode;
		} catch (NameNotFoundException e) {
			APP_VERSION = "Unknown";
			APP_VERSION_CODE = 0;
			e.printStackTrace();
		}
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_MODERATE) {
            mThumbnails.evictAll();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            mThumbnails.trimToSize(mThumbnails.size() / 2);
        }
    }

    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri data = intent.getData();
            if (data != null) {
                final String packageName = data.getSchemeSpecificPart();
                mRoots.updatePackageAsync(packageName);
            } else {
                mRoots.updateAsync();
            }
        }
    };
}
