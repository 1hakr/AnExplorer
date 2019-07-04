/*
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

package dev.dworks.apps.anexplorer.misc;

import android.annotation.TargetApi;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dev.dworks.apps.anexplorer.BaseActivity.State;
import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.cloud.CloudConnection;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.libcore.io.MultiMap;
import dev.dworks.apps.anexplorer.libcore.util.Objects;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.DocumentsContract.Root;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.network.NetworkConnection;
import dev.dworks.apps.anexplorer.provider.AppsProvider;
import dev.dworks.apps.anexplorer.provider.CloudStorageProvider;
import dev.dworks.apps.anexplorer.provider.ContentProvider;
import dev.dworks.apps.anexplorer.provider.DocumentsProvider;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;
import dev.dworks.apps.anexplorer.provider.ExtraDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.MediaDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.NetworkStorageProvider;
import dev.dworks.apps.anexplorer.provider.RecentsProvider;
import dev.dworks.apps.anexplorer.provider.RootedStorageProvider;
import dev.dworks.apps.anexplorer.provider.UsbStorageProvider;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.fragment.HomeFragment.ROOTS_CHANGED;

/**
 * Cache of known storage backends and their roots.
 */
public class RootsCache {
    private static final boolean LOGD = true;
    public static final String TAG = "RootsCache";

    public static final Uri sNotificationUri = Uri.parse(
            "content://"+ BuildConfig.APPLICATION_ID+".roots/");

    private final Context mContext;
    private final ContentObserver mObserver;

    private final RootInfo mHomeRoot = new RootInfo();
    private final RootInfo mConnectionsRoot = new RootInfo();
    private final RootInfo mRecentsRoot = new RootInfo();
    private final RootInfo mTransferRoot = new RootInfo();
    private final RootInfo mCastRoot = new RootInfo();

    private final Object mLock = new Object();
    private final CountDownLatch mFirstLoad = new CountDownLatch(1);

    @GuardedBy("mLock")
    private MultiMap<String, RootInfo> mRoots = new MultiMap<>();
    @GuardedBy("mLock")
    private ArraySet<String> mStoppedAuthorities = new ArraySet<>();

    @GuardedBy("mObservedAuthorities")
    private final ArraySet<String> mObservedAuthorities = new ArraySet<>();

    public RootsCache(Context context) {
        mContext = context;
        mObserver = new RootsChangedObserver();
    }

    private class RootsChangedObserver extends ContentObserver {
        public RootsChangedObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                Log.w(TAG, "Received onChange event for null uri. Skipping.");
                return;
            }
            if (LOGD) Log.d(TAG, "Updating roots due to change at " + uri);
            updateAuthorityAsync(uri.getAuthority());
        }
    }

    /**
     * Gather roots from all known storage providers.
     */
    public void updateAsync() {
        // Special root for home
        mHomeRoot.authority = null;
        mHomeRoot.rootId = "home";
        mHomeRoot.icon = R.drawable.ic_root_home;
        mHomeRoot.flags = Root.FLAG_LOCAL_ONLY;
        mHomeRoot.title = mContext.getString(R.string.root_home);
        mHomeRoot.availableBytes = -1;
        mHomeRoot.deriveFields();

        // Special root for web host
        mConnectionsRoot.authority = null;
        mConnectionsRoot.rootId = "connections";
        mConnectionsRoot.icon = R.drawable.ic_root_connections;
        mConnectionsRoot.flags = Root.FLAG_LOCAL_ONLY;
        mConnectionsRoot.title = mContext.getString(R.string.root_connections);
        mConnectionsRoot.availableBytes = -1;
        mConnectionsRoot.deriveFields();

        // Special root for recents
        mRecentsRoot.authority = RecentsProvider.AUTHORITY;
        mRecentsRoot.rootId = "recents";
        mRecentsRoot.icon = R.drawable.ic_root_recent;
        mRecentsRoot.flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_IS_CHILD;
        mRecentsRoot.title = mContext.getString(R.string.root_recent);
        mRecentsRoot.availableBytes = -1;
        mRecentsRoot.deriveFields();

        // Special root for file transfer
        mTransferRoot.authority = TransferHelper.AUTHORITY;
        mTransferRoot.rootId = "transfer";
        mTransferRoot.icon = R.drawable.ic_root_transfer;
        mTransferRoot.flags = Root.FLAG_LOCAL_ONLY;
        mTransferRoot.title = mContext.getString(R.string.root_transfer);
        mTransferRoot.availableBytes = -1;
        mTransferRoot.deriveFields();

        // Special root for cast queue
        mCastRoot.authority = null;
        mCastRoot.rootId = "cast";
        mCastRoot.icon = R.drawable.ic_root_cast;
        mCastRoot.flags = Root.FLAG_LOCAL_ONLY;
        mCastRoot.title = mContext.getString(R.string.root_cast);
        mCastRoot.availableBytes = -1;
        mCastRoot.deriveFields();

        new UpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Gather roots from storage providers belonging to given authority.
     */
    public void updateAuthorityAsync(String authority) {
        new UpdateTask(authority).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void waitForFirstLoad() {
        boolean success = false;
        try {
            success = mFirstLoad.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!success) {
            Log.w(TAG, "Timeout waiting for first update");
        }
    }

    /**
     * Load roots from authorities that are in stopped state. Normal
     * {@link UpdateTask} passes ignore stopped applications.
     */
    private void loadStoppedAuthorities() {
        final ContentResolver resolver = mContext.getContentResolver();
        synchronized (mLock) {
            for (String authority : mStoppedAuthorities) {
                if (LOGD) Log.d(TAG, "Loading stopped authority " + authority);
                mRoots.putAll(authority, loadRootsForAuthority(resolver, authority));
            }
            mStoppedAuthorities.clear();
        }
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        private final String mAuthority;

        private final MultiMap<String, RootInfo> mTaskRoots = new MultiMap<>();
        private final ArraySet<String> mTaskStoppedAuthorities = new ArraySet<>();

        /**
         * Update all roots.
         */
        public UpdateTask() {
            this(null);
        }

        /**
         * Only update roots belonging to given authority. Other roots will
         * be copied from cached {@link #mRoots} values.
         */
        public UpdateTask(String authority) {
            mAuthority = authority;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... params) {
            final long start = SystemClock.elapsedRealtime();

            if (mAuthority != null) {
                // Need at least first load, since we're going to be using
                // previously cached values for non-matching packages.
                waitForFirstLoad();
            }

            mTaskRoots.put(mHomeRoot.authority, mHomeRoot);
            mTaskRoots.put(mConnectionsRoot.authority, mConnectionsRoot);
            mTaskRoots.put(mTransferRoot.authority, mTransferRoot);
            mTaskRoots.put(mCastRoot.authority, mCastRoot);
            mTaskRoots.put(mRecentsRoot.authority, mRecentsRoot);

            final ContentResolver resolver = mContext.getContentResolver();
            final PackageManager pm = mContext.getPackageManager();

            // Pick up provider with action string
            if(Utils.hasKitKat()){
                final Intent intent = new Intent(DocumentsContract.PROVIDER_INTERFACE);
                final List<ResolveInfo> providers = pm.queryIntentContentProviders(intent, 0);
                for (ResolveInfo info : providers) {
                    handleDocumentsProvider(info.providerInfo);
                }
            }
            else{
                List<ProviderInfo> providers = pm.queryContentProviders(mContext.getPackageName(),
                        mContext.getApplicationInfo().uid, 0);
                for (ProviderInfo providerInfo : providers) {
                    handleDocumentsProvider(providerInfo);
                }
            }
            final long delta = SystemClock.elapsedRealtime() - start;
            Log.d(TAG, "Update found " + mTaskRoots.size() + " roots in " + delta + "ms");
            synchronized (mLock) {
                mRoots = mTaskRoots;
                mStoppedAuthorities = mTaskStoppedAuthorities;
            }
            mFirstLoad.countDown();
            resolver.notifyChange(sNotificationUri, null, false);
            return null;
        }

        private void handleDocumentsProvider(ProviderInfo info) {
            // Ignore stopped packages for now; we might query them
            // later during UI interaction.
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0) {
                if (LOGD) Log.d(TAG, "Ignoring stopped authority " + info.authority);
                mTaskStoppedAuthorities.add(info.authority);
                return;
            }

            // Try using cached roots if filtering
            boolean cacheHit = false;
            if (mAuthority != null && !mAuthority.equals(info.authority)) {
                synchronized (mLock) {
                    if (mTaskRoots.putAll(info.authority, mRoots.get(info.authority))) {
                        if (LOGD) Log.d(TAG, "Used cached roots for " + info.authority);
                        cacheHit = true;
                    }
                }
            }

            // Cache miss, or loading everything
            if (!cacheHit) {
                mTaskRoots.putAll(info.authority,
                        loadRootsForAuthority(mContext.getContentResolver(), info.authority));
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mContext.sendBroadcast(new Intent(ROOTS_CHANGED));
        }
    }

    /**
     * Bring up requested provider and query for all active roots.
     */
    private Collection<RootInfo> loadRootsForAuthority(ContentResolver resolver, String authority) {
        if (LOGD) Log.d(TAG, "Loading roots for " + authority);

        synchronized (mObservedAuthorities) {
            if (mObservedAuthorities.add(authority)) {
                // Watch for any future updates
                final Uri rootsUri = DocumentsContract.buildRootsUri(authority);
                try {
                    mContext.getContentResolver().registerContentObserver(rootsUri, true, mObserver);
                } catch (Exception e) {
                    CrashReportingManager.logException(e, true);
                }
            }
        }

        final List<RootInfo> roots = new ArrayList<>();
        final Uri rootsUri = DocumentsContract.buildRootsUri(authority);

        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, authority);
            cursor = client.query(rootsUri, null, null, null, null);
            while (cursor.moveToNext()) {
                final RootInfo root = RootInfo.fromRootsCursor(authority, cursor);
                roots.add(root);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load some roots from " + authority + ": " + e);
        } finally {
            IoUtils.closeQuietly(cursor);
            ContentProviderClientCompat.releaseQuietly(client);
        }
        return roots;
    }
    
    /**
     * Return the requested {@link RootInfo}, but only loading the roots for the
     * requested authority. This is useful when we want to load fast without
     * waiting for all the other roots to come back.
     */
    public RootInfo getRootOneshot(String authority, String rootId) {
        synchronized (mLock) {
            RootInfo root = getRootLocked(authority, rootId);
            if (root == null) {
                mRoots.putAll(
                        authority, loadRootsForAuthority(mContext.getContentResolver(), authority));
                root = getRootLocked(authority, rootId);
            }
            return root;
        }
    }

    public RootInfo getRootBlocking(String authority, String rootId) {
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (mLock) {
            return getRootLocked(authority, rootId);
        }
    }

    private RootInfo getRootLocked(String authority, String rootId) {
        for (RootInfo root : mRoots.get(authority)) {
            if (Objects.equal(root.rootId, rootId)) {
                return root;
            }
        }
        return null;
    }

    public boolean isIconUniqueBlocking(RootInfo root) {
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (mLock) {
            final int rootIcon = root.derivedIcon != 0 ? root.derivedIcon : root.icon;
            for (RootInfo test : mRoots.get(root.authority)) {
                if (Objects.equal(test.rootId, root.rootId)) {
                    continue;
                }
                final int testIcon = test.derivedIcon != 0 ? test.derivedIcon : test.icon;
                if (testIcon == rootIcon) {
                    return false;
                }
            }
            return true;
        }
    }

    public RootInfo getOneRoot() {
        for (RootInfo root : mRoots.get(ExternalStorageProvider.AUTHORITY)) {
            return root;
        }
        return getDefaultRoot();
    }

    public RootInfo getDefaultRoot() {
        return getHomeRoot();
    }

    public RootInfo getDownloadRoot() {
        for (RootInfo root : mRoots.get(ExternalStorageProvider.AUTHORITY)) {
            if (root.isDownloads() || root.isDownloadsFolder()) {
                return root;
            }
        }
        return getDefaultRoot();
    }

    public RootInfo getStorageRoot() {
        RootInfo rootInfo =  getPrimaryRoot();
        if(null != rootInfo){
            return rootInfo;
        } else {
            return getSecondaryRoot();
        }
    }

    public RootInfo getPrimaryRoot() {
        for (RootInfo root : mRoots.get(ExternalStorageProvider.AUTHORITY)) {
            if (root.isStorage() && !root.isSecondaryStorage()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getSecondaryRoot() {
        for (RootInfo root : mRoots.get(ExternalStorageProvider.AUTHORITY)) {
            if (root.isSecondaryStorage()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getUSBRoot() {
        for (RootInfo root : mRoots.get(UsbStorageProvider.AUTHORITY)) {
            if (root.isUsbStorage()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getDeviceRoot() {
        for (RootInfo root : mRoots.get(ExternalStorageProvider.AUTHORITY)) {
            if (root.isPhoneStorage()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getProcessRoot() {
        for (RootInfo root : mRoots.get(AppsProvider.AUTHORITY)) {
            if (root.isAppProcess()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getAppRoot() {
        for (RootInfo root : mRoots.get(AppsProvider.AUTHORITY)) {
            if (root.isAppPackage()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getServerRoot() {
        for (RootInfo root : mRoots.get(NetworkStorageProvider.AUTHORITY)) {
            if (root.isServer()) {
                return root;
            }
        }
        return null;
    }

    public RootInfo getAppsBackupRoot() {
        for (RootInfo root : mRoots.get(ExternalStorageProvider.AUTHORITY)) {
            if (root.isAppBackupFolder()) {
                return root;
            }
        }
        return getPrimaryRoot();
    }

    public RootInfo getRootInfo(String rootId, String authority){
        for (RootInfo root : mRoots.get(authority)) {
            if (root.rootId.equals(rootId)) {
                return root;
            }
        }

        return null;
    }

    public RootInfo getRootInfo(NetworkConnection connection){
        for (RootInfo root : mRoots.get(NetworkStorageProvider.AUTHORITY)) {
            if (root.rootId.equals(connection.getHost())
                    && root.path.equals(connection.getPath())) {
                return root;
            }
        }

        return null;
    }

    public RootInfo getRootInfo(CloudConnection connection){
        for (RootInfo root : mRoots.get(CloudStorageProvider.AUTHORITY)) {
            if (root.rootId.equals(connection.clientId)
                    && root.path.equals(connection.getPath())) {
                return root;
            }
        }

        return null;
    }

    public RootInfo getRootInfo(String host, String path, String authority){
        for (RootInfo root : mRoots.get(authority)) {
            if (root.rootId.equals(host)
                    && root.path.equals(path)) {
                return root;
            }
        }

        return null;
    }

    public ArrayList<RootInfo> getShortcutsInfo(){
        ArrayList<RootInfo> list = new ArrayList<>();
        if(Utils.hasWiFi(mContext)) {
            list.add(getServerRoot());
            list.add(getTransferRoot());
        }
        if(!isSpecialDevice()) {
            list.add(getCastRoot());
        }
        list.add(getAppRoot());
        for (RootInfo root : mRoots.get(MediaDocumentsProvider.AUTHORITY)) {
            final boolean empty = (root.flags & DocumentsContract.Root.FLAG_EMPTY) != 0;
            if (RootInfo.isLibraryMedia(root) && !empty) {
                list.add(root);
            }
        }
        for (RootInfo root : mRoots.get(ExtraDocumentsProvider.AUTHORITY)) {
            final boolean empty = (root.flags & DocumentsContract.Root.FLAG_EMPTY) != 0;
            if (!empty) {
                list.add(root);
            }
        }
        return list;
    }

    public RootInfo getHomeRoot() {
        return mHomeRoot;
    }

    public RootInfo getRecentsRoot() {
        return mRecentsRoot;
    }

    public RootInfo getConnectionsRoot() {
        return mConnectionsRoot;
    }

    public RootInfo getTransferRoot() {
        return mTransferRoot;
    }

    public RootInfo getCastRoot() {
        return mCastRoot;
    }

    public boolean isHomeRoot(RootInfo root) {
        return mHomeRoot == root;
    }

    public boolean isRecentsRoot(RootInfo root) {
        return mRecentsRoot == root;
    }

    public Collection<RootInfo> getRootsBlocking() {
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (mLock) {
            return mRoots.values();
        }
    }

    public Collection<RootInfo> getMatchingRootsBlocking(State state) {
        waitForFirstLoad();
        loadStoppedAuthorities();
        synchronized (mLock) {
            return getMatchingRoots(mRoots.values(), state);
        }
    }

    static List<RootInfo> getMatchingRoots(Collection<RootInfo> roots, State state) {
        final List<RootInfo> matching = new ArrayList<>();
        for (RootInfo root : roots) {
            final boolean supportsCreate = (root.flags & Root.FLAG_SUPPORTS_CREATE) != 0;
            final boolean supportsIsChild = (root.flags & Root.FLAG_SUPPORTS_IS_CHILD) != 0;
            final boolean advanced = (root.flags & DocumentsContract.Root.FLAG_ADVANCED) != 0;
            final boolean superAdvanced = (root.flags & Root.FLAG_SUPER_ADVANCED) != 0;
            final boolean localOnly = (root.flags & Root.FLAG_LOCAL_ONLY) != 0;
            final boolean empty = (root.flags & DocumentsContract.Root.FLAG_EMPTY) != 0;

            if(null != root.authority
            		&& root.authority.equals(RootedStorageProvider.AUTHORITY)){
            	if(state.action != State.ACTION_BROWSE || !state.rootMode){
            		continue;
            	}
            }
            // Exclude read-only devices when creating
            if (state.action == State.ACTION_CREATE && !supportsCreate) continue;
            // Exclude roots that don't support directory picking
            if (state.action == State.ACTION_OPEN_TREE && !supportsIsChild) continue;
            // Exclude advanced devices when not requested
            if (!state.showAdvanced && advanced) continue;
            // Exclude non-local devices when local only
            if (state.localOnly && !localOnly) continue;
            // Only show empty roots when creating
            if (state.action != State.ACTION_CREATE && empty) continue;

            if ((state.action == State.ACTION_GET_CONTENT
                    || state.action == State.ACTION_GET_CONTENT || state.action == State.ACTION_OPEN
                    || state.action == State.ACTION_OPEN_TREE) && superAdvanced){
                continue;
            }
            // Only include roots that serve requested content
            final boolean overlap =
                    MimePredicate.mimeMatches(root.derivedMimeTypes, state.acceptMimes) ||
                    MimePredicate.mimeMatches(state.acceptMimes, root.derivedMimeTypes);
            if (!overlap) {
                continue;
            }

            matching.add(root);
        }
        return matching;
    }

    public static void updateRoots(Context context, String authority){
        final ContentProviderClient client =
                ContentProviderClientCompat.acquireUnstableContentProviderClient(
                        context.getContentResolver(), authority);
        try {
            DocumentsProvider provider = ((DocumentsProvider) client.getLocalContentProvider());
            if (null == provider){
                return;
            }
            provider.updateRoots();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            ContentProviderClientCompat.releaseQuietly(client);
        }
    }

    public static void updateRoots(Context context){
        MultiMap<String, RootInfo> roots = DocumentsApplication.getRootsCache(context).mRoots;
        for (RootInfo root : roots.values()) {
            String authority = root.authority;
            if (!TextUtils.isEmpty(authority)) {
                updateRoots(context, authority);
            }
        }
    }
}
