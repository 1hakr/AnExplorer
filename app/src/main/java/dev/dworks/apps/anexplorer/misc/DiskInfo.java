package dev.dworks.apps.anexplorer.misc;

import android.content.res.Resources;
import android.os.Parcel;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.libcore.util.Objects;

/**
 * Created by HaKr on 24/08/16.
 */

public class DiskInfo {
    public static final String ACTION_DISK_SCANNED =
            "android.os.storage.action.DISK_SCANNED";
    public static final String EXTRA_DISK_ID =
            "android.os.storage.extra.DISK_ID";
    public static final String EXTRA_VOLUME_COUNT =
            "android.os.storage.extra.VOLUME_COUNT";

    public static final int FLAG_ADOPTABLE = 1 << 0;
    public static final int FLAG_DEFAULT_PRIMARY = 1 << 1;
    public static final int FLAG_SD = 1 << 2;
    public static final int FLAG_USB = 1 << 3;

    public final String id;
    public final int flags;
    public long size;
    public String label;
    /** Hacky; don't rely on this count */
    public int volumeCount;
    public String sysPath;

    public DiskInfo(String id, int flags) {
        this.id = Preconditions.checkNotNull(id);
        this.flags = flags;
    }

    public DiskInfo(Parcel parcel) {
        id = parcel.readString();
        flags = parcel.readInt();
        size = parcel.readLong();
        label = parcel.readString();
        volumeCount = parcel.readInt();
        sysPath = parcel.readString();
    }

    public @NonNull
    String getId() {
        return id;
    }

    public boolean isInteresting(String label) {
        if (TextUtils.isEmpty(label)) {
            return false;
        }
        if (label.equalsIgnoreCase("ata")) {
            return false;
        }
        if (label.toLowerCase().contains("generic")) {
            return false;
        }
        if (label.toLowerCase().startsWith("usb")) {
            return false;
        }
        return !label.toLowerCase().startsWith("multiple");
    }

    public String getDescription() {
        final Resources res = Resources.getSystem();
        if ((flags & FLAG_SD) != 0) {
            if (isInteresting(label)) {
                return res.getString(R.string.storage_sd_card_label, label);
            } else {
                return res.getString(R.string.storage_sd_card);
            }
        } else if ((flags & FLAG_USB) != 0) {
            if (isInteresting(label)) {
                return res.getString(R.string.storage_usb_drive_label, label);
            } else {
                return res.getString(R.string.storage_usb_drive);
            }
        } else {
            return null;
        }
    }

    public boolean isAdoptable() {
        return (flags & FLAG_ADOPTABLE) != 0;
    }

    public boolean isDefaultPrimary() {
        return (flags & FLAG_DEFAULT_PRIMARY) != 0;
    }

    public boolean isSd() {
        return (flags & FLAG_SD) != 0;
    }

    public boolean isUsb() {
        return (flags & FLAG_USB) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DiskInfo) {
            return Objects.equals(id, ((DiskInfo) o).id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
