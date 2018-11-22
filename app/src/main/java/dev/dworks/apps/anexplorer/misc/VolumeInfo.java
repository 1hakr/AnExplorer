package dev.dworks.apps.anexplorer.misc;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.io.File;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentsContract;

import static dev.dworks.apps.anexplorer.misc.SAFManager.DOCUMENT_AUTHORITY;

/**
 * Created by HaKr on 24/08/16.
 */

public class VolumeInfo {
    public static final String ACTION_VOLUME_STATE_CHANGED =
            "android.os.storage.action.VOLUME_STATE_CHANGED";
    public static final String EXTRA_VOLUME_ID =
            "android.os.storage.extra.VOLUME_ID";
    public static final String EXTRA_VOLUME_STATE =
            "android.os.storage.extra.VOLUME_STATE";

    /** Stub volume representing internal private storage */
    public static final String ID_PRIVATE_INTERNAL = "private";
    /** Real volume representing internal emulated storage */
    public static final String ID_EMULATED_INTERNAL = "emulated";

    public static final int TYPE_PUBLIC = 0;
    public static final int TYPE_PRIVATE = 1;
    public static final int TYPE_EMULATED = 2;
    public static final int TYPE_ASEC = 3;
    public static final int TYPE_OBB = 4;

    public static final int STATE_UNMOUNTED = 0;
    public static final int STATE_CHECKING = 1;
    public static final int STATE_MOUNTED = 2;
    public static final int STATE_MOUNTED_READ_ONLY = 3;
    public static final int STATE_FORMATTING = 4;
    public static final int STATE_EJECTING = 5;
    public static final int STATE_UNMOUNTABLE = 6;
    public static final int STATE_REMOVED = 7;
    public static final int STATE_BAD_REMOVAL = 8;

    public static final int MOUNT_FLAG_PRIMARY = 1 << 0;
    public static final int MOUNT_FLAG_VISIBLE = 1 << 1;

    private static SparseArray<String> sStateToEnvironment = new SparseArray<>();
    private static ArrayMap<String, String> sEnvironmentToBroadcast = new ArrayMap<>();
    private static SparseIntArray sStateToDescrip = new SparseIntArray();

    private static final Comparator<VolumeInfo>
            sDescriptionComparator = new Comparator<VolumeInfo>() {
        @Override
        public int compare(VolumeInfo lhs, VolumeInfo rhs) {
            if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(lhs.getId())) {
                return -1;
            } else if (lhs.getDescription() == null) {
                return 1;
            } else if (rhs.getDescription() == null) {
                return -1;
            } else {
                return lhs.getDescription().compareTo(rhs.getDescription());
            }
        }
    };

    static {
        sStateToEnvironment.put(VolumeInfo.STATE_UNMOUNTED, Environment.MEDIA_UNMOUNTED);
        sStateToEnvironment.put(VolumeInfo.STATE_CHECKING, Environment.MEDIA_CHECKING);
        sStateToEnvironment.put(VolumeInfo.STATE_MOUNTED, Environment.MEDIA_MOUNTED);
        sStateToEnvironment.put(VolumeInfo.STATE_MOUNTED_READ_ONLY, Environment.MEDIA_MOUNTED_READ_ONLY);
        sStateToEnvironment.put(VolumeInfo.STATE_FORMATTING, Environment.MEDIA_UNMOUNTED);
        sStateToEnvironment.put(VolumeInfo.STATE_EJECTING, Environment.MEDIA_EJECTING);
        sStateToEnvironment.put(VolumeInfo.STATE_UNMOUNTABLE, Environment.MEDIA_UNMOUNTABLE);
        sStateToEnvironment.put(VolumeInfo.STATE_REMOVED, Environment.MEDIA_REMOVED);
        sStateToEnvironment.put(VolumeInfo.STATE_BAD_REMOVAL, Environment.MEDIA_BAD_REMOVAL);

        sEnvironmentToBroadcast.put(Environment.MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_UNMOUNTED);
        sEnvironmentToBroadcast.put(Environment.MEDIA_CHECKING, Intent.ACTION_MEDIA_CHECKING);
        sEnvironmentToBroadcast.put(Environment.MEDIA_MOUNTED, Intent.ACTION_MEDIA_MOUNTED);
        sEnvironmentToBroadcast.put(Environment.MEDIA_MOUNTED_READ_ONLY, Intent.ACTION_MEDIA_MOUNTED);
        sEnvironmentToBroadcast.put(Environment.MEDIA_EJECTING, Intent.ACTION_MEDIA_EJECT);
        sEnvironmentToBroadcast.put(Environment.MEDIA_UNMOUNTABLE, Intent.ACTION_MEDIA_UNMOUNTABLE);
        sEnvironmentToBroadcast.put(Environment.MEDIA_REMOVED, Intent.ACTION_MEDIA_REMOVED);
        sEnvironmentToBroadcast.put(Environment.MEDIA_BAD_REMOVAL, Intent.ACTION_MEDIA_BAD_REMOVAL);

        sStateToDescrip.put(VolumeInfo.STATE_UNMOUNTED, R.string.ext_media_status_unmounted);
        sStateToDescrip.put(VolumeInfo.STATE_CHECKING, R.string.ext_media_status_checking);
        sStateToDescrip.put(VolumeInfo.STATE_MOUNTED, R.string.ext_media_status_mounted);
        sStateToDescrip.put(VolumeInfo.STATE_MOUNTED_READ_ONLY, R.string.ext_media_status_mounted_ro);
        sStateToDescrip.put(VolumeInfo.STATE_FORMATTING, R.string.ext_media_status_formatting);
        sStateToDescrip.put(VolumeInfo.STATE_EJECTING, R.string.ext_media_status_ejecting);
        sStateToDescrip.put(VolumeInfo.STATE_UNMOUNTABLE, R.string.ext_media_status_unmountable);
        sStateToDescrip.put(VolumeInfo.STATE_REMOVED, R.string.ext_media_status_removed);
        sStateToDescrip.put(VolumeInfo.STATE_BAD_REMOVAL, R.string.ext_media_status_bad_removal);
    }

    /** vold state */
    public final String id; //needed
    public final int type; //needed
    public final DiskInfo disk;
    public final String partGuid;
    public int mountFlags = 0; //needed
    public int mountUserId = -1;
    public int state = STATE_UNMOUNTED; //needed
    public String fsType;
    public String fsUuid; //needed
    public String fsLabel;
    public String path; //needed
    public String internalPath; //needed

    public VolumeInfo(String id, int type, DiskInfo disk, String partGuid) {
        this.id = Preconditions.checkNotNull(id);
        this.type = type;
        this.disk = disk;
        this.partGuid = partGuid;
    }


    public static @NonNull
    String getEnvironmentForState(int state) {
        final String envState = sStateToEnvironment.get(state);
        if (envState != null) {
            return envState;
        } else {
            return Environment.MEDIA_UNKNOWN;
        }
    }

    public static @Nullable
    String getBroadcastForEnvironment(String envState) {
        return sEnvironmentToBroadcast.get(envState);
    }

    public static @Nullable String getBroadcastForState(int state) {
        return getBroadcastForEnvironment(getEnvironmentForState(state));
    }

    public static @NonNull Comparator<VolumeInfo> getDescriptionComparator() {
        return sDescriptionComparator;
    }

    public @NonNull String getId() {
        return id;
    }

    public @Nullable DiskInfo getDisk() {
        return disk;
    }

    public @Nullable String getDiskId() {
        return (disk != null) ? disk.id : null;
    }

    public int getType() {
        return type;
    }

    public int getState() {
        return state;
    }

    public int getStateDescription() {
        return sStateToDescrip.get(state, 0);
    }

    public @Nullable String getFsUuid() {
        return fsUuid;
    }

    public int getMountUserId() {
        return mountUserId;
    }

    public @Nullable String getDescription() {
        if (ID_PRIVATE_INTERNAL.equals(id) || ID_EMULATED_INTERNAL.equals(id)) {
            return "Internal storage";
        } else if (!TextUtils.isEmpty(fsLabel)) {
            return fsLabel;
        } else {
            return null;
        }
    }

    public boolean isMountedReadable() {
        return state == STATE_MOUNTED || state == STATE_MOUNTED_READ_ONLY;
    }

    public boolean isMountedWritable() {
        return state == STATE_MOUNTED;
    }

    public boolean isPrimary() {
        return (mountFlags & MOUNT_FLAG_PRIMARY) != 0;
    }

    public boolean isPrimaryPhysical() {
        return isPrimary() && (getType() == TYPE_PUBLIC);
    }

    public boolean isVisible() {
        return (mountFlags & MOUNT_FLAG_VISIBLE) != 0;
    }

    public boolean isVisibleForUser(int userId) {
        if (type == TYPE_PUBLIC && mountUserId == userId) {
            return isVisible();
        } else if (type == TYPE_EMULATED) {
            return isVisible();
        } else {
            return false;
        }
    }

    public boolean isVisibleForRead(int userId) {
        return isVisibleForUser(userId);
    }

    public boolean isVisibleForWrite(int userId) {
        return isVisibleForUser(userId);
    }

    public File getPath() {
        return (path != null) ? new File(path) : null;
    }

    public File getInternalPath() {
        return (internalPath != null) ? new File(internalPath) : null;
    }

    public File getPathForUser(int userId) {
        if (path == null) {
            return null;
        } else if (type == TYPE_PUBLIC) {
            return new File(path);
        } else if (type == TYPE_EMULATED) {
            return new File(path, Integer.toString(userId));
        } else {
            return null;
        }
    }

    /**
     * Path which is accessible to apps holding
     */
    public File getInternalPathForUser(int userId) {
        if (path == null) {
            return null;
        } else if (type == TYPE_PUBLIC) {
            // TODO: plumb through cleaner path from vold
            return new File(path.replace("/storage/", "/mnt/media_rw/"));
        } else {
            return getPathForUser(userId);
        }
    }

    public static int buildStableMtpStorageId(String fsUuid) {
        if (TextUtils.isEmpty(fsUuid)) {
            return StorageVolume.STORAGE_ID_INVALID;
        } else {
            int hash = 0;
            for (int i = 0; i < fsUuid.length(); ++i) {
                hash = 31 * hash + fsUuid.charAt(i);
            }
            hash = (hash ^ (hash << 16)) & 0xffff0000;
            // Work around values that the spec doesn't allow, or that we've
            // reserved for primary
            if (hash == 0x00000000) hash = 0x00020000;
            if (hash == 0x00010000) hash = 0x00020000;
            if (hash == 0xffff0000) hash = 0xfffe0000;
            return hash | 0x0001;
        }
    }

    // TODO: avoid this layering violation
    private static final String DOCUMENT_ROOT_PRIMARY_EMULATED = "primary";

    /**
     * Build an intent to browse the contents of this volume. Only valid for
     * {@link #TYPE_EMULATED} or {@link #TYPE_PUBLIC}.
     */
    public Intent buildBrowseIntent() {
        final Uri uri;
        if (type == VolumeInfo.TYPE_PUBLIC) {
            uri = DocumentsContract.buildRootUri(DOCUMENT_AUTHORITY, fsUuid);
        } else if (type == VolumeInfo.TYPE_EMULATED && isPrimary()) {
            uri = DocumentsContract.buildRootUri(DOCUMENT_AUTHORITY,
                    DOCUMENT_ROOT_PRIMARY_EMULATED);
        } else {
            return null;
        }

        final Intent intent = new Intent(DocumentsContract.ACTION_BROWSE_DOCUMENT_ROOT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(uri);
        return intent;
    }
}
