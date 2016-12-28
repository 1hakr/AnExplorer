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

import android.content.Context;
import android.text.TextUtils;

import java.io.File;

public final class StorageVolume {
    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    public static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

    private final int mStorageId;
    private final File mPath;

    private final String mDescription;
    private final boolean mPrimary;
    private final boolean mEmulated;
    private final boolean mRemovable;
    private final long mMtpReserveSize;
    private final boolean mAllowMassStorage;
    private final long mMaxFileSize;

    String mId;

    String mFsUuid;
    String mUuid;
    String mUserLabel;

    String mState;

    public static final String EXTRA_STORAGE_VOLUME = "android.os.storage.extra.STORAGE_VOLUME";
    public static final String EXTRA_DIRECTORY_NAME = "android.os.storage.extra.DIRECTORY_NAME";
    private static final String ACTION_OPEN_EXTERNAL_DIRECTORY =
            "android.os.storage.action.OPEN_EXTERNAL_DIRECTORY";
    public static final int STORAGE_ID_INVALID = 0x00000000;
    public static final int STORAGE_ID_PRIMARY = 0x00010001;

    public StorageVolume(int storageId, File path, String description, boolean primary,
                         boolean removable, boolean emulated, long mtpReserveSize, boolean allowMassStorage,
                         long maxFileSize) {
        mStorageId = storageId;
        mPath = path;
        mDescription = description;
        mPrimary = primary;
        mRemovable = removable;
        mEmulated = emulated;
        mMtpReserveSize = mtpReserveSize;
        mAllowMassStorage = allowMassStorage;
        mMaxFileSize = maxFileSize;
        //mId = Utils.Preconditions.checkNotNull(id);
        //mFsUuid = fsUuid;
        //mState = Preconditions.checkNotNull(state);
    }

    public String getId() {
        return mId;
    }

    /**
     * Returns the mount path for the volume.
     *
     * @return the mount path
     */
    public String getPath() {
        return mPath.toString();
    }

    public File getPathFile() {
        return mPath;
    }

    /**
     * Returns a user visible description of the volume.
     *
     * @return the volume description
     */
    public String getDescription(Context context) {
        return mDescription;
    }

    public boolean isPrimary() {
        return mPrimary;
    }

    /**
     * Returns true if the volume is removable.
     *
     * @return is removable
     */
    public boolean isRemovable() {
        return mRemovable;
    }

    /**
     * Returns true if the volume is emulated.
     *
     * @return is removable
     */
    public boolean isEmulated() {
        return mEmulated;
    }

    /**
     * Returns the MTP storage ID for the volume.
     * this is also used for the storage_id column in the media provider.
     *
     * @return MTP storage ID
     */
    public int getStorageId() {
        return mStorageId;
    }

    /**
     * Number of megabytes of space to leave unallocated by MTP.
     * MTP will subtract this value from the free space it reports back
     * to the host via GetStorageInfo, and will not allow new files to
     * be added via MTP if there is less than this amount left free in the storage.
     * If MTP has dedicated storage this value should be zero, but if MTP is
     * sharing storage with the rest of the system, set this to a positive value
     * to ensure that MTP activity does not result in the storage being
     * too close to full.
     *
     * @return MTP reserve space
     */
    public int getMtpReserveSpace() {
        return (int) (mMtpReserveSize / MB_IN_BYTES);
    }

    /**
     * Returns true if this volume can be shared via USB mass storage.
     *
     * @return whether mass storage is allowed
     */
    public boolean allowMassStorage() {
        return mAllowMassStorage;
    }

    /**
     * Returns maximum file size for the volume, or zero if it is unbounded.
     *
     * @return maximum file size
     */
    public long getMaxFileSize() {
        return mMaxFileSize;
    }

    /**
     * Kitkat and above
     *
     * @return
     */
    public String getUuid() {
        return TextUtils.isEmpty(mUuid) ? mFsUuid : mUuid;
    }

    /**
     * Parse and return volume UUID as FAT volume ID, or return -1 if unable to
     * parse or UUID is unknown.
     */
    public int getFatVolumeId() {
        if (mFsUuid == null || mFsUuid.length() != 9) {
            return -1;
        }
        try {
            return (int) Long.parseLong(mFsUuid.replace("-", ""), 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getUserLabel() {
        return TextUtils.isEmpty(mUserLabel) ? mDescription : mUserLabel;
    }

    /**
     * Kitkat and above
     *
     * @return
     */
    public String getState() {
        return mState;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StorageVolume && mPath != null) {
            StorageVolume volume = (StorageVolume)obj;
            return (mPath.equals(volume.mPath));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mPath.hashCode();
    }
}