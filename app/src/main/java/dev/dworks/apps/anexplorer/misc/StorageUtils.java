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
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.libcore.util.Objects;

import static dev.dworks.apps.anexplorer.misc.DiskInfo.FLAG_SD;
import static dev.dworks.apps.anexplorer.misc.DiskInfo.FLAG_USB;
import static dev.dworks.apps.anexplorer.misc.VolumeInfo.ID_EMULATED_INTERNAL;

public final class StorageUtils {
    private static final String TAG = "StorageUtils";

    // partition types
    public static final int PARTITION_SYSTEM = 1;
    public static final int PARTITION_DATA = 2;
    public static final int PARTITION_CACHE = 3;
    public static final int PARTITION_RAM = 4;
    public static final int PARTITION_EXTERNAL = 5;
    public static final int PARTITION_EMMC = 6;
    public static final int PARTITION_ESTORAGE = 7;
    
	private StorageManager mStorageManager;
	private ActivityManager activityManager;
    private Context mContext;
    public StorageUtils(Context context){
        mContext = context;
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public List<VolumeInfo> getVolumes() {
        List<VolumeInfo> mounts = new ArrayList<VolumeInfo>();
        List<Object> vi = null;
        boolean first = false;
        try {
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumes");
            vi = (List<Object>)getVolumeList.invoke(mStorageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null == vi){
            return mounts;
        }
        for (Object object : vi) {
            String id = getString(object, "id");
            int type = getInteger(object, "type");
            DiskInfo disk = getDiskInfo(object);
            String partGuid = getString(object, "id");

            int mountFlags = getInteger(object, "mountFlags");
            int mountUserId = getInteger(object, "mountUserId"); // -1
            int state = getInteger(object, "state");
            String fsType = getString(object, "fsType");
            String fsUuid = getString(object, "fsUuid");
            String fsLabel = getString(object, "fsLabel");
            String path = getString(object, "path");
            String internalPath = getString(object, "internalPath");

            if(Utils.hasPie() && !TextUtils.isEmpty(path)) {
                id = TextUtils.isEmpty(id) && !TextUtils.isEmpty(path)
                        ? (path.contains(ID_EMULATED_INTERNAL) ? ID_EMULATED_INTERNAL : "") : id;
                if(TextUtils.isEmpty(id)){
                    if(!first){
                        first = true;
                        id = ID_EMULATED_INTERNAL;
                    }
                }
            }

            VolumeInfo volumeInfo = new VolumeInfo(id, type, disk, partGuid);
            volumeInfo.mountFlags = mountFlags;
            volumeInfo.mountUserId = mountUserId;
            volumeInfo.state = state;
            volumeInfo.fsType = fsType;
            volumeInfo.fsUuid = fsUuid;
            volumeInfo.fsLabel = fsLabel;
            volumeInfo.path = path;
            volumeInfo.internalPath = internalPath;

            mounts.add(volumeInfo);
        }
        return mounts;
    }

    public List<StorageVolume> getStorageMounts() {
        List<StorageVolume> mounts = new ArrayList<StorageVolume>();
        boolean first = false;
        Object[] sv = null;
        try {
			Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
			sv = (Object[])getVolumeList.invoke(mStorageManager);
		} catch (Exception e) {
            e.printStackTrace();
        }
        if(null == sv){
            return mounts;
        }
        for (Object object : sv) {
            int mStorageId = getInteger(object, "mStorageId");
            File mPath = getFile(object);
            String mDescription = getDescription(object);

            boolean mPrimary = false;
            if(Utils.hasJellyBeanMR1()){
                mPrimary = getBoolean(object, "mPrimary");
            }
            else{
                if(!first){
                    first = true;
                    mPrimary = true;
                }
            }
            boolean mEmulated = getBoolean(object, "mEmulated");
            boolean mRemovable = getBoolean(object, "mRemovable");
            long mMtpReserveSize = getLong(object, "mMtpReserveSize");
            boolean mAllowMassStorage = getBoolean(object, "mAllowMassStorage");
            long mMaxFileSize = getLong(object, "mMaxFileSize");

            String mId = getString(object, "mId");
            String mFsUuid = getString(object, "mFsUuid");
            String mUuid = getString(object, "mUuid");
            String mUserLabel = getString(object, "mUserLabel");
            String mState = getString(object, "mState");

            if(Utils.hasPie()) {
                android.os.storage.StorageVolume volume = mStorageManager.getStorageVolume(mPath);
                if(null != volume) {
                    mPrimary = volume.isEmulated();
                    mEmulated = volume.isEmulated();
                    mRemovable = volume.isRemovable();
                    mUserLabel = volume.getDescription(mContext);
                    mFsUuid = volume.getUuid();
                    mState = volume.getState();
                }
            }

            StorageVolume storageVolume = new StorageVolume(mStorageId, mPath, mDescription, mPrimary,
                    mRemovable, mEmulated, mMtpReserveSize, mAllowMassStorage, mMaxFileSize);

            storageVolume.mId = mId;
            storageVolume.mFsUuid = mFsUuid;
            storageVolume.mUuid = mUuid;
            storageVolume.mUserLabel = mUserLabel;
            storageVolume.mState = mState;

            mounts.add(storageVolume);
        }
        return mounts;
    }

    private DiskInfo getDiskInfo(Object object) {
        String path = "";
        DiskInfo diskInfo = null;
        try {
            Field mPath = object.getClass().getDeclaredField("disk");
            mPath.setAccessible(true);
            Object diskObj = mPath.get(object);

            String id = getString(diskObj, "id");
            int flags = getInteger(diskObj, "flags");
            long size = getLong(diskObj, "size");
            String label = getString(diskObj, "label");
            int volumeCount = getInteger(diskObj, "volumeCount");
            String sysPath = getString(diskObj, "sysPath");

            diskInfo = new DiskInfo(id, flags);
            diskInfo.size = size;
            diskInfo.label = label;
            diskInfo.volumeCount = volumeCount;
            diskInfo.sysPath = sysPath;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return diskInfo;
    }

    private File getFile(Object object) {
        String path = "";
        File file = null;
        try {
            Field mPath = object.getClass().getDeclaredField("mPath");
            mPath.setAccessible(true);
            Object pathObj = mPath.get(object);
            if(Utils.hasJellyBeanMR1()){
                file = (File)pathObj;
            }
            else{
                path = (String)pathObj;
                file = new File(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private String getDescription(Object object) {
        String description = "";
        if(Utils.hasMarshmallow()){
            description = getDescription(object, false);
        }
        else if(Utils.hasJellyBean()){
            try {
                description = getDescription(object, true);
            }
            catch (Resources.NotFoundException e){
                description = getDescription(object, false);
            }
        }
        else{
            description = getDescription(object, false);
        }
        return description;
    }

    private String getDescription(Object object, boolean hasId) {
        String description = "";
        if (hasId) {
            int mDescriptionInt = getInteger(object, "mDescriptionId");
            description = mContext.getResources().getString(mDescriptionInt);
        } else {
            description = getString(object, "mDescription");
        }
        return description;
    }

    private String getString(Object object, String id) {
        String value = "";
        try {
            Field field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = (String) field.get(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private int getInteger(Object object, String id) {
        int value = 0;
        try {
            Field field = null;
            field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = field.getInt(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private boolean getBoolean(Object object, String id) {
        boolean value = false;
        try {
            Field field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = field.getBoolean(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private long getLong(Object object, String id) {
        long value = 0l;
        try {
            Field field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = field.getLong(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
    
	public static long getExtStorageSize(String path, boolean isTotal){
		return getPartionSize(path, isTotal);		
	}

	public long getPartionSize(int type, boolean isTotal){
		Long size = 0L;
		
		switch (type) {
		case PARTITION_SYSTEM:
			size = getPartionSize(Environment.getRootDirectory().getPath(), isTotal);
			break;
		case PARTITION_DATA:
			size = getPartionSize(Environment.getDataDirectory().getPath(), isTotal);
			break;
		case PARTITION_CACHE:
			size = getPartionSize(Environment.getDownloadCacheDirectory().getPath(), isTotal);
			break;
		case PARTITION_EXTERNAL:
			size = getPartionSize(Environment.getExternalStorageDirectory().getPath(), isTotal);
			break;
		/*case PARTITION_EMMC:
			size = getPartionSize(DIR_EMMC, isTotal);
			break;*/
		case PARTITION_RAM:
			size = getSizeTotalRAM(isTotal);
			break;			
		}
		return size;
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private long getSizeTotalRAM(boolean isTotal) {
		long sizeInBytes = 1000;
		MemoryInfo mi = new MemoryInfo();
		activityManager.getMemoryInfo(mi);
		if(isTotal) {
			try { 
				if(Utils.hasJellyBean()){
					long totalMegs = mi.totalMem;
					sizeInBytes = totalMegs;
				}
				else{
					RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
					String load = reader.readLine();
					String[] totrm = load.split(" kB");
					String[] trm = totrm[0].split(" ");
					sizeInBytes=Long.parseLong(trm[trm.length-1]);
					sizeInBytes=sizeInBytes*1024;
					reader.close();	
				}
			} 
			catch (Exception e) { }
		}
		else{
			long availableMegs = mi.availMem;
			sizeInBytes = availableMegs;
		}		
		return sizeInBytes;
	}
	
	/**
	 * @param isTotal  The parameter for calculating total size
	 * @return return Total Size when isTotal is true else return Free Size of Internal memory(data folder)
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
	private static long getPartionSize(String path, boolean isTotal){
		StatFs stat = null;
		try {
			stat = new StatFs(path);	
		} catch (Exception e) { }
		if(null != stat){
            if(Utils.hasJellyBeanMR2()){
                final long blockSize = stat.getBlockSizeLong();
                final long availableBlocks = (isTotal ? stat.getBlockCountLong() : stat.getAvailableBlocksLong());
                return availableBlocks * blockSize;
            }
            else{
                final long blockSize = stat.getBlockSize();
                final long availableBlocks = (isTotal ? (long)stat.getBlockCount() : (long)stat.getAvailableBlocks());
                return availableBlocks * blockSize;
            }
		}
		else return 0L;
	}

    public static String getBestVolumeDescription(Context context, VolumeInfo vol) {
        if (vol == null) return null;
        // Nickname always takes precedence when defined
/*        if (!TextUtils.isEmpty(vol.fsUuid)) {
            final VolumeRecord rec = findRecordByUuid(vol.fsUuid);
            if (rec != null && !TextUtils.isEmpty(rec.nickname)) {
                return rec.nickname;
            }
        }*/
        if (!TextUtils.isEmpty(vol.getDescription())) {
            return vol.getDescription();
        }
        if (vol.disk != null) {
            final Resources res = context.getResources();
            int flags = vol.disk.flags;
            String label = vol.disk.label;
            if ((flags & FLAG_SD) != 0) {
                if (vol.disk.isInteresting(label)) {
                    return res.getString(R.string.storage_sd_card_label, label);
                } else {
                    return res.getString(R.string.storage_sd_card);
                }
            } else if ((flags & FLAG_USB) != 0) {
                if (vol.disk.isInteresting(label)) {
                    return res.getString(R.string.storage_usb_drive_label, label);
                } else {
                    return res.getString(R.string.storage_usb_drive);
                }
            } else {
                return null;
            }
            //return vol.disk.getDescription();
        }
        return null;
    }

    public VolumeInfo findPrivateForEmulated(VolumeInfo emulatedVol) {
        if (emulatedVol != null) {
            return findVolumeById(emulatedVol.getId().replace("emulated", "private"));
        } else {
            return null;
        }
    }

    public VolumeInfo findVolumeById(String id) {
        Preconditions.checkNotNull(id);
        // TODO; go directly to service to make this faster
        for (VolumeInfo vol : getVolumes()) {
            if (Objects.equals(vol.id, id)) {
                return vol;
            }
        }
        return null;
    }
}