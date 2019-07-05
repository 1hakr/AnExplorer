/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.MediaQueue;

import java.util.List;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import dev.dworks.apps.anexplorer.cast.Casty;
import dev.dworks.apps.anexplorer.common.ActionBarActivity;
import dev.dworks.apps.anexplorer.misc.PermissionUtil;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentStack;
import dev.dworks.apps.anexplorer.model.DurableUtils;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.ExternalStorageProvider;
import dev.dworks.apps.anexplorer.server.WebServer;

import static dev.dworks.apps.anexplorer.DocumentsApplication.isSpecialDevice;
import static dev.dworks.apps.anexplorer.DocumentsApplication.isWatch;
import static dev.dworks.apps.anexplorer.model.RootInfo.openRoot;

public abstract class BaseActivity extends ActionBarActivity {
    public static final String TAG = "Documents";
    private RootsCache mRoots;

    public abstract State getDisplayState();
    public abstract RootInfo getCurrentRoot();
    public abstract void onStateChanged();
    public abstract void setRootsDrawerOpen(boolean open);
    public abstract void onDocumentPicked(DocumentInfo doc);
    public abstract void onDocumentsPicked(List<DocumentInfo> docs);
    public abstract DocumentInfo getCurrentDirectory();
    public abstract void setPending(boolean pending);
    public abstract void onStackPicked(DocumentStack stack);
    public abstract void onPickRequested(DocumentInfo pickTarget);
    public abstract void onAppPicked(ResolveInfo info);
    public abstract void onRootPicked(RootInfo root, boolean closeDrawer);
    public abstract void onSaveRequested(DocumentInfo replaceTarget);
    public abstract void onSaveRequested(String mimeType, String displayName);

    public abstract boolean isCreateSupported();
    public abstract RootInfo getDownloadRoot();
    public abstract boolean getActionMode();
    public abstract void setActionMode(boolean actionMode);
    public abstract void setUpStatusBar();
    public abstract void setUpDefaultStatusBar();


    public abstract boolean isShowAsDialog();
    public abstract void upadateActionItems(RecyclerView mCurrentView);
    public abstract void setInfoDrawerOpen(boolean open);
    public abstract void again();

    public static BaseActivity get(Fragment fragment) {
        return (BaseActivity) fragment.getActivity();
    }

    public static class State implements android.os.Parcelable {
        public int action;
        public String[] acceptMimes;

        /** Explicit user choice */
        public int userMode = MODE_UNKNOWN;
        /** Derived after loader */
        public int derivedMode = MODE_LIST;

        /** Explicit user choice */
        public int userSortOrder = SORT_ORDER_UNKNOWN;
        /** Derived after loader */
        public int derivedSortOrder = SORT_ORDER_DISPLAY_NAME;

        public boolean allowMultiple = false;
        public boolean showSize = false;
        public boolean showFolderSize = false;
        public boolean showThumbnail = false;
        public boolean showHiddenFiles = false;
        public boolean localOnly = false;
        public boolean forceAdvanced = false;
        public boolean showAdvanced = false;
        public boolean rootMode = false;
        public boolean stackTouched = false;
        public boolean restored = false;

        /** Current user navigation stack; empty implies recents. */
        public DocumentStack stack = new DocumentStack();
        /** Currently active search, overriding any stack. */
        public String currentSearch;

        /** Instance state for every shown directory */
        public ArrayMap<String, SparseArray<Parcelable>> dirState = new ArrayMap<>();

        public static final int ACTION_OPEN = 1;
        public static final int ACTION_CREATE = 2;
        public static final int ACTION_GET_CONTENT = 3;
        public static final int ACTION_OPEN_TREE = 4;
        public static final int ACTION_MANAGE = 5;
        public static final int ACTION_BROWSE = 6;
        public static final int ACTION_MANAGE_ALL = 7;

        public @interface ViewMode {}
        public static final int MODE_UNKNOWN = 0;
        public static final int MODE_LIST = 1;
        public static final int MODE_GRID = 2;

        public static final int SORT_ORDER_UNKNOWN = 0;
        public static final int SORT_ORDER_DISPLAY_NAME = 1;
        public static final int SORT_ORDER_LAST_MODIFIED = 2;
        public static final int SORT_ORDER_SIZE = 3;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(action);
            out.writeInt(userMode);
            out.writeInt(acceptMimes.length);
            out.writeStringArray(acceptMimes);
            out.writeInt(userSortOrder);
            out.writeInt(allowMultiple ? 1 : 0);
            out.writeInt(showSize ? 1 : 0);
            out.writeInt(showFolderSize ? 1 : 0);
            out.writeInt(showThumbnail ? 1 : 0);
            out.writeInt(showHiddenFiles ? 1 : 0);
            out.writeInt(localOnly ? 1 : 0);
            out.writeInt(forceAdvanced ? 1 : 0);
            out.writeInt(showAdvanced ? 1 : 0);
            out.writeInt(rootMode ? 1 : 0);
            out.writeInt(stackTouched ? 1 : 0);
            out.writeInt(restored ? 1 : 0);
            DurableUtils.writeToParcel(out, stack);
            out.writeString(currentSearch);
            out.writeMap(dirState);
        }

        public static final ClassLoaderCreator<State> CREATOR = new ClassLoaderCreator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                return createFromParcel(in, null);
            }

            @Override
            public State createFromParcel(Parcel in, ClassLoader loader) {
                final State state = new State();
                state.action = in.readInt();
                state.userMode = in.readInt();
                state.acceptMimes = new String[in.readInt()];
                in.readStringArray(state.acceptMimes);
                state.userSortOrder = in.readInt();
                state.allowMultiple = in.readInt() != 0;
                state.showSize = in.readInt() != 0;
                state.showFolderSize = in.readInt() != 0;
                state.showThumbnail = in.readInt() != 0;
                state.showHiddenFiles = in.readInt() != 0;
                state.localOnly = in.readInt() != 0;
                state.forceAdvanced = in.readInt() != 0;
                state.showAdvanced = in.readInt() != 0;
                state.rootMode = in.readInt() != 0;
                state.stackTouched = in.readInt() != 0;
                state.restored = in.readInt() != 0;
                DurableUtils.readFromParcel(in, state.stack);
                state.currentSearch = in.readString();
                in.readMap(state.dirState, loader);
                return state;
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }

    public boolean isSAFIssue(String docId){
        boolean isSAFIssue = Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT
                && !TextUtils.isEmpty(docId) && docId.startsWith(ExternalStorageProvider.ROOT_ID_SECONDARY);

        if(isSAFIssue){
            Utils.showError(this, R.string.saf_issue);
        }
        return isSAFIssue;
    }

    private static String[] storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final int REQUEST_STORAGE = 47;

    protected void requestStoragePermissions() {
        if(PermissionUtil.hasStoragePermission(this)) {
            again();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Utils.showPermanentRetrySnackBar(this, "Storage permissions are needed for Exploring.", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(BaseActivity.this, storagePermissions, REQUEST_STORAGE);
                    }
                });
            } else {
                ActivityCompat.requestPermissions(this, storagePermissions, REQUEST_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    again();
                } else {
                    Utils.showRetrySnackBar(this, "Permission grating failed", null);
                    requestStoragePermissions();
                }
                return;
            }
        }
    }

    private Casty casty;
    protected CastSession castSession;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isWatch()){
            return;
        }
        DocumentsApplication.getInstance().initCasty(this);
        casty = DocumentsApplication.getInstance().getCasty();
        casty.setOnConnectChangeListener(new Casty.OnConnectChangeListener() {
            @Override
            public void onConnected() {
                castSession = casty.getCastSession();
                supportInvalidateOptionsMenu();
                WebServer.getServer().startServer(BaseActivity.this);
            }

            @Override
            public void onDisconnected() {
                WebServer.getServer().stopServer();
            }
        });
        mRoots = DocumentsApplication.getRootsCache(this);
    }

    @CallSuper
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final RootInfo root = getCurrentRoot();
        if(!isSpecialDevice() && casty.isConnected()) {
            if (findViewById(R.id.casty_mini_controller) == null) {
                casty.addMiniController();
            }
        }
        if(!isSpecialDevice() && (null != root && RootInfo.isChromecastFeature(root))) {
            casty.addMediaRouteMenuItem(menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(!isSpecialDevice()) {
            boolean connected = (castSession != null) && castSession.isConnected();
            MediaQueue queue = casty.getMediaQueue();
            int queueCount = null != queue ? queue.getItemCount() : 0;
            MenuItem showQueue = menu.findItem(R.id.action_show_queue);
            if(null != showQueue) {
                showQueue.setVisible(connected && queueCount > 0);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_show_queue){
            final RootInfo root = getCurrentRoot();
            DocumentsActivity activity = ((DocumentsActivity)this);
            openRoot(activity, mRoots.getCastRoot(), root);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(!isSpecialDevice() && Casty.isAvailable(this)) {
            return CastContext.getSharedInstance(this).onDispatchVolumeKeyEventBeforeJellyBean(event)
                    || super.dispatchKeyEvent(event);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }
}